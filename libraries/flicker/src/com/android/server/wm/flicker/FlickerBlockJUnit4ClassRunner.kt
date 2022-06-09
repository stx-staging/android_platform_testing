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

package com.android.server.wm.flicker

import android.platform.test.util.TestFilter
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import com.android.server.wm.flicker.annotation.FlickerServiceCompatible
import com.android.server.wm.flicker.dsl.FlickerBuilder
import com.android.server.wm.flicker.helpers.isShellTransitionsEnabled
import com.android.server.wm.flicker.service.FlickerFrameworkMethod
import com.android.server.wm.flicker.service.FlickerTestCase
import com.android.server.wm.flicker.service.assertors.AssertionResult
import com.android.server.wm.flicker.service.config.AssertionInvocationGroup
import java.lang.reflect.Modifier
import org.junit.Test
import org.junit.internal.runners.statements.RunAfters
import org.junit.runner.notification.RunNotifier
import org.junit.runners.Parameterized
import org.junit.runners.model.FrameworkField
import org.junit.runners.model.FrameworkMethod
import org.junit.runners.model.Statement
import org.junit.runners.model.TestClass
import org.junit.runners.parameterized.BlockJUnit4ClassRunnerWithParameters
import org.junit.runners.parameterized.TestWithParameters

/**
 * Implements the JUnit 4 standard test case class model, parsing from a flicker DSL.
 *
 * Supports both assertions in {@link org.junit.Test} and assertions defined in the DSL
 *
 * When using this runner the default `atest class#method` command doesn't work.
 * Instead use: -- --test-arg \
 *     com.android.tradefed.testtype.AndroidJUnitTest:instrumentation-arg:filter-tests:=<TEST_NAME>
 *
 * For example:
 * `atest FlickerTests -- \
 *     --test-arg com.android.tradefed.testtype.AndroidJUnitTest:instrumentation-arg:filter-tests\
 *     :=com.android.server.wm.flicker.close.\
 *     CloseAppBackButtonTest#launcherWindowBecomesVisible[ROTATION_90_GESTURAL_NAV]`
 */
