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

package android.tools.device.flicker

import android.tools.common.CrossPlatform
import android.tools.common.flicker.FlickerService
import android.tools.common.flicker.ScenarioInstance
import android.tools.common.flicker.config.FlickerServiceConfig
import android.tools.common.flicker.extractors.CombinedScenarioExtractor
import android.tools.common.flicker.extractors.ScenarioExtractor
import android.tools.common.io.Reader

/** Contains the logic for Flicker as a Service. */
class FlickerServiceImpl(
    val scenarioExtractor: ScenarioExtractor =
        CombinedScenarioExtractor(FlickerServiceConfig.getExtractors())
) : FlickerService {
    /*override fun process(reader: IReader): Collection<AssertionResult> {
        return CrossPlatform.log.withTracing("FlickerService#process") {
            try {
                detectScenarios(reader)
                    .flatMap { it.generateAssertions() }
                    .map { it.execute() }
            } catch (exception: Throwable) {
                CrossPlatform.log.e("$FLICKER_TAG-ASSERT", "FAILED PROCESSING", exception)
                throw exception
            }
        }
    }*/

    override fun detectScenarios(reader: Reader): Collection<ScenarioInstance> {
        return CrossPlatform.log.withTracing("FlickerService#detectScenarios") {
            scenarioExtractor.extract(reader)
        }
    }
}
