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

package com.android.server.wm.flicker.service.assertors

import com.android.server.wm.flicker.readLayerTraceFromFile
import com.android.server.wm.flicker.readTestFile
import com.android.server.wm.flicker.readWmTraceFromFile
import com.android.server.wm.traces.common.tags.Tag
import com.android.server.wm.traces.common.tags.Transition
import com.google.common.truth.Truth
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters

/**
 * Contains tests for Ime Appear assertions. To run this test:
 * `atest FlickerLibTest:ImeAppearAssertionsTest`
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class ImeAppearAssertionsTest {
    private val jsonByteArray = readTestFile("assertors/config.json")
    private val assertions =
        AssertionConfigParser.parseConfigFile(String(jsonByteArray))
            .filter { it.transitionType == Transition.IME_APPEAR }

    private val imeAppearAssertor = TransitionAssertor(assertions) { }

    @Test
    fun testValidImeAppearTraces() {
        val wmTrace = readWmTraceFromFile(
            "assertors/ime/appear/WindowManagerTrace.winscope")
        val layersTrace = readLayerTraceFromFile(
            "assertors/ime/appear/SurfaceFlingerTrace.winscope")
        val errorTrace = imeAppearAssertor.analyze(VALID_IME_APPEAR_TAG, wmTrace, layersTrace)

        Truth.assertThat(errorTrace).isEmpty()
    }

    @Test
    fun testInvalidImeAppearTraces() {
        val wmTrace = readWmTraceFromFile(
            "assertors/ime/appear/WindowManagerInvalidTrace.winscope")
        val layersTrace = readLayerTraceFromFile(
            "assertors/ime/appear/SurfaceFlingerInvalidTrace.winscope")
        val errorTrace = imeAppearAssertor.analyze(INVALID_IME_APPEAR_TAG, wmTrace, layersTrace)

        Truth.assertThat(errorTrace).isNotEmpty()
        Truth.assertThat(errorTrace.entries.size).isEqualTo(4)
    }

    companion object {
        private val VALID_IME_APPEAR_TAG = Tag(
            1,
            Transition.IME_APPEAR,
            true,
            layerId = 125
        )
        private val INVALID_IME_APPEAR_TAG = Tag(
            2,
            Transition.IME_APPEAR,
            true,
            layerId = 669
        )
    }
}