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
import com.android.server.wm.flicker.FlickerBuilder
import com.android.server.wm.flicker.FlickerTest
import com.google.common.truth.Truth
import kotlin.reflect.KClass
import org.junit.Test
import org.junit.runners.model.TestClass
import org.junit.runners.parameterized.TestWithParameters

/** Tests for [FlickerJUnitWrapper] */
@SuppressLint("VisibleForTests")
class FlickerJUnitWrapperTest {
    @Test
    fun passValidClass() {
        val test =
            TestWithParameters(
                "test",
                TestClass(TestUtils.DummyTestClassValid::class.java),
                listOf(TestUtils.VALID_ARGS_EMPTY)
            )
        var failures = FlickerJUnitWrapper.validateConstructorForTests(test.testClass)
        Truth.assertWithMessage("Failure count").that(failures).isEmpty()

        failures = FlickerJUnitWrapper.validateInstanceMethodsForTests(test.testClass)
        Truth.assertWithMessage("Failure count").that(failures).isEmpty()
    }

    @Test
    fun failNoProviderMethods() {
        assertFailProviderMethod(
            TestUtils.DummyTestClassEmpty::class,
            expectedExceptions =
                listOf("One object should be annotated with @FlickerBuilderProvider")
        )
    }

    @Test
    fun failMultipleProviderMethods() {
        assertFailProviderMethod(
            TestUtils.DummyTestClassMultipleProvider::class,
            expectedExceptions =
                listOf("Only one object should be annotated with @FlickerBuilderProvider")
        )
    }

    @Test
    fun failStaticProviderMethod() {
        assertFailProviderMethod(
            TestUtils.DummyTestClassProviderStatic::class,
            expectedExceptions = listOf("Method myMethod() should not be static")
        )
    }

    @Test
    fun failPrivateProviderMethod() {
        assertFailProviderMethod(
            TestUtils.DummyTestClassProviderPrivateVoid::class,
            expectedExceptions =
                listOf(
                    "Method myMethod() should be public",
                    "Method myMethod() should return a " +
                        "${FlickerBuilder::class.java.simpleName} object"
                )
        )
    }

    @Test
    fun failConstructorWithNoArguments() {
        assertFailConstructor(emptyList())
    }

    @Test
    fun failWithInvalidConstructorArgument() {
        assertFailConstructor(listOf(1, 2, 3))
    }

    private fun assertFailProviderMethod(cls: KClass<*>, expectedExceptions: List<String>) {
        val test =
            TestWithParameters("test", TestClass(cls.java), listOf(TestUtils.VALID_ARGS_EMPTY))
        val failures = FlickerJUnitWrapper.validateInstanceMethodsForTests(test.testClass)
        Truth.assertWithMessage("Failure count").that(failures).hasSize(expectedExceptions.count())
        expectedExceptions.forEachIndexed { idx, expectedException ->
            val failure = failures[idx]
            Truth.assertWithMessage("Failure")
                .that(failure)
                .hasMessageThat()
                .contains(expectedException)
        }
    }

    private fun assertFailConstructor(args: List<Any>) {
        val test =
            TestWithParameters("test", TestClass(TestUtils.DummyTestClassEmpty::class.java), args)
        val failures = FlickerJUnitWrapper.validateConstructorForTests(test.testClass)

        Truth.assertWithMessage("Failure count").that(failures).hasSize(1)

        val failure = failures.first()
        Truth.assertWithMessage("Expected failure")
            .that(failure)
            .hasMessageThat()
            .contains(
                "Constructor should have a parameter of type ${FlickerTest::class.simpleName}"
            )
    }
}
