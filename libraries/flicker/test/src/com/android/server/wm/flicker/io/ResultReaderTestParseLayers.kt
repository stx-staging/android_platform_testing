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

/** Tests for [ResultReader] parsing [TraceType.SF] */
class ResultReaderTestParseLayers : BaseResultReaderTestParseTrace() {
    override val assetFile: File
        get() = TestTraces.LayerTrace.FILE
    override val traceName: String
        get() = "Layers trace"
    override val startTimeTrace: TraceTime
        get() = TraceTime(0, TestTraces.LayerTrace.START_TIME, 0)
    override val endTimeTrace: TraceTime
        get() = TraceTime(0, TestTraces.LayerTrace.END_TIME, 0)
    override val validSliceTime: TraceTime
        get() = TraceTime(0, TestTraces.LayerTrace.SLICE_TIME, 0)
    override val invalidSliceTime: TraceTime
        get() = startTimeTrace
    override val traceType: TraceType
        get() = TraceType.SF
    override val expectedSlicedTraceSize: Int
        get() = 2

    override fun doParse(reader: ResultReader): ITrace<*>? {
        return reader.readLayersTrace()
    }

    override fun getTime(traceTime: TraceTime): Long {
        return traceTime.systemTime
    }
}
