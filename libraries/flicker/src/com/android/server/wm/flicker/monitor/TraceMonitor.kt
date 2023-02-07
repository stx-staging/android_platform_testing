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

import com.android.server.wm.flicker.IResultSetter
import com.android.server.wm.flicker.ITransitionMonitor
import com.android.server.wm.flicker.Utils
import com.android.server.wm.flicker.io.ResultWriter
import com.android.server.wm.flicker.io.TraceType
import java.io.File

/**
 * Base class for monitors containing common logic to read the trace as a byte array and save the
 * trace to another location.
 */
abstract class TraceMonitor internal constructor(outputDir: File, val sourceFile: File) :
    ITransitionMonitor, IResultSetter, IFileGeneratingMonitor {
    override val outputFile: File = outputDir.resolve(sourceFile.name)
    abstract val isEnabled: Boolean
    abstract val traceType: TraceType

    /** Starts monitor. */
    final override fun start() {
        try {
            startTracing()
        } catch (e: Throwable) {
            throw RuntimeException("Could not start trace", e)
        }
    }

    /** Stops monitor. */
    final override fun stop() {
        try {
            stopTracing()
            moveTraceFileToOutputDir()
        } catch (e: Throwable) {
            throw RuntimeException("Could not stop trace", e)
        }
    }

    final override fun setResult(result: ResultWriter) {
        result.addTraceResult(traceType, outputFile)
    }

    abstract fun startTracing()
    abstract fun stopTracing()

    private fun moveTraceFileToOutputDir(): File {
        outputFile.parentFile.mkdirs()
        if (sourceFile != outputFile) {
            Utils.moveFile(sourceFile, outputFile)
        }
        require(outputFile.exists()) { "Unable to save trace file $outputFile" }
        return outputFile
    }

    companion object {
        @JvmStatic protected val TRACE_DIR = File("/data/misc/wmtrace/")
    }
}
