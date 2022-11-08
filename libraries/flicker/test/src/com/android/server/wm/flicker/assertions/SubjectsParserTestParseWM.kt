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

package com.android.server.wm.flicker.assertions

import android.annotation.SuppressLint
import com.android.server.wm.flicker.TestTraces
import com.android.server.wm.flicker.io.TraceType
import com.android.server.wm.flicker.traces.FlickerTraceSubject
import java.io.File

@SuppressLint("VisibleForTests")
class SubjectsParserTestParseWM : BaseSubjectsParserTestParse() {
    override val assetFile: File
        get() = TestTraces.WMTrace.FILE
    override val expectedStartTime: Long
        get() = TestTraces.WMTrace.START_TIME
    override val expectedEndTime: Long
        get() = TestTraces.WMTrace.END_TIME
    override val subjectName: String
        get() = "WM Trace"
    override val traceType: TraceType
        get() = TraceType.WM

    override fun doParseTrace(parser: TestSubjectsParser): FlickerTraceSubject<*>? =
        parser.doGetWmTraceSubject()

    override fun doParseState(parser: TestSubjectsParser, tag: String): FlickerSubject? =
        parser.doGetWmStateSubject(tag)
}
