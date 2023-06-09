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

package android.tools.common.flicker.assertions

import android.tools.common.CrossPlatform
import android.tools.common.FLICKER_TAG
import android.tools.common.flicker.AssertionInvocationGroup
import android.tools.common.flicker.assertors.AssertionResult
import android.tools.common.flicker.assertors.AssertionResultImpl
import android.tools.common.io.IReader

class ScenarioAssertionImpl(
    private val reader: IReader,
    private val assertionData: AssertionData,
    override val stabilityGroup: AssertionInvocationGroup,
    private val assertionRunner: AssertionRunner = ReaderAssertionRunner(reader)
) : ScenarioAssertion {
    override fun execute(): AssertionResult =
        CrossPlatform.log.withTracing("executeAssertion") {
            AssertionResultImpl(
                    assertionData,
                    assertionRunner.runAssertion(assertionData),
                    stabilityGroup
                )
                .also { log(it) }
        }

    private fun log(result: AssertionResult) {
        if (result.failed) {
            CrossPlatform.log.w(
                "$FLICKER_TAG-SERVICE",
                "${result.assertion} FAILED :: " +
                    (result.assertionError?.message ?: "<NO ERROR MESSAGE>")
            )
        } else {
            CrossPlatform.log.w("$FLICKER_TAG-SERVICE", "${result.assertion} PASSED")
        }
    }
}
