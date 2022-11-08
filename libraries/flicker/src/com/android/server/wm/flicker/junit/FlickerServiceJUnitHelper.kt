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
import android.os.Bundle
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import com.android.server.wm.flicker.FLICKER_TAG
import com.android.server.wm.flicker.Scenario
import com.android.server.wm.flicker.annotation.FlickerServiceCompatible
import com.android.server.wm.flicker.datastore.DataStore
import com.android.server.wm.flicker.helpers.IS_FAAS_ENABLED
import com.android.server.wm.flicker.helpers.isShellTransitionsEnabled
import com.android.server.wm.flicker.service.FlickerFrameworkMethod
import kotlin.system.measureTimeMillis
import org.junit.runner.Description
import org.junit.runners.model.FrameworkMethod
import org.junit.runners.model.TestClass

class FlickerServiceJUnitHelper(
    testClass: TestClass,
    scenario: Scenario,
    instrumentation: Instrumentation
) : LegacyFlickerJUnitHelper(testClass, scenario, instrumentation) {

    private val arguments: Bundle = InstrumentationRegistry.getArguments()

    private val onlBlocking: Boolean
        get() = arguments.getString("faas:blocking").toBoolean()

    private val isClassFlickerServiceCompatible: Boolean
        get() =
            testClass.annotations.filterIsInstance<FlickerServiceCompatible>().firstOrNull() != null

    override fun computeTestMethods(): List<FrameworkMethod> {
        val tests = mutableListOf<FrameworkMethod>()

        if (shouldComputeTestMethods()) {
            val flickerTests: List<FrameworkMethod>
            measureTimeMillis { flickerTests = computeFlickerServiceTests() }
                .also {
                    Log.d(FLICKER_TAG, "Took ${it}ms to compute ${flickerTests.size} flicker tests")
                }
            tests.addAll(flickerTests)
        }
        return tests
    }

    private fun shouldComputeTestMethods(): Boolean {
        // Don't compute when called from validateInstanceMethods since this will fail
        // as the parameters will not be set. And AndroidLogOnlyBuilder is a non-executing runner
        // used to run tests in dry-run mode, so we don't want to execute in flicker transition in
        // that case either.
        val stackTrace = Thread.currentThread().stackTrace
        val isDryRun =
            stackTrace.any { it.methodName == "validateInstanceMethods" } ||
                stackTrace.any {
                    it.className == "androidx.test.internal.runner.AndroidLogOnlyBuilder"
                }

        return IS_FAAS_ENABLED &&
            isShellTransitionsEnabled &&
            isClassFlickerServiceCompatible &&
            !isDryRun
    }

    override fun processTest(test: Any, description: Description?) {
        super.processTest(test, description)
        if (IS_FAAS_ENABLED && isClassFlickerServiceCompatible) {
            scenario.enableFaas()
        }
    }

    /**
     * Runs the flicker transition to collect the traces and run FaaS on them to get the FaaS
     * results and then create functional test results for each of them.
     */
    private fun computeFlickerServiceTests(): List<FrameworkMethod> {
        val executeMethod = FlickerServiceCachedTestCase::class.java.getMethod("execute")
        val assertions = DataStore.getFassAssertions(scenario)
        return assertions.map {
            val testCase = FlickerServiceCachedTestCase(scenario, it.assertionName, onlBlocking)
            FlickerFrameworkMethod(executeMethod, testCase, "$scenario${it.assertionName}")
        }
    }
}
