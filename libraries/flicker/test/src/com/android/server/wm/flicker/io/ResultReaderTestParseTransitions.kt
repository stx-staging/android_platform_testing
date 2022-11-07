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

import com.android.server.wm.flicker.TestTraces
import com.android.server.wm.flicker.readAssetAsFile
import com.android.server.wm.traces.common.ITrace
import java.io.File

/** Tests for [ResultReader] parsing [TraceType.TRANSITION] */
class ResultReaderTestParseTransitions : BaseResultReaderTestParseTrace() {
    override val assetFile: File
        get() = TestTraces.TransitionTrace.FILE
    override val traceName: String
        get() = "Transitions trace"
    override val startTimeTrace: TraceTime
        get() =
            TraceTime(
                TestTraces.TransitionTrace.START_TIME,
                TestTraces.TransactionTrace.START_TIME,
                0
            )
    override val endTimeTrace: TraceTime
        get() =
            TraceTime(TestTraces.TransitionTrace.END_TIME, TestTraces.TransactionTrace.END_TIME, 0)
    override val validSliceTime: TraceTime
        get() =
            TraceTime(
                TestTraces.TransitionTrace.VALID_SLICE_TIME,
                TestTraces.TransactionTrace.VALID_SLICE_TIME,
                0
            )
    override val invalidSliceTime: TraceTime
        get() =
            TraceTime(
                TestTraces.TransitionTrace.INVALID_SLICE_TIME,
                TestTraces.TransactionTrace.INVALID_SLICE_TIME,
                0
            )
    override val traceType: TraceType
        get() = TraceType.TRANSITION
    override val invalidSizeMessage: String
        get() = "Transitions trace cannot be empty"
    override val expectedSlicedTraceSize: Int
        get() = 1

    override fun writeTrace(writer: ResultWriter): ResultWriter {
        return super.writeTrace(writer).also {
            val trace = readAssetAsFile("transactions_trace.winscope")
            it.addTraceResult(TraceType.TRANSACTION, trace)
        }
    }

    override fun doParse(reader: ResultReader): ITrace<*>? {
        return reader.readTransitionsTrace()
    }

    override fun getTime(traceTime: TraceTime): Long {
        return traceTime.elapsedRealtimeNanos
    }
}
