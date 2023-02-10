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

import com.android.server.wm.flicker.Utils
import com.android.server.wm.flicker.io.ResultWriter
import com.android.server.wm.traces.common.io.TraceType
import java.io.File

/**
 * Base class for monitors containing common logic to read the trace as a byte array and save the
 * trace to another location.
 */
abstract class TraceMonitor : ITransitionMonitor {
    abstract val isEnabled: Boolean
    abstract val traceType: TraceType
    protected abstract fun doStop(): File

    /** Stops monitor. */
    final override fun stop(writer: ResultWriter) {
        val artifact =
            try {
                val srcFile = doStop()
                moveTraceFileToTmpDir(srcFile)
            } catch (e: Throwable) {
                throw RuntimeException("Could not stop trace", e)
            }
        writer.addTraceResult(traceType, artifact)
    }

    private fun moveTraceFileToTmpDir(sourceFile: File): File {
        val newFile = File.createTempFile(sourceFile.name, "")
        Utils.moveFile(sourceFile, newFile)
        require(newFile.exists()) { "Unable to save trace file $newFile" }
        return newFile
    }

    companion object {
        @JvmStatic protected val TRACE_DIR = File("/data/misc/wmtrace/")
    }
}
