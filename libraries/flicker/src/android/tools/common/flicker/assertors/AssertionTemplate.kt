/*
 * Copyright (C) 2023 The Android Open Source Project
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

package android.tools.common.flicker.assertors

import android.tools.common.flicker.ScenarioInstance
import android.tools.common.flicker.assertions.AssertionData
import android.tools.common.flicker.assertions.FlickerTest
import android.tools.common.flicker.assertions.ServiceFlickerTest

/** Base class for a FaaS assertion */
abstract class AssertionTemplate(name: String? = null) {
    protected open val name =
        this::class.simpleName
            ?: name ?: error("Must provide a name to assertions when using anonymous classes.")
    val id
        get() = AssertionId(name)

    private fun qualifiedAssertionName(scenarioInstance: ScenarioInstance): String =
        "${scenarioInstance.type}::$name"

    /** Evaluates assertions */
    abstract fun doEvaluate(scenarioInstance: ScenarioInstance, flicker: FlickerTest)

    fun createAssertions(scenarioInstance: ScenarioInstance): Collection<AssertionData> {
        val flicker = ServiceFlickerTest(qualifiedAssertionName(scenarioInstance))
        doEvaluate(scenarioInstance, flicker)

        return flicker.assertions
    }

    override fun equals(other: Any?): Boolean {
        if (other == null) {
            return false
        }
        // Ensure both assertions are instances of the same class.
        return this::class == other::class
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}
