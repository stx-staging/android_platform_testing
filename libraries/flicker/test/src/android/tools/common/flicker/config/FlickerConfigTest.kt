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

package android.tools.common.flicker.config

import android.tools.assertThrows
import android.tools.common.flicker.AssertionInvocationGroup
import android.tools.common.flicker.FlickerConfig
import android.tools.common.flicker.ScenarioInstance
import android.tools.common.flicker.assertions.FlickerTest
import android.tools.common.flicker.assertors.AssertionTemplate
import android.tools.common.flicker.extractors.EntireTraceExtractor
import android.tools.common.flicker.extractors.ScenarioExtractor
import android.tools.common.io.Reader
import android.tools.getTraceReaderFromScenario
import com.google.common.truth.Truth
import org.junit.Test

class FlickerConfigTest {
    @Test
    fun canRegisterScenario() {
        val registry = FlickerConfig()

        registry.registerScenario(SOME_SCENARIO, EXTRACTOR_FOR_SOME_SCENARIO)

        val extractors = registry.getExtractors()
        Truth.assertThat(extractors).hasSize(1)
        Truth.assertThat(extractors.first().scenarioId).isEqualTo(SOME_SCENARIO)
    }

    @Test
    fun canRegisterScenarioWithAssertions() {
        val registry = FlickerConfig()

        var executed = false
        val assertion =
            object : AssertionTemplate("Mock Assertion") {
                override fun doEvaluate(scenarioInstance: ScenarioInstance, flicker: FlickerTest) {
                    flicker.assertLayers { executed = true }
                }
            }

        registry.registerScenario(
            SOME_SCENARIO,
            EXTRACTOR_FOR_SOME_SCENARIO,
            mapOf(assertion to AssertionInvocationGroup.BLOCKING)
        )

        val extractors = registry.getExtractors()
        Truth.assertThat(extractors).hasSize(1)
        Truth.assertThat(extractors.first().scenarioId).isEqualTo(SOME_SCENARIO)

        val reader = getTraceReaderFromScenario("AppLaunch")
        val extractedScenarioInstance = extractors.first().extract(reader)
        Truth.assertThat(extractedScenarioInstance).hasSize(1)
        val assertions = extractedScenarioInstance.first().generateAssertions()

        Truth.assertThat(assertions).hasSize(1)
        assertions.first().execute()
        Truth.assertThat(executed).isTrue()

        // TODO: Check invocation group is respected
    }

    @Test
    fun canUnregisterScenario() {
        val registry = FlickerConfig()

        registry.registerScenario(SOME_SCENARIO, EXTRACTOR_FOR_SOME_SCENARIO)

        var extractors = registry.getExtractors()
        Truth.assertThat(extractors).hasSize(1)

        registry.unregisterScenario(SOME_SCENARIO)
        extractors = registry.getExtractors()
        Truth.assertThat(extractors).hasSize(0)
    }

    // TODO: Require anonymous assertions to have a proper name

    @Test
    fun canUnregisterAssertion() {
        val registry = FlickerConfig()

        val assertion =
            object : AssertionTemplate("Mock Assertion") {
                override fun doEvaluate(scenarioInstance: ScenarioInstance, flicker: FlickerTest) {
                    flicker.assertLayers { error("Should never be called") }
                }
            }

        registry.registerScenario(
            SOME_SCENARIO,
            EXTRACTOR_FOR_SOME_SCENARIO,
            mapOf(assertion to AssertionInvocationGroup.BLOCKING)
        )
        registry.unregisterAssertion(SOME_SCENARIO, assertion.id)

        val extractors = registry.getExtractors()
        Truth.assertThat(extractors).hasSize(1)
        Truth.assertThat(extractors.first().scenarioId).isEqualTo(SOME_SCENARIO)

        val reader = getTraceReaderFromScenario("AppLaunch")
        val extractedScenarioInstance = extractors.first().extract(reader)
        Truth.assertThat(extractedScenarioInstance).hasSize(1)
        val assertions = extractedScenarioInstance.first().generateAssertions()

        Truth.assertThat(assertions).hasSize(0)
    }

