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

import com.android.server.wm.flicker.traces.layers.LayersTraceSubject
import com.android.server.wm.flicker.traces.windowmanager.WindowManagerTraceSubject
import com.android.server.wm.traces.common.layers.LayersTrace
import com.android.server.wm.traces.common.transition.Transition
import com.android.server.wm.traces.common.windowmanager.WindowManagerTrace

/**
 * Class that runs FASS assertions.
 */
class TransitionAsserter(
    internal val assertions: List<AssertionData>,
    private val logger: (String) -> Unit
) {
    /** {@inheritDoc} */
    fun analyze(
        transition: Transition,
        _wmTrace: WindowManagerTrace?,
        _layersTrace: LayersTrace?
    ): List<AssertionResult> {
        var wmTrace = _wmTrace
        if (wmTrace != null && wmTrace.entries.isEmpty()) {
            // Empty trace, nothing to assert on the wmTrace
            logger("Passed an empty wmTrace")
            wmTrace = null
        }

        var layersTrace = _layersTrace
        if (layersTrace != null && layersTrace.entries.isEmpty()) {
            layersTrace = null
        }

        require(wmTrace != null || layersTrace != null)

        logger.invoke("Running assertions...")
        return runAssertionsOnSubjects(transition, wmTrace, layersTrace, assertions)
    }

    private fun runAssertionsOnSubjects(
        transition: Transition,
        wmTrace: WindowManagerTrace?,
        layersTrace: LayersTrace?,
        assertions: List<AssertionData>
    ): List<AssertionResult> {
        val results: MutableList<AssertionResult> = mutableListOf()

        assertions.forEach {
            val assertion = it.assertionBuilder
            logger.invoke("Running assertion $assertion for ${it.assertionGroup}")
            val wmSubject = if (wmTrace != null) {
                WindowManagerTraceSubject.assertThat(wmTrace)
            } else {
                null
            }
            val layersSubject = if (layersTrace != null) {
                LayersTraceSubject.assertThat(layersTrace)
            } else {
                null
            }
            val result = assertion.evaluate(transition, wmSubject, layersSubject, it.assertionGroup)
            results.add(result)
        }

        return results
    }
}
