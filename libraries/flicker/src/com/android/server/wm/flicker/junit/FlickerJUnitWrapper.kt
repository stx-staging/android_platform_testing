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
import androidx.annotation.VisibleForTesting
import androidx.test.platform.app.InstrumentationRegistry
import com.android.server.wm.flicker.FlickerBuilder
import com.android.server.wm.flicker.FlickerTest
import com.android.server.wm.flicker.Scenario
import java.lang.reflect.Modifier
import org.junit.runner.Description
import org.junit.runners.model.FrameworkMethod
import org.junit.runners.model.TestClass

object FlickerJUnitWrapper {
    private val instrumentation: Instrumentation = InstrumentationRegistry.getInstrumentation()

    internal fun validateConstructor(testClass: TestClass): List<Throwable> {
        val errors = mutableListOf<Throwable>()
        val ctor = testClass.javaClass.constructors.firstOrNull()
        if (ctor?.parameterTypes?.none { it == FlickerTest::class.java } != false) {
            errors.add(
                IllegalStateException(
                    "Constructor should have a parameter of type " +
                        FlickerTest::class.java.simpleName
                )
            )
        }
        return errors
    }

    /** Validate that the test has one method annotated with [FlickerBuilderProvider] */
    internal fun validateInstanceMethods(testClass: TestClass): List<Throwable> {
        val errors = mutableListOf<Throwable>()
        val methods = Utils.getCandidateProviderMethods(testClass)

        if (methods.isEmpty() || methods.size > 1) {
            val prefix = if (methods.isEmpty()) "One" else "Only one"
            errors.add(
                IllegalArgumentException(
                    "$prefix object should be annotated with @FlickerBuilderProvider"
                )
            )
        } else {
            val method = methods.first()

            if (Modifier.isStatic(method.method.modifiers)) {
                errors.add(IllegalArgumentException("Method ${method.name}() should not be static"))
            }
            if (!Modifier.isPublic(method.method.modifiers)) {
                errors.add(IllegalArgumentException("Method ${method.name}() should be public"))
            }
            if (method.returnType != FlickerBuilder::class.java) {
                errors.add(
                    IllegalArgumentException(
                        "Method ${method.name}() should return a " +
                            "${FlickerBuilder::class.java.simpleName} object"
                    )
                )
            }
            if (method.method.parameterTypes.isNotEmpty()) {
                errors.add(
                    IllegalArgumentException("Method ${method.name} should have no parameters")
                )
            }
        }

        return errors
    }

    internal fun computeTestMethods(
        testClass: TestClass,
        scenario: Scenario
    ): List<FrameworkMethod> =
        createHelpers(testClass, scenario).flatMap { it.computeTestMethods() }

    internal fun processTest(
        testClass: TestClass,
        scenario: Scenario,
        test: Any,
        description: Description?
    ) {
        createHelpers(testClass, scenario).forEach { it.processTest(test, description) }
    }

    private fun createHelpers(
        testClass: TestClass,
        scenario: Scenario
    ): List<LegacyFlickerJUnitHelper> =
        listOf(
            LegacyFlickerJUnitHelper(testClass, scenario, instrumentation),
            // FlickerServiceJUnitHelper(testClass, scenario, arguments, instrumentation)
            )

    @VisibleForTesting
    fun validateInstanceMethodsForTests(testClass: TestClass) = validateInstanceMethods(testClass)

    @VisibleForTesting
    fun validateConstructorForTests(testClass: TestClass) = validateConstructor(testClass)
}