    @Test
    fun canOverrideStabilityGroup() {
        val registry = FlickerConfig()

        val assertion =
            object : AssertionTemplate("Mock Assertion") {
                override fun doEvaluate(scenarioInstance: ScenarioInstance, flicker: FlickerTest) {
                    flicker.assertLayers { error("Should never be called") }
                }
            }

        registry.registerScenario(
            SOME_SCENARIO,
            EXTRACTOR_FOR_SOME_SCENARIO,
            mapOf(assertion to AssertionInvocationGroup.BLOCKING)
        )
        registry.overrideAssertionStabilityGroup(
            SOME_SCENARIO,
            assertion.id,
            AssertionInvocationGroup.NON_BLOCKING
        )

        val extractors = registry.getExtractors()
        Truth.assertThat(extractors).hasSize(1)
        Truth.assertThat(extractors.first().scenarioId).isEqualTo(SOME_SCENARIO)

        val reader = getTraceReaderFromScenario("AppLaunch")
        val extractedScenarioInstance = extractors.first().extract(reader)
        Truth.assertThat(extractedScenarioInstance).hasSize(1)
        val assertions = extractedScenarioInstance.first().generateAssertions()

        Truth.assertThat(assertions).hasSize(1)
        Truth.assertThat(assertions.first().stabilityGroup)
            .isEqualTo(AssertionInvocationGroup.NON_BLOCKING)
    }

    @Test
    fun registerAssertionToScenario() {
        val registry = FlickerConfig()

        var executed1 = false
        val assertion1 =
            object : AssertionTemplate("Mock Assertion 1") {
                override fun doEvaluate(scenarioInstance: ScenarioInstance, flicker: FlickerTest) {
                    flicker.assertLayers { executed1 = true }
                }
            }

        var executed2 = false
        val assertion2 =
            object : AssertionTemplate("Mock Assertion 2") {
                override fun doEvaluate(scenarioInstance: ScenarioInstance, flicker: FlickerTest) {
                    flicker.assertLayers { executed2 = true }
                }
            }

        registry.registerScenario(
            SOME_SCENARIO,
            EXTRACTOR_FOR_SOME_SCENARIO,
            mapOf(assertion1 to AssertionInvocationGroup.BLOCKING)
        )
        registry.registerAssertion(SOME_SCENARIO, assertion2, AssertionInvocationGroup.NON_BLOCKING)

        val extractors = registry.getExtractors()
        Truth.assertThat(extractors).hasSize(1)
        Truth.assertThat(extractors.first().scenarioId).isEqualTo(SOME_SCENARIO)

        val reader = getTraceReaderFromScenario("AppLaunch")
        val extractedScenarioInstance = extractors.first().extract(reader)
        Truth.assertThat(extractedScenarioInstance).hasSize(1)
        val assertions = extractedScenarioInstance.first().generateAssertions()

        Truth.assertThat(assertions).hasSize(2)
        Truth.assertThat(assertions.first().stabilityGroup)
            .isEqualTo(AssertionInvocationGroup.BLOCKING)
        Truth.assertThat(assertions.last().stabilityGroup)
            .isEqualTo(AssertionInvocationGroup.NON_BLOCKING)

        assertions.forEach { it.execute() }
        Truth.assertThat(executed1).isTrue()
        Truth.assertThat(executed2).isTrue()
    }

    @Test
    fun throwsOnRegisteringAssertionToNotRegisteredScenario() {
        val registry = FlickerConfig()

        val assertion =
            object : AssertionTemplate("Mock Assertion") {
                override fun doEvaluate(scenarioInstance: ScenarioInstance, flicker: FlickerTest) {
                    flicker.assertLayers { error("Should never be called") }
                }
            }

        val error = assertThrows<Throwable> { registry.registerAssertion(SOME_SCENARIO, assertion) }
        Truth.assertThat(error)
            .hasMessageThat()
            .contains("No scenario named 'SOME_SCENARIO' registered")
    }

    @Test
    fun throwsOnRegisteringTheSameScenario() {
        val registry = FlickerConfig()

        val assertion =
            object : AssertionTemplate("Mock Assertion") {
                override fun doEvaluate(scenarioInstance: ScenarioInstance, flicker: FlickerTest) {
                    flicker.assertLayers { error("Should never be called") }
                }
            }

        registry.registerScenario(
            SOME_SCENARIO,
            EXTRACTOR_FOR_SOME_SCENARIO,
            mapOf(assertion to AssertionInvocationGroup.BLOCKING)
        )

        val error =
            assertThrows<Throwable> {
                registry.registerScenario(
                    SOME_SCENARIO,
                    EXTRACTOR_FOR_SOME_SCENARIO,
                    mapOf(assertion to AssertionInvocationGroup.BLOCKING)
                )
            }
        Truth.assertThat(error)
            .hasMessageThat()
            .contains("already has a registered scenario with name 'SOME_SCENARIO'")
    }