class FlickerBlockJUnit4ClassRunner @JvmOverloads constructor(
    test: TestWithParameters,
    private val parameters: Array<Any> = test.parameters.toTypedArray(),
    private val flickerTestParameter: FlickerTestParameter? =
        parameters.filterIsInstance<FlickerTestParameter>().firstOrNull()
) : BlockJUnit4ClassRunnerWithParameters(test) {
    private var flickerBuilderProviderMethod: FrameworkMethod? = null

    private val arguments = InstrumentationRegistry.getArguments()
    // null parses to false (so defaults to running all FaaS tests)
    private val isBlockingTest = arguments.getString("faas:blocking").toBoolean()

    /**
     * {@inheritDoc}
     */
    override fun validateInstanceMethods(errors: MutableList<Throwable>) {
        validateFlickerObject(errors)
        super.validateInstanceMethods(errors)
    }

    /**
     * Returns the methods that run tests.
     * Is ran after validateInstanceMethods, so flickerBuilderProviderMethod should be set.
     */
    override fun computeTestMethods(): List<FrameworkMethod> {
        return computeTests()
    }

    private fun computeTests(): MutableList<FrameworkMethod> {
        val tests = mutableListOf<FrameworkMethod>()
        tests.addAll(super.computeTestMethods())

        // Don't compute when called from validateInstanceMethods since this will fail
        // as the parameters will not be set. And AndroidLogOnlyBuilder is a non-executing runner
        // used to run tests in dry-run mode so we don't want to execute in flicker transition in
        // that case either.
        val stackTrace = Thread.currentThread().stackTrace
        if (stackTrace.none { it.methodName == "validateInstanceMethods" } &&
            stackTrace.none {
                it.className == "androidx.test.internal.runner.AndroidLogOnlyBuilder"
            }
        ) {
            require(flickerTestParameter != null) {
                "Can't computeTests with null flickerTestParameter"
            }

            val hasFlickerServiceCompatibleAnnotation = TestClass(super.createTest()::class.java)
                .annotations.filterIsInstance<FlickerServiceCompatible>().firstOrNull() != null

            if (hasFlickerServiceCompatibleAnnotation && isShellTransitionsEnabled) {
                if (!flickerTestParameter.isInitialized) {
                    Log.v(FLICKER_TAG, "Flicker object is not yet initialized")
                    val test = super.createTest()
                    injectFlickerOnTestParams(test)
                }

                tests.addAll(computeFlickerServiceTests(isBlockingTest))
            }
        }

        return tests
    }

    /**
     * Runs the flicker transition to collect the traces and run FaaS on them to get the FaaS
     * results and then create functional test results for each of them.
     */
    private fun computeFlickerServiceTests(onlyBlockingAssertions: Boolean): List<FrameworkMethod> {
        require(flickerTestParameter != null) {
            "Can't computeFlickerServiceTests with null flickerTestParameter"
        }

        val flickerTestMethods = mutableListOf<FlickerFrameworkMethod>()

        val flicker = flickerTestParameter.flicker
        if (flicker.result == null) {
            flicker.execute()
        }

        // TODO: Figure out how we can report this without aggregation to have more precise and
        //       granular data on the actual failure rate.
        for (aggregatedResult in aggregateFaasResults(flicker.faas.assertionResults)
            .entries.iterator()) {
            val testName = aggregatedResult.key
            var results = aggregatedResult.value
            if (onlyBlockingAssertions) {
                results = results.filter { it.invocationGroup == AssertionInvocationGroup.BLOCKING }
            }
            if (results.isEmpty()) {
                continue
            }

            val injectedTestCase = FlickerTestCase(results)
            val mockedTestMethod = TestClass(injectedTestCase.javaClass)
                .getAnnotatedMethods(Test::class.java).first()
            val mockedFrameworkMethod = FlickerFrameworkMethod(
                mockedTestMethod.method, injectedTestCase, testName
            )
            flickerTestMethods.add(mockedFrameworkMethod)
        }

        return flickerTestMethods
    }

    /**
     * {@inheritDoc}
     */
    override fun getChildren(): MutableList<FrameworkMethod> {
        val arguments = InstrumentationRegistry.getArguments()
        val validChildren = super.getChildren().filter {
            val childDescription = describeChild(it)
            TestFilter.isFilteredOrUnspecified(arguments, childDescription)
        }
        return validChildren.toMutableList()
    }

    /**
     * {@inheritDoc}
     */
    override fun classBlock(notifier: RunNotifier): Statement {
        val statement = childrenInvoker(notifier)
        val cleanUpMethod = getFlickerCleanUpMethod()
        val frameworkMethod = FrameworkMethod(cleanUpMethod)
        return RunAfters(statement, listOf(frameworkMethod), flickerTestParameter)
    }

    /**
     * Adds to `errors` for each method annotated with `@Test`that
     * is not a public, void instance method with no arguments.
     */
    fun validateFlickerObject(errors: MutableList<Throwable>) {
        val methods = testClass.getAnnotatedMethods(FlickerBuilderProvider::class.java)

        if (methods.isEmpty() || methods.size > 1) {
            val prefix = if (methods.isEmpty()) "One" else "Only one"
            errors.add(Exception("$prefix object should be annotated with @FlickerObject"))
        } else {
            val method = methods.first()

            if (Modifier.isStatic(method.method.modifiers)) {
                errors.add(Exception("Method ${method.name}() should not be static"))
            }
            if (!Modifier.isPublic(method.method.modifiers)) {
                errors.add(Exception("Method ${method.name}() should be public"))
            }
            if (method.returnType != FlickerBuilder::class.java) {
                errors.add(
                    Exception(
                        "Method ${method.name}() should return a " +
                            "${FlickerBuilder::class.java.simpleName} object"
                    )
                )
            }
            if (method.method.parameterTypes.isNotEmpty()) {
                errors.add(Exception("Method ${method.name} should have no parameters"))
            }
        }

        if (errors.isEmpty()) {
            flickerBuilderProviderMethod = methods.first()
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun createTest(): Any {
        val test = super.createTest()
        if (flickerTestParameter?.isInitialized != true) {
            Log.v(FLICKER_TAG, "Flicker object is not yet initialized")
            injectFlickerOnTestParams(test)
        }

        val flicker = flickerTestParameter?.flicker
        return test
    }

    /**
     * Builds a flicker object and assigns it to the test parameters
     */
    private fun injectFlickerOnTestParams(test: Any) {
        val flickerTestParameter = flickerTestParameter
        val flickerBuilderProviderMethod = flickerBuilderProviderMethod
        if (flickerTestParameter != null && flickerBuilderProviderMethod != null) {
            val testClass = test::class.java
            val testName = testClass.simpleName
            Log.v(
                FLICKER_TAG,
                "Creating flicker object for $testName and adding it into " +
                    "test parameter"
            )

            val isFlickerServiceCompatible = TestClass(testClass).annotations
                .filterIsInstance<FlickerServiceCompatible>().firstOrNull() != null
            if (isFlickerServiceCompatible) {
                flickerTestParameter.enableFaas()
            }

            val builder = flickerBuilderProviderMethod.invokeExplosively(test) as FlickerBuilder
            flickerTestParameter.initialize(builder, testName)
        } else {
            Log.v(
                FLICKER_TAG,
                "Missing flicker builder provider method " +
                    "in ${test::class.java.simpleName}"
            )
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun validateConstructor(errors: MutableList<Throwable>) {
        super.validateConstructor(errors)

        if (errors.isEmpty()) {
            // should have only one constructor, otherwise parent
            // validator will create an exception
            val ctor = testClass.javaClass.constructors.first()
            if (ctor.parameterTypes.none { it == FlickerTestParameter::class.java }) {
                errors.add(
                    Exception(
                        "Constructor should have a parameter of type " +
                            FlickerTestParameter::class.java.simpleName
                    )
                )
            }
        }
    }

    /**
     * Obtains the method to clean up a flicker object cache,
     * necessary to release memory after a configuration is executed
     */
    private fun getFlickerCleanUpMethod() = FlickerTestParameter::class.java.getMethod("clear")

    private fun getAnnotatedFieldsByParameter(): List<FrameworkField?> {
        return testClass.getAnnotatedFields(Parameterized.Parameter::class.java)
    }

    private fun getInjectionType(): String {
        return if (fieldsAreAnnotated()) {
            "FIELD"
        } else {
            "CONSTRUCTOR"
        }
    }

    private fun fieldsAreAnnotated(): Boolean {
        return !getAnnotatedFieldsByParameter().isEmpty()
    }

    private fun aggregateFaasResults(
        assertionResults: MutableList<AssertionResult>
    ): Map<String, List<AssertionResult>> {
        val aggregatedResults = mutableMapOf<String, MutableList<AssertionResult>>()
        for (result in assertionResults) {
            val testName = "FaaS_${result.scenario.description}_${result.assertionName}"
            if (!aggregatedResults.containsKey(testName)) {
                aggregatedResults[testName] = mutableListOf()
            }
            aggregatedResults[testName]!!.add(result)
        }
        return aggregatedResults
    }
}
