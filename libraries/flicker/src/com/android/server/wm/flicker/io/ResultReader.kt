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

package com.android.server.wm.flicker.io

import android.util.Log
import androidx.annotation.VisibleForTesting
import com.android.server.wm.flicker.AssertionTag
import com.android.server.wm.flicker.TraceConfig
import com.android.server.wm.flicker.TraceConfigs
import com.android.server.wm.flicker.traces.eventlog.FocusEvent
import com.android.server.wm.traces.common.Timestamp
import com.android.server.wm.traces.common.layers.LayersTrace
import com.android.server.wm.traces.common.transactions.TransactionsTrace
import com.android.server.wm.traces.common.transition.TransitionsTrace
import com.android.server.wm.traces.common.windowmanager.WindowManagerTrace
import com.android.server.wm.traces.parser.layers.LayersTraceParser
import com.android.server.wm.traces.parser.transaction.TransactionsTraceParser
import com.android.server.wm.traces.parser.transition.TransitionsTraceParser
import com.android.server.wm.traces.parser.windowmanager.WindowManagerTraceParser
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

/**
 * Helper class to read results from a flicker artifact
 *
 * @param result to read from
 * @param traceConfig
 */
open class ResultReader(protected var result: ResultData, private val traceConfig: TraceConfigs) {
    @VisibleForTesting
    val artifactPath
        get() = result.artifactPath
    @VisibleForTesting
    val runStatus
        get() = result.runStatus
    private val transitionTimeRange
        get() = result.transitionTimeRange
    internal val isFailure
        get() = runStatus.isFailure
    internal val executionError
        get() = result.executionError

    private fun withZipFile(predicate: (ZipInputStream) -> Unit) {
        val zipInputStream =
            ZipInputStream(
                BufferedInputStream(FileInputStream(result.artifactPath.toFile()), BUFFER_SIZE)
            )
        try {
            predicate(zipInputStream)
        } finally {
            zipInputStream.closeEntry()
            zipInputStream.close()
        }
    }

    private fun forEachFileInZip(predicate: (ZipEntry) -> Unit) {
        withZipFile {
            var zipEntry: ZipEntry? = it.nextEntry
            while (zipEntry != null) {
                predicate(zipEntry)
                zipEntry = it.nextEntry
            }
        }
    }

    @Throws(IOException::class)
    private fun readFromZip(descriptor: ResultFileDescriptor): ByteArray? {
        Log.d(FLICKER_IO_TAG, "Reading descriptor=$descriptor from $result")

        var foundFile = false
        val outByteArray = ByteArrayOutputStream()
        val tmpBuffer = ByteArray(BUFFER_SIZE)
        withZipFile {
            var zipEntry: ZipEntry? = it.nextEntry
            while (zipEntry != null) {
                if (zipEntry.name == descriptor.fileNameInArtifact) {
                    val outputStream = BufferedOutputStream(outByteArray, BUFFER_SIZE)
                    try {
                        var size = it.read(tmpBuffer, 0, BUFFER_SIZE)
                        while (size > 0) {
                            outputStream.write(tmpBuffer, 0, size)
                            size = it.read(tmpBuffer, 0, BUFFER_SIZE)
                        }
                        it.closeEntry()
                    } finally {
                        outputStream.flush()
                        outputStream.close()
                    }
                    foundFile = true
                    break
                }
                zipEntry = it.nextEntry
            }
        }

        return if (foundFile) outByteArray.toByteArray() else null
    }

    /**
     * @return a [WindowManagerTrace] from the dump associated to [tag]
     * @throws IOException if the artifact file doesn't exist or can't be read
     */
    @Throws(IOException::class)
    internal fun readWmState(tag: String): WindowManagerTrace? = doReadWmState(tag)

    protected open fun doReadWmState(tag: String): WindowManagerTrace? {
        val descriptor = ResultFileDescriptor(TraceType.WM_DUMP, tag)
        Log.d(FLICKER_IO_TAG, "Reading WM trace descriptor=$descriptor from $result")
        val traceData = readFromZip(descriptor)
        return traceData?.let {
            WindowManagerTraceParser.parseFromDump(it, clearCacheAfterParsing = true)
        }
    }

    /**
     * @return a [WindowManagerTrace] for the part of the trace we want to run the assertions on
     * @throws IOException if the artifact file doesn't exist or can't be read
     */
    @Throws(IOException::class) internal fun readWmTrace(): WindowManagerTrace? = doReadWmTrace()

    protected open fun doReadWmTrace(): WindowManagerTrace? {
        val descriptor = ResultFileDescriptor(TraceType.WM)
        val traceData = readFromZip(descriptor)
        return traceData?.let {
            val trace =
                WindowManagerTraceParser.parseFromTrace(
                    it,
                    from = transitionTimeRange.start.elapsedRealtimeNanos,
                    to = transitionTimeRange.end.elapsedRealtimeNanos,
                    addInitialEntry = true,
                    clearCacheAfterParsing = true
                )
            val minimumEntries = minimumTraceEntriesForConfig(traceConfig.wmTrace)
            require(trace.entries.size >= minimumEntries) {
                "WM trace contained ${trace.entries.size} entries, " +
                    "expected at least $minimumEntries... :: " +
                    "transition starts at ${transitionTimeRange.start.elapsedRealtimeNanos} and " +
                    "ends at ${transitionTimeRange.end.elapsedRealtimeNanos}."
            }
            trace
        }
    }

