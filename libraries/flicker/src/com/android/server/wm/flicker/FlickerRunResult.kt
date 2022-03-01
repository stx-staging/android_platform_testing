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
import androidx.annotation.VisibleForTesting
import com.android.compatibility.common.util.ZipUtil
import com.android.server.wm.flicker.assertions.AssertionData
import com.android.server.wm.flicker.assertions.FlickerAssertionError
import com.android.server.wm.flicker.assertions.FlickerAssertionErrorBuilder
import com.android.server.wm.flicker.assertions.FlickerSubject
import com.android.server.wm.flicker.dsl.AssertionTag
import com.android.server.wm.flicker.traces.FlickerTraceSubject
import com.android.server.wm.flicker.traces.eventlog.EventLogSubject
import com.android.server.wm.traces.common.windowmanager.WindowManagerTrace
import com.android.server.wm.flicker.traces.eventlog.FocusEvent
import com.android.server.wm.flicker.traces.layers.LayersTraceSubject
import com.android.server.wm.flicker.traces.windowmanager.WindowManagerTraceSubject
import com.android.server.wm.traces.common.layers.LayerTraceEntry
import com.android.server.wm.traces.common.layers.LayersTrace
import com.android.server.wm.traces.common.windowmanager.WindowManagerState
import java.io.File
import java.io.IOException
import java.lang.Exception
import java.nio.file.Path
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Defines the result of a flicker run
 */
