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

package com.android.server.wm.flicker.service

import com.android.server.wm.flicker.service.assertors.AssertionResult
import junit.framework.Assert

class FlickerTestCase(val results: List<AssertionResult>) {

    // Used by the FlickerBlockJUnit4ClassRunner to identify the test method within this class
    annotation class InjectedTest

    @InjectedTest
    fun injectedTest(param: Any) {
        if (containsFailures) {
            Assert.fail(assertionMessage)
        }
    }

    private val containsFailures: Boolean get() = results.any { it.failed }

    private val assertionMessage: String get() {
        if (!containsFailures) {
            return "${results.size}/${results.size} PASSED"
        }

        if (results.size == 1) {
            return results[0].assertionError!!.message
        }

        return buildString {
            append("$failedCount/${results.size} FAILED\n")
            for (result in results) {
                if (result.failed) {
                    append("\n${result.assertionError!!.message.prependIndent("  ")}")
                }
            }
        }
    }

    private val failedCount: Int get() = results.filter { it.failed }.size
}