    @Test
    fun throwsOnRegisteringTheSameAssertion() {
        val registry = FlickerConfig()

        val assertion =
            object : AssertionTemplate("Mock Assertion") {
                override fun doEvaluate(scenarioInstance: ScenarioInstance, flicker: FlickerTest) {
                    flicker.assertLayers { error("Should never be called") }
                }
            }

        registry.registerScenario(
            SOME_SCENARIO,
            EXTRACTOR_FOR_SOME_SCENARIO,
            mapOf(assertion to AssertionInvocationGroup.BLOCKING)
        )

        val error = assertThrows<Throwable> { registry.registerAssertion(SOME_SCENARIO, assertion) }
        Truth.assertThat(error)
            .hasMessageThat()
            .contains("Assertion with id 'Mock Assertion' already present.")
    }

    @Test
    fun canUseConfigs() {
        val registry = FlickerConfig()
        val config = createConfig(SOME_SCENARIO)
        registry.use(config)

        // TODO: Validate
    }

    @Test
    fun canUseMultipleConfigs() {
        val registry = FlickerConfig()

        val config1 = createConfig(ScenarioId("SCENARIO_1"))
        val config2 = createConfig(ScenarioId("SCENARIO_2"))
        val config3 = createConfig(ScenarioId("SCENARIO_3"))

        registry.use(config1, config2).use(config3)

        // TODO: Validate
    }

    @Test
    fun canRegisterSameAssertionForDifferentScenarios() {
        val registry = FlickerConfig()

        val assertion =
            object : AssertionTemplate("Mock Assertion") {
                override fun doEvaluate(scenarioInstance: ScenarioInstance, flicker: FlickerTest) {
                    flicker.assertLayers { error("Should never be called") }
                }
            }

        registry.registerScenario(
            SOME_SCENARIO,
            EXTRACTOR_FOR_SOME_SCENARIO,
            mapOf(assertion to AssertionInvocationGroup.BLOCKING)
        )
        registry.registerScenario(
            SOME_OTHER_SCENARIO,
            EXTRACTOR_FOR_SOME_OTHER_SCENARIO,
            mapOf(assertion to AssertionInvocationGroup.BLOCKING)
        )
    }

    private fun createConfig(scenarioId: ScenarioId): FlickerConfigEntry {
        val scenarioExtractorProvider = { config: ScenarioAssertionsConfig ->
            object : ScenarioExtractor {
                override val scenarioId = config.scenarioId

                override fun extract(reader: Reader): List<ScenarioInstance> {
                    return EntireTraceExtractor(config).extract(reader)
                }
            }
        }

        return object : FlickerConfigEntry {
            override val extractorProvider = scenarioExtractorProvider
            override val assertions = mapOf<AssertionTemplate, AssertionInvocationGroup>()
            override val enabled = true
            override val scenarioId = scenarioId
        }
    }

    companion object {
        private val SOME_SCENARIO = ScenarioId("SOME_SCENARIO")
        private val SOME_OTHER_SCENARIO = ScenarioId("SOME_OTHER_SCENARIO")

        private val EXTRACTOR_FOR_SOME_SCENARIO = { config: ScenarioAssertionsConfig ->
            object : ScenarioExtractor {
                override val scenarioId: ScenarioId = SOME_SCENARIO

                override fun extract(reader: Reader): List<ScenarioInstance> {
                    return EntireTraceExtractor(config).extract(reader)
                }
            }
        }
        private val EXTRACTOR_FOR_SOME_OTHER_SCENARIO = { _: ScenarioAssertionsConfig ->
            object : ScenarioExtractor {
                override val scenarioId: ScenarioId
                    get() = SOME_OTHER_SCENARIO

                override fun extract(reader: Reader): List<ScenarioInstance> {
                    error("Should never be called...")
                }
            }
        }
    }
}
