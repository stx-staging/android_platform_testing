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

import com.android.server.wm.InitRule
import com.android.server.wm.flicker.DEFAULT_TRACE_CONFIG
import com.android.server.wm.flicker.deleteIfExists
import com.android.server.wm.flicker.io.ResultReader
import com.android.server.wm.flicker.newTestResultWriter
import com.android.server.wm.flicker.outputFileName
import com.android.server.wm.traces.common.AssertionTag
import com.android.server.wm.traces.common.assertions.SubjectsParser
import com.android.server.wm.traces.common.io.RunStatus
import com.google.common.truth.Truth
import org.junit.ClassRule
import org.junit.Test

/** Tests for [SubjectsParser] */
class SubjectsParserTest {

    @Test
    fun isEmptyWhenFileNotFound() {
        val data = newTestResultWriter().write()
        outputFileName(RunStatus.RUN_EXECUTED).deleteIfExists()
        val parser = SubjectsParser(ResultReader(data, DEFAULT_TRACE_CONFIG))
        Truth.assertWithMessage("Is empty").that(parser.getSubjects(AssertionTag.ALL)).isEmpty()
    }

    companion object {
        @ClassRule @JvmField val initRule = InitRule()
    }
}
