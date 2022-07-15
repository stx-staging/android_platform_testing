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

import com.android.server.wm.flicker.TransitionRunner.Companion.ExecutionError
import com.android.server.wm.flicker.assertions.AssertionData
import com.android.server.wm.flicker.assertions.FlickerAssertionError
import com.google.common.truth.Truth

/**
 * Result of a flicker run, including transitions, errors and create tags
 */
data class FlickerResult(
    /**
     * Result of each transition run
     */
    @JvmField val runResults: Collection<FlickerRunResult>,
    /**
     * List of test created during the execution
     */
    @JvmField val tags: Set<String> = setOf(),
    /**
     * Execution errors which happened during the execution of the Flicker test
     */
    @JvmField val executionErrors: List<ExecutionError> = listOf()
) {
    /** Successful runs on which we can run assertions */
    val successfulRuns: List<FlickerRunResult> = runResults.filter { it.isSuccessfulRun }
    /** Failed runs due to execution errors which we shouldn't run assertions on */
    private val failedRuns: List<FlickerRunResult> = runResults.filter { it.isFailedRun }

    /**
     * List of failures during assertion
     */
    private val failures: MutableList<FlickerAssertionError> = mutableListOf()

    /**
     * Run the assertion on the trace
     *
     * @throws AssertionError If the assertion fail or the transition crashed
     */
    internal fun checkAssertion(assertion: AssertionData): List<FlickerAssertionError> {
        Truth.assertWithMessage("Expected to have runResults but none were found")
                .that(runResults).isNotEmpty()
        Truth.assertWithMessage("No transitions were not executed successful")
                .that(successfulRuns).isNotEmpty()

        val currFailures = successfulRuns.mapNotNull { run -> run.checkAssertion(assertion) }
        failures.addAll(currFailures)
        return currFailures
    }

    fun isEmpty(): Boolean = executionErrors.isEmpty() && successfulRuns.isEmpty()

    fun isNotEmpty(): Boolean = !isEmpty()

    fun clearFromMemory() {
        runResults.forEach { it.clearFromMemory() }
    }

    fun lock() {
        runResults.forEach { it.lock() }
    }
}
