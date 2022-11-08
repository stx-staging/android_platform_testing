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

import android.annotation.SuppressLint
import android.app.Instrumentation
import androidx.test.platform.app.InstrumentationRegistry
import com.android.server.wm.flicker.FlickerTest
import com.android.server.wm.flicker.Scenario
import com.android.server.wm.flicker.datastore.DataStore
import com.google.common.truth.Truth
import org.junit.Before
import org.junit.Test
import org.junit.runner.Description
import org.junit.runners.model.TestClass
import org.junit.runners.parameterized.TestWithParameters

/** Tests for [LegacyFlickerJUnitHelper] */
@SuppressLint("VisibleForTests")
class LegacyFlickerJUnitHelperTest {
    private val instrumentation: Instrumentation = InstrumentationRegistry.getInstrumentation()

    @Before
    fun setup() {
        DataStore.clear()
    }

    @Test
    fun hasNoTestMethods() {
        val scenario =
            Scenario(
                TestUtils.DummyTestClassValid::class.java.name,
                TestUtils.VALID_ARGS_EMPTY.config
            )
        val test =
            TestWithParameters(
                "test",
                TestClass(TestUtils.DummyTestClassValid::class.java),
                listOf(TestUtils.VALID_ARGS_EMPTY)
            )
        val helper = LegacyFlickerJUnitHelper(test.testClass, scenario, instrumentation)
        Truth.assertWithMessage("Test method count").that(helper.computeTestMethods()).isEmpty()
    }

    @Test
    fun runTransitionAndAddToDatastore() {
        val scenario =
            Scenario(
                TestUtils.DummyTestClassValid::class.java.name,
                TestUtils.VALID_ARGS_EMPTY.config
            )
        val test =
            TestWithParameters(
                "test",
                TestClass(TestUtils.DummyTestClassValid::class.java),
                listOf(TestUtils.VALID_ARGS_EMPTY)
            )
        val helper = LegacyFlickerJUnitHelper(test.testClass, scenario, instrumentation)
        TestUtils.executionCount = 0
        repeat(3) {
            helper.processTestForTests(
                test = TestUtils.DummyTestClassValid(FlickerTest(mutableMapOf())),
                Description.createTestDescription("cls$it", "test$it")
            )
        }

        Truth.assertWithMessage("Executed").that(TestUtils.executionCount).isEqualTo(1)
        Truth.assertWithMessage("In Datastore")
            .that(DataStore.containsResult(TestUtils.DummyTestClassValid.SCENARIO))
            .isTrue()
    }
}
