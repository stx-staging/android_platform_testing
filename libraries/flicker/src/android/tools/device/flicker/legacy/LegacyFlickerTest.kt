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

package android.tools.device.flicker.legacy

import android.tools.common.CrossPlatform
import android.tools.common.Scenario
import android.tools.common.ScenarioBuilder
import android.tools.common.flicker.assertions.AssertionData
import android.tools.common.flicker.assertions.AssertionFactory
import android.tools.common.flicker.assertions.SubjectsParser
import android.tools.common.flicker.subject.events.EventLogSubject
import android.tools.common.flicker.subject.layers.LayerTraceEntrySubject
import android.tools.common.flicker.subject.layers.LayersTraceSubject
import android.tools.common.flicker.subject.region.RegionTraceSubject
import android.tools.common.flicker.subject.wm.WindowManagerStateSubject
import android.tools.common.flicker.subject.wm.WindowManagerTraceSubject
import android.tools.common.io.IReader
import android.tools.common.traces.component.IComponentMatcher
import android.tools.device.flicker.assertions.BaseAssertionRunner
import android.tools.device.flicker.datastore.CachedAssertionRunner
import android.tools.device.flicker.datastore.CachedResultReader
import android.tools.device.traces.TRACE_CONFIG_REQUIRE_CHANGES

/** Specification of a flicker test for JUnit ParameterizedRunner class */
data class LegacyFlickerTest(
    private val scenarioBuilder: ScenarioBuilder = ScenarioBuilder(),
    private val resultReaderProvider: (Scenario) -> CachedResultReader = {
        CachedResultReader(it, TRACE_CONFIG_REQUIRE_CHANGES)
    },
    private val subjectsParserProvider: (IReader) -> SubjectsParser = { SubjectsParser(it) },
    private val runnerProvider: (Scenario) -> BaseAssertionRunner = {
        val reader = resultReaderProvider(it)
        CachedAssertionRunner(it, reader)
    }
) : FlickerTest {
    private val assertionFactory = AssertionFactory()

    var scenario: Scenario = ScenarioBuilder().createEmptyScenario()
        private set

    override fun toString(): String = scenario.toString()

    fun initialize(testClass: String): Scenario {
        scenario = scenarioBuilder.forClass(testClass).build()
        return scenario
    }

    /** Obtains a reader for the flicker result artifact */
    val reader: IReader
        get() = resultReaderProvider(scenario)

    /**
     * Execute [assertion] on the initial state of a WM trace (before transition)
     *
     * @param assertion Assertion predicate
     */
    override fun assertWmStart(assertion: WindowManagerStateSubject.() -> Unit) {
        CrossPlatform.log.withTracing("assertWmStart") {
            val assertionData = assertionFactory.createWmStartAssertion(assertion)
            doRunAssertion(assertionData)
        }
    }

    /**
     * Execute [assertion] on the final state of a WM trace (after transition)
     *
     * @param assertion Assertion predicate
     */
    override fun assertWmEnd(assertion: WindowManagerStateSubject.() -> Unit) {
        CrossPlatform.log.withTracing("assertWmEnd") {
            val assertionData = assertionFactory.createWmEndAssertion(assertion)
            doRunAssertion(assertionData)
        }
    }

    /**
     * Execute [assertion] on a WM trace
     *
     * @param assertion Assertion predicate
     */
    override fun assertWm(assertion: WindowManagerTraceSubject.() -> Unit) {
        CrossPlatform.log.withTracing("assertWm") {
            val assertionData = assertionFactory.createWmAssertion(assertion)
            doRunAssertion(assertionData)
        }
    }

    /**
     * Execute [assertion] on a user defined moment ([tag]) of a WM trace
     *
     * @param assertion Assertion predicate
     */
    override fun assertWmTag(tag: String, assertion: WindowManagerStateSubject.() -> Unit) {
        CrossPlatform.log.withTracing("assertWmTag") {
            val assertionData = assertionFactory.createWmTagAssertion(tag, assertion)
            doRunAssertion(assertionData)
        }
    }

    /**
     * Execute [assertion] on the visible region of WM state matching [componentMatcher]
     *
     * @param componentMatcher Components to search
     * @param assertion Assertion predicate
     */
    override fun assertWmVisibleRegion(
        componentMatcher: IComponentMatcher,
        assertion: RegionTraceSubject.() -> Unit
    ) {
        CrossPlatform.log.withTracing("assertWmVisibleRegion") {
            val assertionData =
                assertionFactory.createWmVisibleRegionAssertion(componentMatcher, assertion)
            doRunAssertion(assertionData)
        }
    }

    /**
     * Execute [assertion] on the initial state of a SF trace (before transition)
     *
     * @param assertion Assertion predicate
     */
    override fun assertLayersStart(assertion: LayerTraceEntrySubject.() -> Unit) {
        CrossPlatform.log.withTracing("assertLayersStart") {
            val assertionData = assertionFactory.createLayersStartAssertion(assertion)
            doRunAssertion(assertionData)
        }
    }

    /**
     * Execute [assertion] on the final state of a SF trace (after transition)
     *
     * @param assertion Assertion predicate
     */
    override fun assertLayersEnd(assertion: LayerTraceEntrySubject.() -> Unit) {
        CrossPlatform.log.withTracing("assertLayersEnd") {
            val assertionData = assertionFactory.createLayersEndAssertion(assertion)
            doRunAssertion(assertionData)
        }
    }

    override fun assertLayers(assertion: LayersTraceSubject.() -> Unit) {
        CrossPlatform.log.withTracing("assertLayers") {
            val assertionData = assertionFactory.createLayersAssertion(assertion)
            doRunAssertion(assertionData)
        }
    }

    override fun assertLayersTag(tag: String, assertion: LayerTraceEntrySubject.() -> Unit) {
        CrossPlatform.log.withTracing("assertLayersTag") {
            val assertionData = assertionFactory.createLayersTagAssertion(tag, assertion)
            doRunAssertion(assertionData)
        }
    }

    override fun assertLayersVisibleRegion(
        componentMatcher: IComponentMatcher,
        useCompositionEngineRegionOnly: Boolean,
        assertion: RegionTraceSubject.() -> Unit
    ) {
        CrossPlatform.log.withTracing("assertLayersVisibleRegion") {
            val assertionData =
                assertionFactory.createLayersVisibleRegionAssertion(
                    componentMatcher,
                    useCompositionEngineRegionOnly,
                    assertion
                )
            doRunAssertion(assertionData)
        }
    }

    override fun assertEventLog(assertion: EventLogSubject.() -> Unit) {
        CrossPlatform.log.withTracing("assertEventLog") {
            val assertionData = assertionFactory.createEventLogAssertion(assertion)
            doRunAssertion(assertionData)
        }
    }

    private fun doRunAssertion(assertion: AssertionData) {
        require(!scenario.isEmpty) { "Scenario shouldn't be empty" }
        runnerProvider.invoke(scenario).runAssertion(assertion)?.let { throw it }
    }
}
