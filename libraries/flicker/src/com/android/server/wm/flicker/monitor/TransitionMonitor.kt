/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.server.wm.flicker.monitor

import com.android.server.wm.flicker.DEFAULT_TRACE_CONFIG
import com.android.server.wm.flicker.deleteIfExists
import com.android.server.wm.flicker.io.IResultData
import com.android.server.wm.flicker.io.ResultReader
import com.android.server.wm.flicker.io.ResultWriter
import com.android.server.wm.traces.common.ScenarioBuilder
import java.io.File

abstract class TransitionMonitor : TraceMonitor() {
    /**
     * Acquires the trace generated when executing the commands defined in the [predicate].
     *
     * @param predicate Commands to execute
     * @throws UnsupportedOperationException If tracing is already activated
     */
    fun withTracing(predicate: () -> Unit): ByteArray {
        if (this.isEnabled) {
            throw UnsupportedOperationException(
                "Trace already running. " + "This is likely due to chained 'withTracing' calls."
            )
        }
        val result: IResultData
        try {
            this.start()
            predicate()
        } finally {
            val writer = createWriter()
            this.stop(writer)
            result = writer.write()
        }
        val reader = ResultReader(result, DEFAULT_TRACE_CONFIG)
        val bytes = reader.readBytes(traceType) ?: error("Missing trace $traceType")
        result.artifact.deleteIfExists()
        return bytes
    }

    private fun createWriter(): ResultWriter {
        val className = this::class.simpleName ?: error("Missing class name for $this")
        val scenario = ScenarioBuilder().forClass(className).build()
        val tmpDir = File.createTempFile("withTracing", className).parentFile
        return ResultWriter().forScenario(scenario).withOutputDir(tmpDir)
    }
}
