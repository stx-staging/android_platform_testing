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

import com.android.server.wm.flicker.DEFAULT_TRACE_CONFIG
import com.android.server.wm.flicker.io.IResultData
import com.android.server.wm.flicker.io.ResultReaderWithLru
import com.android.server.wm.flicker.io.ResultWriter
import com.android.server.wm.flicker.monitor.EventLogMonitor
import com.android.server.wm.flicker.monitor.LayersTraceMonitor
import com.android.server.wm.flicker.monitor.TransactionsTraceMonitor
import com.android.server.wm.flicker.monitor.TransitionsTraceMonitor
import com.android.server.wm.flicker.monitor.WindowManagerTraceMonitor
import com.android.server.wm.traces.common.CrossPlatform
import com.android.server.wm.traces.common.FLICKER_TAG
import com.android.server.wm.traces.common.Scenario
import com.android.server.wm.traces.common.ScenarioBuilder
import com.android.server.wm.traces.common.io.IReader
import com.android.server.wm.traces.common.service.ITracesCollector
import java.io.File

class FlickerServiceTracesCollector(
    val outputDir: File,
    val scenario: Scenario =
        ScenarioBuilder().forClass(FlickerServiceTracesCollector::class.java.simpleName).build()
) : ITracesCollector {

    private var result: IResultData? = null

    private val traceMonitors =
        listOf(
            WindowManagerTraceMonitor(),
            LayersTraceMonitor(),
            TransitionsTraceMonitor(),
            TransactionsTraceMonitor(),
            EventLogMonitor()
        )

    override fun start() {
        reportErrorsBlock("Failed to start traces") {
            reset()
            traceMonitors.forEach { it.start() }
        }
    }

    override fun stop() {
        reportErrorsBlock("Failed to stop traces") {
            CrossPlatform.log.v(LOG_TAG, "Creating output directory for trace files")
            outputDir.mkdirs()

            CrossPlatform.log.v(LOG_TAG, "Stopping trace monitors")
            val writer = ResultWriter().forScenario(scenario).withOutputDir(outputDir)
            traceMonitors.forEach { it.stop(writer) }
            result = writer.write()
        }
    }

    override fun getResultReader(): IReader {
        return reportErrorsBlock("Failed to get collected traces") {
            val result = result
            requireNotNull(result) { "Result not set" }
            ResultReaderWithLru(result, DEFAULT_TRACE_CONFIG)
        }
    }

    private fun reset() {
        result = null
        cleanupTraceFiles()
    }

    /**
     * Remove the WM trace and layers trace files collected from previous test runs if the directory
     * exists.
     */
    private fun cleanupTraceFiles() {
        if (outputDir.exists()) {
            outputDir.deleteRecursively()
        }
    }

    private fun <T : Any> reportErrorsBlock(msg: String, block: () -> T): T {
        try {
            return block()
        } catch (e: Throwable) {
            CrossPlatform.log.e(LOG_TAG, msg, e)
            throw e
        }
    }

    companion object {
        private const val LOG_TAG = "$FLICKER_TAG-Collector"
    }
}
