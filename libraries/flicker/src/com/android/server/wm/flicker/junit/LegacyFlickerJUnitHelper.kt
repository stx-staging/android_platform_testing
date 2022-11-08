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

package com.android.server.wm.flicker.junit

import android.app.Instrumentation
import android.util.Log
import androidx.annotation.VisibleForTesting
import com.android.server.wm.flicker.FLICKER_TAG
import com.android.server.wm.flicker.FlickerBuilder
import com.android.server.wm.flicker.Scenario
import com.android.server.wm.flicker.datastore.DataStore
import com.android.server.wm.flicker.runner.TransitionRunner
import com.android.server.wm.traces.common.IScenario
import org.junit.runner.Description
import org.junit.runners.model.FrameworkMethod
import org.junit.runners.model.TestClass

open class LegacyFlickerJUnitHelper(
    protected val testClass: TestClass,
    protected val scenario: Scenario,
    protected val instrumentation: Instrumentation
) {
    open fun computeTestMethods(): List<FrameworkMethod> = emptyList()

    open fun processTest(test: Any, description: Description?) {
        if (!DataStore.containsResult(scenario)) {
            Log.v(FLICKER_TAG, "Creating flicker object for $scenario")
            val builder = getFlickerBuilder(test)
            Log.v(FLICKER_TAG, "Creating flicker object for $scenario")
            val flicker = builder.build()
            val runner = TransitionRunner(scenario, instrumentation)
            Log.v(FLICKER_TAG, "Running transition for $scenario")
            runner.execute(flicker, description)
        }
    }

    private val providerMethod: FrameworkMethod
        get() =
            Utils.getCandidateProviderMethods(testClass).firstOrNull()
                ?: error("Provider method not found")

    private fun getFlickerBuilder(test: Any): FlickerBuilder {
        Log.v(FLICKER_TAG, "Obtaining flicker builder for $testClass")
        return providerMethod.invokeExplosively(test) as FlickerBuilder
    }

    @VisibleForTesting fun getScenarioForTests(): IScenario = scenario

    @VisibleForTesting
    fun processTestForTests(test: Any, description: Description?) =
        FlickerJUnitWrapper.processTest(testClass, scenario, test, description)
}
