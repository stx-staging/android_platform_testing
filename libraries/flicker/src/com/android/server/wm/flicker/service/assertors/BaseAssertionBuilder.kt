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

import com.android.server.wm.flicker.service.config.AssertionInvocationGroup
import com.android.server.wm.flicker.service.config.AssertionInvocationGroup.POSTSUBMIT
import com.android.server.wm.flicker.service.config.FlickerServiceConfig.Companion.AssertionGroup
import com.android.server.wm.flicker.traces.FlickerSubjectException
import com.android.server.wm.flicker.traces.layers.LayersTraceSubject
import com.android.server.wm.flicker.traces.windowmanager.WindowManagerTraceSubject
import com.android.server.wm.traces.common.transition.Transition

/**
 * Base class for a FASS assertion
 */
abstract class BaseAssertionBuilder {
    internal var invocationGroup: AssertionInvocationGroup = POSTSUBMIT

    // Assertion name
    val name: String = this::class.java.simpleName

    protected abstract fun doEvaluate(
        t: Transition,
        wmSubject: WindowManagerTraceSubject,
        layerSubject: LayersTraceSubject
    )

    /**
     * Evaluate the assertion on a transition [Tag] in a [WindowManagerTraceSubject] and
     * [LayersTraceSubject]
     *
     * @param tag a list with all [TransitionTag]s
     * @param wmSubject Window Manager trace subject
     * @param layerSubject Surface Flinger trace subject
     */
    fun evaluate(
        transition: Transition,
        wmSubject: WindowManagerTraceSubject,
        layerSubject: LayersTraceSubject,
        assertionGroup: AssertionGroup
    ): AssertionResult {
        var assertionError: FlickerSubjectException? = null
        try {
            doEvaluate(transition, wmSubject, layerSubject)
        } catch (e: FlickerSubjectException) {
            assertionError = e
        }
        return AssertionResult(name, assertionGroup, invocationGroup, assertionError)
    }

    infix fun runAs(invocationGroup: AssertionInvocationGroup): BaseAssertionBuilder {
        this.invocationGroup = invocationGroup
        return this
    }

    override fun equals(other: Any?): Boolean {
        if (other == null) {
            return false
        }
        // Ensure both assertions are instances of the same class.
        return this::class == other::class
    }
}
