/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.server.wm.flicker

import android.util.Log
import com.android.server.wm.flicker.FlickerRunResult.Companion.RunResults
import com.android.server.wm.flicker.FlickerRunResult.Companion.RunStatus
import com.android.server.wm.flicker.monitor.IFileGeneratingMonitor
import com.android.server.wm.flicker.monitor.ITransitionMonitor
import com.android.server.wm.flicker.traces.layers.LayersTraceSubject
import com.android.server.wm.flicker.traces.windowmanager.WindowManagerTraceSubject
import com.android.server.wm.traces.common.ConditionList
import com.android.server.wm.traces.common.WindowManagerConditionsFactory
import com.android.server.wm.traces.parser.DeviceDumpParser
import com.android.server.wm.traces.parser.getCurrentState
import java.io.IOException
import java.nio.file.Files
import org.junit.runner.Description

/**
 * Runner to execute the transitions of a flicker test
 *
 * The commands are executed in the following order:
 * 1) [Flicker.testSetup]
 * 2) [Flicker.runSetup
 * 3) Start monitors
 * 4) [Flicker.transitions]
 * 5) Stop monitors
 * 6) [Flicker.runTeardown]
 * 7) [Flicker.testTeardown]
 *
 * If the tests were already executed, reuse the previous results
 *
 */
open class TransitionRunner {
    /**
     * Iteration identifier during test run
     */
    internal var iteration = 0
        private set
    private val tags = mutableSetOf<String>()
    private var tagsResults = mutableListOf<FlickerRunResult>()

    /**
     * Executes the setup, transitions and teardown defined in [flicker]
     *
     * @param flicker test specification
     * @throws IllegalArgumentException If the transitions are empty or repetitions is set to 0
     */
    open fun execute(flicker: Flicker): FlickerResult {
        check(flicker)
        return run(flicker)
    }

    /**
     * Validate the [flicker] test specification before executing the transitions
     *
     * @param flicker test specification
     * @throws IllegalArgumentException If the transitions are empty or repetitions is set to 0
     */
    protected fun check(flicker: Flicker) {
        require(flicker.transitions.isNotEmpty()) {
            "A flicker test must include transitions to run"
        }
        require(flicker.repetitions > 0) {
            "Number of repetitions must be greater than 0"
        }
    }

    open fun cleanUp() {
        tags.clear()
        tagsResults.clear()
    }

    /**
     * Runs the actual setup, transitions and teardown defined in [flicker]
     *
     * @param flicker test specification
     */
    internal open fun run(flicker: Flicker): FlickerResult {
        val runs = mutableListOf<FlickerRunResult>()
        val executionErrors = mutableListOf<ExecutionError>()
        safeExecution(flicker, runs, executionErrors) {
            runTestSetup(flicker)

            for (x in 0 until flicker.repetitions) {
                iteration = x
                val description = Description.createSuiteDescription(flicker.testName)
                if (flicker.faasEnabled) {
                    flicker.faas.setCriticalUserJourneyName(flicker.testName)
                    flicker.faas.testStarted(description)
                }
                runTransitionSetup(flicker)
                runTransition(flicker)
                runTransitionTeardown(flicker)
                processRunTraces(flicker, runs, RunStatus.ASSERTION_SUCCESS)
                if (flicker.faasEnabled) {
                    flicker.faas.testFinished(description)
                    if (flicker.faas.executionErrors.isNotEmpty()) {
                        executionErrors.addAll(flicker.faas.executionErrors)
                    }
                }
            }

            runTestTeardown(flicker)
        }

        runs.addAll(tagsResults)
        val result = FlickerResult(runs.toList(), tags.toSet(), executionErrors)
        cleanUp()
        return result
    }

