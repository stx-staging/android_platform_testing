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
import com.android.server.wm.traces.common.ITrace
import java.io.File

/** Tests for [ResultReader] parsing [TraceType.TRANSACTION] */
class ResultReaderTestParseTransactions : BaseResultReaderTestParseTrace() {
    override val assetFile: File
        get() = TestTraces.TransactionTrace.FILE
    override val traceName: String
        get() = "Transactions trace"
    override val startTimeTrace: TraceTime
        get() = TraceTime(0, TestTraces.TransactionTrace.START_TIME, 0)
    override val endTimeTrace: TraceTime
        get() = TraceTime(0, TestTraces.TransactionTrace.END_TIME, 0)
    override val validSliceTime: TraceTime
        get() = TraceTime(0, TestTraces.TransactionTrace.VALID_SLICE_TIME, 0)
    override val invalidSliceTime: TraceTime
        get() = TraceTime(0, TestTraces.TransactionTrace.INVALID_SLICE_TIME, 0)
    override val traceType: TraceType
        get() = TraceType.TRANSACTION
    override val invalidSizeMessage: String
        get() = "Trimmed transactions trace cannot be empty"
    override val expectedSlicedTraceSize: Int
        get() = 2

    override fun doParse(reader: ResultReader): ITrace<*>? {
        return reader.readTransactionsTrace()
    }

    override fun getTime(traceTime: TraceTime): Long {
        return traceTime.systemTime
    }
}