    /**
     * @return a [LayersTrace] for the part of the trace we want to run the assertions on
     * @throws IOException if the artifact file doesn't exist or can't be read
     */
    @Throws(IOException::class) internal fun readLayersTrace(): LayersTrace? = doReadLayersTrace()

    protected open fun doReadLayersTrace(): LayersTrace? {
        val descriptor = ResultFileDescriptor(TraceType.SF)
        val traceData = readFromZip(descriptor)
        return traceData?.let {
            val trace =
                LayersTraceParser.parseFromTrace(
                    it,
                    transitionTimeRange.start.systemTime,
                    transitionTimeRange.end.systemTime,
                    addInitialEntry = true,
                    clearCacheAfterParsing = true
                )
            val minimumEntries = minimumTraceEntriesForConfig(traceConfig.layersTrace)
            require(trace.entries.size >= minimumEntries) {
                "Layers trace contained ${trace.entries.size} entries, " +
                    "expected at least $minimumEntries... :: " +
                    "transition starts at ${transitionTimeRange.start.systemTime} and " +
                    "ends at ${transitionTimeRange.end.systemTime}."
            }
            trace
        }
    }

    /**
     * @return a [LayersTrace] from the dump associated to [tag]
     * @throws IOException if the artifact file doesn't exist or can't be read
     */
    @Throws(IOException::class)
    internal fun readLayersDump(tag: String): LayersTrace? = doReadLayersDump(tag)

    protected open fun doReadLayersDump(tag: String): LayersTrace? {
        val descriptor = ResultFileDescriptor(TraceType.SF_DUMP, tag)
        val traceData = readFromZip(descriptor)
        return traceData?.let {
            LayersTraceParser.parseFromTrace(it, clearCacheAfterParsing = true)
        }
    }

    @Throws(IOException::class)
    private fun readFullTransactionsTrace(): TransactionsTrace? {
        val traceData = readFromZip(ResultFileDescriptor(TraceType.TRANSACTION))
        return traceData?.let {
            val fullTrace = TransactionsTraceParser.parseFromTrace(it)
            require(fullTrace.entries.isNotEmpty()) { "Transactions trace cannot be empty" }
            fullTrace
        }
    }

    /**
     * @return a [TransactionsTrace] for the part of the trace we want to run the assertions on
     * @throws IOException if the artifact file doesn't exist or can't be read
     */
    @Throws(IOException::class)
    internal fun readTransactionsTrace(): TransactionsTrace? = doReadTransactionsTrace()

    protected open fun doReadTransactionsTrace(): TransactionsTrace? {
        val fullTrace = readFullTransactionsTrace() ?: return null
        val trace =
            fullTrace.slice(
                transitionTimeRange.start.systemTime,
                transitionTimeRange.end.systemTime
            )
        require(trace.entries.isNotEmpty()) { "Trimmed transactions trace cannot be empty" }
        return trace
    }

    /**
     * @return a [TransitionsTrace] for the part of the trace we want to run the assertions on
     * @throws IOException if the artifact file doesn't exist or can't be read
     */
    @Throws(IOException::class)
    internal fun readTransitionsTrace(): TransitionsTrace? = doReadTransitionsTrace()

    protected open fun doReadTransitionsTrace(): TransitionsTrace? {
        val transactionsTrace = readFullTransactionsTrace()
        val traceData = readFromZip(ResultFileDescriptor(TraceType.TRANSITION))
        if (transactionsTrace == null || traceData == null) {
            return null
        }

        val fullTrace = TransitionsTraceParser.parseFromTrace(traceData, transactionsTrace)
        val trace =
            fullTrace.slice(
                transitionTimeRange.start.elapsedRealtimeNanos,
                transitionTimeRange.end.elapsedRealtimeNanos
            )
        if (!traceConfig.transitionsTrace.allowNoChange) {
            require(trace.entries.isNotEmpty()) { "Transitions trace cannot be empty" }
        }
        return trace
    }

    private fun minimumTraceEntriesForConfig(config: TraceConfig): Int {
        return if (config.allowNoChange) 1 else 2
    }

    /**
     * @return a List<[FocusEvent]> for the part of the trace we want to run the assertions on
     * @throws IOException if the artifact file doesn't exist or can't be read
     */
    internal fun readEventLogTrace(): List<FocusEvent>? = doReadEventLogTrace()

    protected open fun doReadEventLogTrace(): List<FocusEvent>? {
        return result.eventLog?.slice(
            transitionTimeRange.start.unixTimeNanos,
            transitionTimeRange.end.unixTimeNanos
        )
    }

    private fun List<FocusEvent>.slice(from: Timestamp, to: Timestamp): List<FocusEvent> {
        return dropWhile { it.timestamp.unixTimeNanos < from.unixTimeNanos }
            .dropLastWhile { it.timestamp.unixTimeNanos > to.unixTimeNanos }
    }

    override fun toString(): String = "$result"

    /** @return the number of files in the artifact */
    @VisibleForTesting
    fun countFiles(): Int {
        var count = 0
        forEachFileInZip { count++ }
        return count
    }

    /** @return if a file with type [traceType] linked to a [tag] exists in the artifact */
    @VisibleForTesting
    fun hasTraceFile(traceType: TraceType, tag: String = AssertionTag.ALL): Boolean {
        val descriptor = ResultFileDescriptor(traceType, tag)
        var found = false
        forEachFileInZip { found = found || (it.name == descriptor.fileNameInArtifact) }
        return found
    }
}