class FlickerRunResult private constructor(
    /**
     * The trace files associated with the result (incl. screen recording)
     */
    _traceFile: Path?,
    /**
     * Determines which assertions to run (e.g., start, end, all, or a custom tag)
     */
    @JvmField var assertionTag: String,
    /**
     * Truth subject that corresponds to a [WindowManagerTrace] or [WindowManagerState]
     */
    internal val wmSubject: FlickerSubject?,
    /**
     * Truth subject that corresponds to a [LayersTrace] or [LayerTraceEntry]
     */
    internal val layersSubject: FlickerSubject?,
    /**
     * Truth subject that corresponds to a list of [FocusEvent]
     */
    @VisibleForTesting
    val eventLogSubject: EventLogSubject?
) {
    var traceFile = _traceFile
        private set

    private val traceName = traceFile?.fileName ?: "UNNAMED_TRACE"

    var status: RunStatus = RunStatus.UNDEFINED
        private set(value) {
            if (field != value) {
                if (field.isFailure) {
                    throw Exception("Status of run already set to a failed status $field and " +
                            "can't be changed to $value.")
                }
                field = value
                syncFileWithStatus()
            }
        }

    fun setRunFailed() {
        status = RunStatus.RUN_FAILED
    }

    val isSuccessfulRun: Boolean get() = !isFailedRun
    val isFailedRun: Boolean get() {
        require(status != RunStatus.UNDEFINED) {
            "RunStatus cannot be UNDEFINED for $traceName ($assertionTag)"
        }
        // Other types of failures can only happen if the run has succeeded
        return status == RunStatus.RUN_FAILED
    }

    private fun syncFileWithStatus() {
        // Since we don't expect this to run in a multi-threaded context this is fine
        val localTraceFile = traceFile
        if (localTraceFile != null) {
            try {
                val newFileName = "${status.prefix}_$traceName"
                val dst = localTraceFile.resolveSibling(newFileName)
                Utils.renameFile(localTraceFile, dst)
                traceFile = dst
            } catch (e: IOException) {
                Log.e(FLICKER_TAG, "Unable to update file status $this", e)
            }
        }
    }

    fun getSubjects(): List<FlickerSubject> {
        val result = mutableListOf<FlickerSubject>()

        wmSubject?.run { result.add(this) }
        layersSubject?.run { result.add(this) }
        eventLogSubject?.run { result.add(this) }

        return result
    }

    fun checkAssertion(assertion: AssertionData): FlickerAssertionError? {
        return try {
            assertion.checkAssertion(this)
            null
        } catch (error: Throwable) {
            status = RunStatus.ASSERTION_FAILED
            FlickerAssertionErrorBuilder()
                    .fromError(error)
                    .atTag(assertion.tag)
                    .withTrace(this.traceFile)
                    .build()
        }
    }

    /**
     * Parse a [trace] into a [SubjectType] asynchronously
     *
     * The parsed subject is available in [promise]
     */
    class AsyncSubjectParser<SubjectType : FlickerTraceSubject<*>>(
        val trace: Path,
        parser: ((Path) -> SubjectType?)?
    ) {
        val promise: Deferred<SubjectType?>? = parser?.run { SCOPE.async { parser(trace) } }
    }

    class Builder {
        private var wmTraceData: AsyncSubjectParser<WindowManagerTraceSubject>? = null
        private var layersTraceData: AsyncSubjectParser<LayersTraceSubject>? = null
        var screenRecording: Path? = null

        var status: RunStatus = RunStatus.UNDEFINED

        /**
         * List of focus events, if collected
         */
        var eventLog: List<FocusEvent>? = null

        /**
         * Parses a [WindowManagerTraceSubject]
         *
         * @param traceFile of the trace file to parse
         * @param parser lambda to parse the trace into a [WindowManagerTraceSubject]
         */
        fun setWmTrace(traceFile: Path, parser: (Path) -> WindowManagerTraceSubject?) {
            wmTraceData = AsyncSubjectParser(traceFile, parser)
        }

        /**
         * Parses a [LayersTraceSubject]
         *
         * @param traceFile of the trace file to parse
         * @param parser lambda to parse the trace into a [LayersTraceSubject]
         */
        fun setLayersTrace(traceFile: Path, parser: (Path) -> LayersTraceSubject?) {
            layersTraceData = AsyncSubjectParser(traceFile, parser)
        }

        private fun buildResult(
            assertionTag: String,
            wmSubject: FlickerSubject?,
            layersSubject: FlickerSubject?,
            traceFile: Path? = null,
            eventLogSubject: EventLogSubject? = null
        ): FlickerRunResult {
            return FlickerRunResult(
                traceFile,
                assertionTag,
                wmSubject,
                layersSubject,
                eventLogSubject
            )
        }

        /**
         * Builds a new [FlickerRunResult] for a trace
         *
         * @param assertionTag Tag to associate with the result
         * @param wmTrace WindowManager trace
         * @param layersTrace Layers trace
         */
        fun buildStateResult(
            assertionTag: String,
            wmTrace: WindowManagerTrace?,
            layersTrace: LayersTrace?
        ): FlickerRunResult {
            val wmSubject = wmTrace?.let { WindowManagerTraceSubject.assertThat(it).first() }
            val layersSubject = layersTrace?.let { LayersTraceSubject.assertThat(it).first() }
            return buildResult(assertionTag, wmSubject, layersSubject)
        }

        @VisibleForTesting
        fun buildEventLogResult(): FlickerRunResult {
            val events = eventLog ?: emptyList()
            return buildResult(
                AssertionTag.ALL,
                wmSubject = null,
                layersSubject = null,
                eventLogSubject = EventLogSubject.assertThat(events)
            )
        }

        @VisibleForTesting
        fun buildTraceResults(
            testName: String,
            iteration: Int
        ): List<FlickerRunResult> = runBlocking {
            val wmSubject = wmTraceData?.promise?.await()
            val layersSubject = layersTraceData?.promise?.await()

            val traceFile = compress(testName, iteration)
            val traceResult = buildResult(
                AssertionTag.ALL, wmSubject, layersSubject, traceFile = traceFile)
            val initialStateResult = buildResult(
                AssertionTag.START, wmSubject?.first(), layersSubject?.first())
            val finalStateResult = buildResult(
                AssertionTag.END, wmSubject?.last(), layersSubject?.last())

            listOf(initialStateResult, finalStateResult, traceResult)
        }

        private fun compress(testName: String, iteration: Int): Path? {
            val traceFiles = mutableListOf<File>()
            wmTraceData?.trace?.let { traceFiles.add(it.toFile()) }
            layersTraceData?.trace?.let { traceFiles.add(it.toFile()) }
            screenRecording?.let { traceFiles.add(it.toFile()) }

            val files = traceFiles.filter { it.exists() }
            if (files.isEmpty()) {
                return null
            }

            val firstFile = files.first()
            val compressedFile = firstFile.resolveSibling("${testName}_$iteration.zip")
            ZipUtil.createZip(traceFiles, compressedFile)
            traceFiles.forEach {
                it.delete()
            }

            return compressedFile.toPath()
        }

        fun buildAll(testName: String, iteration: Int, status: RunStatus): List<FlickerRunResult> {
            val results = buildTraceResults(testName, iteration).toMutableList()
            if (eventLog != null) {
                results.add(buildEventLogResult())
            }

            require(status != RunStatus.UNDEFINED) { "Valid RunStatus must be provided" }
            for (result in results) {
                result.status = status
            }

            return results
        }

        fun setResultFrom(resultSetter: IResultSetter) {
            resultSetter.setResult(this)
        }
    }

    interface IResultSetter {
        fun setResult(builder: Builder)
    }

    companion object {
        private val SCOPE = CoroutineScope(Dispatchers.IO + SupervisorJob())

        enum class RunStatus(val prefix: String = "", val isFailure: Boolean) {
            UNDEFINED("???", false),

            RUN_SUCCESS("UNCHECKED", false),
            ASSERTION_SUCCESS("PASS", false),

            RUN_FAILED("FAILED_RUN", true),
            PARSING_FAILURE("FAILED_PARSING", true),
            ASSERTION_FAILED("FAIL", true);

            companion object {
                fun merge(runStatuses: List<RunStatus>): RunStatus {
                    val precedence = listOf(ASSERTION_FAILED, RUN_FAILED, ASSERTION_SUCCESS)
                    for (status in precedence) {
                        if (runStatuses.any { it == status }) {
                            return status
                        }
                    }

                    return UNDEFINED
                }
            }
        }
    }
}
