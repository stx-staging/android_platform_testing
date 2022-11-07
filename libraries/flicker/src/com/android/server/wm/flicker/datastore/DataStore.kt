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

package com.android.server.wm.flicker.datastore

import androidx.annotation.VisibleForTesting
import com.android.server.wm.flicker.io.ResultData
import com.android.server.wm.flicker.service.assertors.AssertionResult
import com.android.server.wm.traces.common.IScenario

/** In memory data store for flicker transitions, assertions and results */
object DataStore {
    private val cachedResults = mutableMapOf<IScenario, ResultData>()
    private val cachedFassAssertions = mutableMapOf<IScenario, FassScenarioResultStore>()

    @VisibleForTesting
    fun clear() {
        cachedResults.clear()
        cachedFassAssertions.clear()
    }

    /** @return if the store has results for [scenario] */
    fun containsResult(scenario: IScenario): Boolean = cachedResults.containsKey(scenario)

    /**
     * Adds [result] to the store with [scenario] as id
     *
     * @throws IllegalStateException is [scenario] already exists in the data store
     */
    @Throws(IllegalStateException::class)
    fun addResult(scenario: IScenario, result: ResultData) {
        if (containsResult(scenario)) {
            error("Result for $scenario already in data store")
        }
        cachedResults[scenario] = result
    }

    /**
     * Replaces the old value [scenario] result in the store by [newResult]
     *
     * @throws IllegalStateException is [scenario] doesn't exist in the data store
     */
    @Throws(IllegalStateException::class)
    fun replaceResult(scenario: IScenario, newResult: ResultData) {
        if (!containsResult(scenario)) {
            error("Result for $scenario not in data store")
        }
        cachedResults[scenario] = newResult
    }

    /**
     * @return the result for [scenario]
     *
     * @throws IllegalStateException is [scenario] doesn't exist in the data store
     */
    @Throws(IllegalStateException::class)
    fun getResult(scenario: IScenario): ResultData =
        cachedResults[scenario] ?: error("No value for $scenario")

    fun getFassAssertions(scenario: IScenario): List<AssertionResult> =
        cachedFassAssertions[scenario]?.getAssertions() ?: emptyList()

    fun addFassResults(results: MutableList<AssertionResult>) {
        results.forEach { result ->
            val scenario = result.scenario
            cachedFassAssertions.putIfAbsent(scenario, FassScenarioResultStore(scenario))
            cachedFassAssertions[scenario]?.addResult(result)
        }
    }

    fun getFassResults(scenario: IScenario, assertionName: String): List<AssertionResult> {
        return cachedFassAssertions[scenario]?.getResults(assertionName)
            ?: error("Assertion with name $assertionName not found for scenario $scenario")
    }
}