    private fun safeExecution(
        flicker: Flicker,
        runs: MutableList<FlickerRunResult>,
        executionErrors: MutableList<ExecutionError>,
        execution: () -> Unit
    ) {
        try {
            execution()
        } catch (e: TestSetupFailure) {
            // If we failure on the test setup we can't run any of the transitions
            executionErrors.add(e)
        } catch (e: TransitionSetupFailure) {
            // If we fail on the transition run setup then we don't want to run any further
            // transitions nor save any results for this run. We simply want to run the test
            // teardown.
            executionErrors.add(e)
            safeExecution(flicker, runs, executionErrors) {
                runTestTeardown(flicker)
            }
        } catch (e: TransitionExecutionFailure) {
            // If a transition fails to run we don't want to run the following iterations as the
            // device is likely in an unexpected state which would lead to further errors. We simply
            // want to run the test teardown
            executionErrors.add(e)
            flicker.traceMonitors.forEach { it.tryStop() }
            safeExecution(flicker, runs, executionErrors) {
                processRunTraces(flicker, runs, RunStatus.RUN_FAILED)
                runTestTeardown(flicker)
            }
        } catch (e: TransitionTeardownFailure) {
            // If a transition teardown fails to run we don't want to run the following iterations
            // as the device is likely in an unexpected state which would lead to further errors.
            // But, we do want to run the test teardown.
            executionErrors.add(e)
            flicker.traceMonitors.forEach { it.tryStop() }
            safeExecution(flicker, runs, executionErrors) {
                processRunTraces(flicker, runs, RunStatus.RUN_FAILED)
                runTestTeardown(flicker)
            }
        } catch (e: TraceProcessingFailure) {
            // If we fail to process the run traces we still want to run the teardowns and report
            // the execution error.
            executionErrors.add(e)
            safeExecution(flicker, runs, executionErrors) {
                runTransitionTeardown(flicker)
                runTestTeardown(flicker)
            }
        } catch (e: TestTeardownFailure) {
            // If we fail in the execution of the test teardown there is nothing else to do apart
            // from reporting the execution error.
            executionErrors.add(e)
            for (run in runs) {
                run.setRunFailed()
            }
        }
    }

    /**
     * Parses the traces collected by the monitors to generate FlickerRunResults containing the
     * parsed trace and information about the status of the run.
     * The run results are added to the runs list which is then used to run Flicker assertions on.
     */
    @Throws(TraceProcessingFailure::class)
    private fun processRunTraces(
        flicker: Flicker,
        runs: MutableList<FlickerRunResult>,
        status: RunStatus
    ) {
        try {
            val runResults = buildRunResults(flicker, iteration, status)
            runs.addAll(runResults.toList())

            if (flicker.faasEnabled && !status.isFailure) {
                // Don't run FaaS on failed transitions
                val wmTrace = (runResults.traceResult.wmSubject as WindowManagerTraceSubject).trace
                val layersTrace = (runResults.traceResult.layersSubject as LayersTraceSubject).trace
                val transitionsTrace = runResults.traceResult.transitionsTrace

                flicker.faasTracesCollector.wmTrace = wmTrace
                flicker.faasTracesCollector.layersTrace = layersTrace
                flicker.faasTracesCollector.transitionsTrace = transitionsTrace
            }
        } catch (e: Throwable) {
            // We have failed to add the results to the runs, so we can effectively consider these
            // results as "lost" as they won't be used from now forth. So we can safely rename
            // to file to indicate the failure and make it easier to find in the archives.
            flicker.traceMonitors.forEach {
                // All monitors that generate files we want to keep in the archives should implement
                // IFileGeneratingMonitor
                if (it is IFileGeneratingMonitor) {
                    Utils.addStatusToFileName(it.outputFile, RunStatus.PARSING_FAILURE)
                }
            }
            throw TraceProcessingFailure(e)
        }

        // Update the status of all the tags created in this iteration and add them to runs
        for (result in tagsResults) {
            result.status = status
            runs.add(result)
        }
        tagsResults.clear()
    }

    @Throws(TestSetupFailure::class)
    private fun runTestSetup(flicker: Flicker) {
        try {
            flicker.testSetup.forEach { it.invoke(flicker) }
        } catch (e: Throwable) {
            throw TestSetupFailure(e)
        }
    }

    @Throws(TestTeardownFailure::class)
    private fun runTestTeardown(flicker: Flicker) {
        try {
            flicker.testTeardown.forEach { it.invoke(flicker) }
        } catch (e: Throwable) {
            throw TestTeardownFailure(e)
        }
    }

    @Throws(TransitionSetupFailure::class)
    private fun runTransitionSetup(flicker: Flicker) {
        try {
            flicker.runSetup.forEach { it.invoke(flicker) }
            flicker.wmHelper.StateSyncBuilder()
                .add(UI_STABLE_CONDITIONS)
                .waitFor()
        } catch (e: Throwable) {
            throw TransitionSetupFailure(e)
        }
    }

