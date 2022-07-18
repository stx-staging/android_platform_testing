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

package com.android.server.wm.flicker

import android.app.Instrumentation
import androidx.test.platform.app.InstrumentationRegistry
import com.android.server.wm.flicker.dsl.FlickerBuilder
import com.android.server.wm.flicker.helpers.SampleAppHelper
import com.android.server.wm.flicker.helpers.wakeUpAndGoToHomeScreen
import com.google.common.truth.Truth
import org.junit.Assert
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runner.notification.RunNotifier
import org.junit.runners.MethodSorters
import org.junit.runners.Parameterized
import org.junit.runners.model.TestClass
import org.junit.runners.parameterized.TestWithParameters
import org.mockito.ArgumentMatchers.argThat
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

/**
 * Contains [FlickerBlockJUnit4ClassRunnerTest] tests.
 *
 * To run this test: `atest FlickerLibTest:FlickerBlockJUnit4ClassRunnerTest`
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class FlickerBlockJUnit4ClassRunnerTest {

    @Test
    fun doesNotRunWithEmptyTestParameter() {
        val testClass = TestClass(SimpleFaasTest::class.java)
        val test = TestWithParameters("[TEST]", testClass, listOf())
        try {
            val runner = FlickerBlockJUnit4ClassRunner(test)
            runner.run(RunNotifier())
            throw Throwable("Expected runner to fail but did not")
        } catch (e: Throwable) {
            Truth.assertThat(e).hasMessageThat()
                    .contains("No FlickerTestParameter provided for FlickerRunner")
        }
    }

    @Test
    fun doesNotRunWithoutValidFlickerTestParameter() {
        val testClass = TestClass(SimpleFaasTest::class.java)
        val test = TestWithParameters("[TEST]", testClass, listOf("invalid param"))
        try {
            val runner = FlickerBlockJUnit4ClassRunner(test)
            runner.run(RunNotifier())
            throw Throwable("Expected runner to fail but did not")
        } catch (e: Throwable) {
            Truth.assertThat(e).hasMessageThat()
                    .contains("No FlickerTestParameter provided for FlickerRunner")
        }
    }

    @Test
    fun runsWithValidFlickerTestParameter() {
        val testClass = TestClass(SimpleFaasTest::class.java)
        val parameters = FlickerTestParameterFactory.getInstance()
                .getConfigNonRotationTests()
        val test = TestWithParameters("[TEST]", testClass, listOf(parameters[0]))
        val runner = FlickerBlockJUnit4ClassRunner(test)
        runner.run(RunNotifier())
    }

    @Test
    fun transitionNotRerunWithFaasEnabled() {
        val repetitions = 3
        transitionRunCount = 0
        val testClass = TestClass(SimpleFaasTest::class.java)
        val parameters = FlickerTestParameterFactory.getInstance()
                .getConfigNonRotationTests(repetitions = repetitions)
        val test = TestWithParameters("[TEST]", testClass, listOf(parameters[0]))

        val runner = FlickerBlockJUnit4ClassRunner(test)
        runner.run(RunNotifier())
        val executionErrors = parameters[0].flicker.result!!.executionErrors
        Truth.assertWithMessage("No flicker execution errors were expected but got some ::" +
                executionErrors.joinToString())
                .that(executionErrors).isEmpty()

        Assert.assertEquals(repetitions, transitionRunCount)
        transitionRunCount = 0
    }

    @Test
    fun reportsExecutionErrors() {
        checkTestRunReportsExecutionErrors(AlwaysFailExecutionTestClass::class.java)
    }

    @Test
    fun reportsExecutionErrorsWithSomeSuccessfulRuns() {
        checkTestRunReportsExecutionErrors(OneOfThreeFailExecutionTestClass::class.java)
    }

    private fun checkTestRunReportsExecutionErrors(klass: Class<*>) {
        val testClass = TestClass(klass)
        val parameters = FlickerTestParameterFactory.getInstance()
                .getConfigNonRotationTests(repetitions = 3)
        val test = TestWithParameters("[TEST]", testClass, listOf(parameters[0]))

        val runner = FlickerBlockJUnit4ClassRunner(test)
        val notifier = mock(RunNotifier::class.java)

        runner.run(notifier)
        verify(notifier).fireTestFailure(argThat {
            failure -> failure.message.contains(TRANSITION_FAILURE_MESSAGE) &&
                failure.description.isTest &&
                failure.description.displayName == "test[TEST](${klass.name})"
        })
    }

    /**
     * Below are all the mock test classes uses for testing purposes
     */

    @RunWith(Parameterized::class)
    @Parameterized.UseParametersRunnerFactory(FlickerParametersRunnerFactory::class)
    class SimpleFaasTest(private val testSpec: FlickerTestParameter) {
        val instrumentation: Instrumentation = InstrumentationRegistry.getInstrumentation()
        private val testApp: SampleAppHelper = SampleAppHelper(instrumentation)

        @FlickerBuilderProvider
        fun buildFlicker(): FlickerBuilder {
            return FlickerBuilder(instrumentation).apply {
                setup {
                    test {
                        device.wakeUpAndGoToHomeScreen()
                    }
                }
                transitions {
                    transitionRunCount++
                    testApp.launchViaIntent(wmHelper)
                    wmHelper.StateSyncBuilder().withFullScreenApp(testApp).waitForAndVerify()
                }
                teardown {
                    test {
                        testApp.exit(wmHelper)
                    }
                }
            }
        }

        @Test
        fun test() {
            testSpec.assertWm {
                // Random test to make sure flicker transition is executed
                this.visibleWindowsShownMoreThanOneConsecutiveEntry()
            }
        }
    }

    @RunWith(Parameterized::class)
    @Parameterized.UseParametersRunnerFactory(FlickerParametersRunnerFactory::class)
    class AlwaysFailExecutionTestClass(private val testSpec: FlickerTestParameter) {
        val instrumentation: Instrumentation = InstrumentationRegistry.getInstrumentation()

        @FlickerBuilderProvider
        fun buildFlicker(): FlickerBuilder {
            return FlickerBuilder(instrumentation).apply {
                transitions {
                    throw Exception(TRANSITION_FAILURE_MESSAGE)
                }
            }
        }

        @Test
        fun test() {
            testSpec.assertWm {
                // Random test to make sure flicker transition is executed
                this.visibleWindowsShownMoreThanOneConsecutiveEntry()
            }
        }
    }

    @RunWith(Parameterized::class)
    @Parameterized.UseParametersRunnerFactory(FlickerParametersRunnerFactory::class)
    class OneOfThreeFailExecutionTestClass(private val testSpec: FlickerTestParameter) {
        val instrumentation: Instrumentation = InstrumentationRegistry.getInstrumentation()

        @FlickerBuilderProvider
        fun buildFlicker(): FlickerBuilder {
            return FlickerBuilder(instrumentation).apply {
                transitions {
                    if ((testSpec.currentIteration + 1) % 3 == 0) {
                        throw Exception(TRANSITION_FAILURE_MESSAGE)
                    }
                }
            }
        }

        @Test
        fun test() {
            testSpec.assertWm {
                // Random test to make sure flicker transition is executed
                this.visibleWindowsShownMoreThanOneConsecutiveEntry()
            }
        }
    }

    companion object {
        const val TRANSITION_FAILURE_MESSAGE = "Transition execution failed"

        var transitionRunCount = 0
    }
}
