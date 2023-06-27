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

import android.tools.common.flicker.AssertionInvocationGroup
import android.tools.common.flicker.assertors.AssertionId
import android.tools.common.flicker.assertors.AssertionTemplate
import android.tools.common.flicker.extractors.ScenarioExtractor

typealias ScenarioExtractorProvider = (ScenarioAssertionsConfig) -> ScenarioExtractor

internal class FlickerConfigImpl : FlickerConfig {
    private val registry = mutableMapOf<ScenarioId, RegistryEntry>()
    private data class RegistryEntry(
        val extractorProviders: MutableSet<ScenarioExtractorProvider> = mutableSetOf(),
        val assertions: MutableSet<AssertionEntry> = mutableSetOf()
    )

    override fun use(flickerConfigEntries: Collection<FlickerConfigEntry>): FlickerConfig = apply {
        for (config in flickerConfigEntries) {
            if (!config.enabled) {
                continue
            }
            registerScenario(config.scenarioId, config.extractorProvider, config.assertions)
        }
    }

    override fun registerScenario(
        scenario: ScenarioId,
        extractorProvider: ScenarioExtractorProvider,
        assertions: Map<AssertionTemplate, AssertionInvocationGroup>?
    ) {
        require(!registry.containsKey(scenario)) {
            "${this::class.simpleName} already has a registered scenario with name '$scenario'."
        }

        registry.putIfAbsent(scenario, RegistryEntry())
        registerExtractor(scenario, extractorProvider)
        assertions?.forEach { registerAssertion(scenario, it.key, it.value) }
    }

    override fun registerExtractor(
        scenario: ScenarioId,
        extractorProvider: ScenarioExtractorProvider
    ) {
        val entry = registry[scenario]
        require(entry != null) { "No scenario named '$scenario' registered." }

        entry.extractorProviders.add(extractorProvider)
    }

    override fun unregisterScenario(scenario: ScenarioId) {
        val entry = registry[scenario]
        require(entry != null) { "No scenario named '$scenario' registered." }
        registry.remove(scenario)
    }

    override fun registerAssertion(
        scenario: ScenarioId,
        assertion: AssertionTemplate,
        stabilityGroup: AssertionInvocationGroup
    ) {
        val entry = registry[scenario]
        require(entry != null) { "No scenario named '$scenario' registered." }

        require(entry.assertions.none { it.template.id == assertion.id }) {
            "Assertion with id '${assertion.id.name}' already present for scenario " +
                "'${scenario.name}'."
        }

        entry.assertions.add(AssertionEntry(assertion, stabilityGroup))
    }

    override fun overrideAssertionStabilityGroup(
        scenario: ScenarioId,
        assertionId: AssertionId,
        stabilityGroup: AssertionInvocationGroup
    ) {
        val entry = registry[scenario]
        require(entry != null) { "No scenario named '$scenario' registered." }

        val targetAssertion = entry.assertions.firstOrNull { it.template.id == assertionId }
        require(targetAssertion != null) {
            "No assertion with id $assertionId present in registry for scenario $scenario."
        }

        targetAssertion.stabilityGroup = stabilityGroup
    }

    override fun unregisterAssertion(scenario: ScenarioId, assertionId: AssertionId) {
        val entry = registry[scenario]
        require(entry != null) { "No scenario named '$scenario' registered." }

        val targetAssertion = entry.assertions.firstOrNull { it.template.id == assertionId }
        require(targetAssertion != null) {
            "No assertion with id $assertionId present in registry for scenario $scenario."
        }

        entry.assertions.remove(targetAssertion)
    }

    override fun getExtractors(): Collection<ScenarioExtractor> {
        return registry.entries.flatMap { entry ->
            entry.value.extractorProviders.map {
                it(ScenarioAssertionsConfig(entry.key, entry.value.assertions))
            }
        }
    }
}