    @Throws(TransitionExecutionFailure::class)
    private fun runTransition(flicker: Flicker) {
        try {
            flicker.traceMonitors.forEach { it.start() }
            flicker.transitions.forEach { it.invoke(flicker) }
        } catch (e: Throwable) {
            throw TransitionExecutionFailure(e)
        }
    }

    @Throws(TransitionTeardownFailure::class)
    private fun runTransitionTeardown(flicker: Flicker) {
        try {
            flicker.wmHelper.StateSyncBuilder()
                .add(UI_STABLE_CONDITIONS)
                .waitFor()
            flicker.traceMonitors.forEach { it.tryStop() }
            flicker.runTeardown.forEach { it.invoke(flicker) }
        } catch (e: Throwable) {
            throw TransitionTeardownFailure(e)
        }
    }

    private fun buildRunResults(
        flicker: Flicker,
        iteration: Int,
        status: RunStatus
    ): RunResults {
        val resultBuilder = FlickerRunResult.Builder()
        flicker.traceMonitors.forEach {
            resultBuilder.setResultFrom(it)
        }

        return resultBuilder.buildAll(flicker.testName, iteration, status)
    }

    private fun ITransitionMonitor.tryStop() {
        this.run {
            try {
                stop()
            } catch (e: Exception) {
                Log.e(FLICKER_TAG, "Unable to stop $this", e)
            }
        }
    }

    private fun getTaggedFilePath(flicker: Flicker, tag: String, file: String) =
        "${flicker.testName}_${iteration}_${tag}_$file"

    /**
     * Captures a snapshot of the device state and associates it with a new tag.
     *
     * This tag can be used to make assertions about the state of the device when the
     * snapshot is collected.
     *
     * [tag] is used as part of the trace file name, thus, only valid letters and digits
     * can be used
     *
     * @param flicker test specification
     * @throws IllegalArgumentException If [tag] contains invalid characters
     */
    open fun createTag(flicker: Flicker, tag: String) {
        require(!tag.contains(" ")) {
            "The test tag $tag can not contain spaces since it is a part of the file name"
        }
        tags.add(tag)

        val deviceStateBytes = getCurrentState(flicker.instrumentation.uiAutomation)
        val deviceState = DeviceDumpParser.fromDump(deviceStateBytes.first, deviceStateBytes.second)
        try {
            val wmTraceFile = flicker.outputDir.resolve(
                getTaggedFilePath(flicker, tag, "wm_trace")
            )
            Files.write(wmTraceFile, deviceStateBytes.first)

            val layersTraceFile = flicker.outputDir.resolve(
                getTaggedFilePath(flicker, tag, "layers_trace")
            )
            Files.write(layersTraceFile, deviceStateBytes.second)

            val builder = FlickerRunResult.Builder()
            val result = builder.buildStateResult(
                tag,
                deviceState.wmState?.asTrace(),
                deviceState.layerState?.asTrace(),
                wmTraceFile,
                layersTraceFile,
                flicker.testName,
                iteration,
                // Undefined until it is updated in processRunTraces
                RunStatus.UNDEFINED
            )
            tagsResults.add(result)
        } catch (e: IOException) {
            throw RuntimeException("Unable to create trace file: ${e.message}", e)
        }
    }

    companion object {
        /**
         * Conditions that determine when the UI is in a stable stable and no windows or layers are
         * animating or changing state.
         */
        private val UI_STABLE_CONDITIONS = ConditionList(
            listOf(
                WindowManagerConditionsFactory.isWMStateComplete(),
                WindowManagerConditionsFactory.hasLayersAnimating().negate()
            )
        )

        open class ExecutionError(val inner: Throwable) : Throwable(inner) {
            init {
                super.setStackTrace(inner.stackTrace)
            }

            override val message: String?
                get() = inner.toString()
        }

        class TestSetupFailure(val e: Throwable) : ExecutionError(e)
        class TransitionSetupFailure(val e: Throwable) : ExecutionError(e)
        class TransitionExecutionFailure(val e: Throwable) : ExecutionError(e)
        class TraceProcessingFailure(val e: Throwable) : ExecutionError(e)
        class TransitionTeardownFailure(val e: Throwable) : ExecutionError(e)
        class TestTeardownFailure(val e: Throwable) : ExecutionError(e)
    }
}
