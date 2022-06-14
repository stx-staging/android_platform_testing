/*
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.server.wm.flicker.service

import android.app.Instrumentation
import android.device.collectors.BaseMetricListener
import android.device.collectors.DataRecord
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import com.android.server.wm.flicker.service.assertors.AssertionResult
import com.android.server.wm.flicker.service.config.AssertionInvocationGroup
import java.nio.file.Path
import org.junit.runner.Description
import org.junit.runner.Result
import org.junit.runner.notification.Failure

/**
 * Collects all the Flicker Service's metrics which are then uploaded for analysis and monitoring
 * to the CrystalBall database.
 */
class FlickerServiceResultsCollector(
    val outputDir: Path,
    private val tracesCollector: ITracesCollector = FlickerServiceTracesCollector(outputDir),
    instrumentation: Instrumentation = InstrumentationRegistry.getInstrumentation()
) : BaseMetricListener() {
    private var criticalUserJourneyName: String = UNDEFINED_CUJ
    private var collectMetricsPerTest = true

    private val _executionErrors = mutableListOf<Throwable>()
    val executionErrors: List<Throwable> get() = _executionErrors

    internal val assertionResults = mutableListOf<AssertionResult>()

    init {
        setInstrumentation(instrumentation)
    }

    private fun errorReportingBlock(function: () -> Unit) {
        try {
            function()
        } catch (e: Throwable) {
            _executionErrors.add(e)
        }
    }

    override fun onTestRunStart(runData: DataRecord, description: Description) {
        errorReportingBlock {
            Log.i(LOG_TAG, "onTestRunStart :: collectMetricsPerTest = $collectMetricsPerTest")
            if (!collectMetricsPerTest) {
                tracesCollector.start()
            }
        }
    }

    override fun onTestStart(testData: DataRecord, description: Description) {
        errorReportingBlock {
            Log.i(LOG_TAG, "onTestStart :: collectMetricsPerTest = $collectMetricsPerTest")
            if (collectMetricsPerTest) {
                tracesCollector.start()
            }
        }
    }

    override fun onTestFail(testData: DataRecord, description: Description, failure: Failure) {
        errorReportingBlock {
            Log.i(LOG_TAG, "onTestFail")
        }
    }

    override fun onTestEnd(testData: DataRecord, description: Description) {
        errorReportingBlock {
            Log.i(LOG_TAG, "onTestEnd :: collectMetricsPerTest = $collectMetricsPerTest")
            if (collectMetricsPerTest) {
                stopTracingAndCollectFlickerMetrics(testData)
            }
        }
    }

    override fun onTestRunEnd(runData: DataRecord, result: Result) {
        errorReportingBlock {
            Log.i(LOG_TAG, "onTestRunEnd :: collectMetricsPerTest = $collectMetricsPerTest")
            if (!collectMetricsPerTest) {
                stopTracingAndCollectFlickerMetrics(runData)
            }
        }
    }

    private fun stopTracingAndCollectFlickerMetrics(dataRecord: DataRecord) {
        tracesCollector.stop()
        val collectedTraces = tracesCollector.getCollectedTraces()
        val flickerService = FlickerService()
        val results = flickerService.process(
            collectedTraces.wmTrace,
            collectedTraces.layersTrace,
            collectedTraces.transitionsTrace,
            collectedTraces.transactionsTrace
        )
        assertionResults.addAll(results)
        val aggregatedResults = processFlickerResults(results)
        collectMetrics(dataRecord, aggregatedResults)
    }

    private fun processFlickerResults(
        results: List<AssertionResult>
    ): Map<String, AggregatedFlickerResult> {
        val aggregatedResults = mutableMapOf<String, AggregatedFlickerResult>()
        for (result in results) {
            val key = getKeyForAssertionResult(result)
            if (!aggregatedResults.containsKey(key)) {
                aggregatedResults[key] = AggregatedFlickerResult()
            }
            aggregatedResults[key]!!.addResult(result)
        }
        return aggregatedResults
    }

    private fun collectMetrics(
        data: DataRecord,
        aggregatedResults: Map<String, AggregatedFlickerResult>
    ) {
        val it = aggregatedResults.entries.iterator()

        while (it.hasNext()) {
            val (key, result) = it.next()
            val resultString = "${result.passes}/${result.passes + result.failures}"
            var color = ANSI_RESET
            if (result.failures > 0) {
                color = ANSI_RED
            }
            if (result.failures == 0 && result.passes > 0) {
                color = ANSI_GREEN
            }

            val errorString = StringBuilder()
            if (result.errors.isNotEmpty()) {
                errorString.append("\n\t$ANSI_RED_BOLD$key$ANSI_RESET\n")
                for ((index, error) in result.errors.withIndex()) {
                    errorString.append(
                        "$ANSI_RED\t  ${index + 1}) ${error.lines()[0]}" +
                            "${error.substring(error.indexOf('\n') + 1)
                                .prependIndent("\t    ")}$ANSI_RESET\n"
                    )
                }
            }

            var blockingStatus = ""
            if (result.failures > 0) {
                blockingStatus = if (result.invocationGroup == AssertionInvocationGroup.BLOCKING) {
                    "$ANSI_RED_BOLD(BLOCKING)$ANSI_RESET"
                } else {
                    "$ANSI_WHITE$ANSI_LOW_INTENSITY(non blocking)$ANSI_RESET"
                }
            }

            data.addStringMetric(
                key,
                "$color$resultString$ANSI_RESET $blockingStatus$errorString"
            )
        }
    }

    private fun getKeyForAssertionResult(result: AssertionResult): String {
        val assertionName = "${result.scenario}#${result.assertionName}"
        return "$FASS_METRICS_PREFIX::$criticalUserJourneyName::$assertionName"
    }

    fun setCriticalUserJourneyName(className: String?) {
        this.criticalUserJourneyName = className ?: UNDEFINED_CUJ
    }

    companion object {
        // Unique prefix to add to all fass metrics to identify them
        private const val FASS_METRICS_PREFIX = "FASS"
        private const val UNDEFINED_CUJ = "UndefinedCUJ"
        private val LOG_TAG = "FlickerResultsCollector"

        class AggregatedFlickerResult {
            var failures = 0
            var passes = 0
            val errors = mutableListOf<String>()
            var invocationGroup: AssertionInvocationGroup? = null

            fun addResult(result: AssertionResult) {
                if (result.failed) {
                    failures++
                    errors.add(result.assertionError?.message ?: "FAILURE WITHOUT ERROR MESSAGE...")
                } else {
                    passes++
                }

                if (invocationGroup == null) {
                    invocationGroup = result.invocationGroup
                }

                if (invocationGroup != result.invocationGroup) {
                    error("Unexpected assertion group mismatch")
                }
            }
        }
    }
}
