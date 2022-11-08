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

package com.android.server.wm.flicker

import com.android.server.wm.flicker.assertions.AssertionData
import com.android.server.wm.flicker.assertions.AssertionDataFactory
import com.android.server.wm.flicker.assertions.AssertionRunner
import com.android.server.wm.flicker.assertions.AssertionStateDataFactory
import com.android.server.wm.flicker.assertions.FlickerSubject
import com.android.server.wm.flicker.datastore.CachedResultReader
import com.android.server.wm.flicker.io.ResultReader
import com.android.server.wm.flicker.traces.FlickerTraceSubject
import com.android.server.wm.flicker.traces.eventlog.EventLogSubject
import com.android.server.wm.flicker.traces.layers.LayerTraceEntrySubject
import com.android.server.wm.flicker.traces.layers.LayersTraceSubject
import com.android.server.wm.flicker.traces.region.RegionTraceSubject
import com.android.server.wm.flicker.traces.windowmanager.WindowManagerStateSubject
import com.android.server.wm.flicker.traces.windowmanager.WindowManagerTraceSubject
import com.android.server.wm.traces.common.IComponentMatcher

/** Specification of a flicker test for JUnit ParameterizedRunner class */
data class FlickerTest(
    val config: MutableMap<String, Any?>,
    private val nameOverride: String? = null,
    private val resultReaderProvider: (Scenario) -> ResultReader = {
        CachedResultReader(it, DEFAULT_TRACE_CONFIG)
    },
    private val runnerProvider: (Scenario) -> AssertionRunner = {
        val reader = resultReaderProvider.invoke(it)
        AssertionRunner(reader)
    }
) {
    private val wmAssertionFactory =
        AssertionDataFactory(WindowManagerStateSubject::class, WindowManagerTraceSubject::class)
    private val layersAssertionFactory =
        AssertionDataFactory(LayerTraceEntrySubject::class, LayersTraceSubject::class)
    private val eventLogAssertionFactory = AssertionStateDataFactory(EventLogSubject::class)

    var scenario: Scenario = Scenario.EMPTY
        private set

    override fun toString(): String = scenario.toString()

    fun initialize(_scenario: Scenario) {
        scenario = _scenario
    }

    /**
     * Execute [assertion] on the initial state of a WM trace (before transition)
     *
     * @param assertion Assertion predicate
     */
    fun assertWmStart(assertion: WindowManagerStateSubject.() -> Unit): Throwable? {
        val assertionData =
            wmAssertionFactory.startStateAssertion(assertion as FlickerSubject.() -> Unit)
        return doRunAssertion(assertionData)
    }

    /**
     * Execute [assertion] on the final state of a WM trace (after transition)
     *
     * @param assertion Assertion predicate
     */
    fun assertWmEnd(assertion: WindowManagerStateSubject.() -> Unit): Throwable? {
        val wrappedAssertion: (WindowManagerStateSubject) -> Unit = { assertion(it) }
        val assertionData =
            wmAssertionFactory.endStateAssertion(wrappedAssertion as (FlickerSubject) -> Unit)
        return doRunAssertion(assertionData)
    }

    /**
     * Execute [assertion] on a WM trace
     *
     * @param assertion Assertion predicate
     */
    fun assertWm(assertion: WindowManagerTraceSubject.() -> Unit): Throwable? {
        val assertionData =
            wmAssertionFactory.traceAssertion(
                assertion as (FlickerTraceSubject<FlickerSubject>) -> Unit
            )
        return doRunAssertion(assertionData)
    }

    /**
     * Execute [assertion] on a user defined moment ([tag]) of a WM trace
     *
     * @param assertion Assertion predicate
     */
    fun assertWmTag(tag: String, assertion: WindowManagerStateSubject.() -> Unit): Throwable? {
        val assertionData =
            wmAssertionFactory.tagAssertion(tag, assertion as FlickerSubject.() -> Unit)
        return doRunAssertion(assertionData)
    }

    /**
     * Execute [assertion] on the visible region of WM state matching [componentMatcher]
     *
     * @param componentMatcher Components to search
     * @param assertion Assertion predicate
     */
    fun assertWmVisibleRegion(
        componentMatcher: IComponentMatcher,
        assertion: RegionTraceSubject.() -> Unit
    ): Throwable? {
        val assertionData = buildWmVisibleRegionAssertion(componentMatcher, assertion)
        return doRunAssertion(assertionData)
    }

    /**
     * Execute [assertion] on the initial state of a SF trace (before transition)
     *
     * @param assertion Assertion predicate
     */
    fun assertLayersStart(assertion: LayerTraceEntrySubject.() -> Unit): Throwable? {
        val assertionData =
            layersAssertionFactory.startStateAssertion(assertion as FlickerSubject.() -> Unit)
        return doRunAssertion(assertionData)
    }

    /**
     * Execute [assertion] on the final state of a SF trace (after transition)
     *
     * @param assertion Assertion predicate
     */
    fun assertLayersEnd(assertion: LayerTraceEntrySubject.() -> Unit): Throwable? {
        val assertionData =
            layersAssertionFactory.endStateAssertion(assertion as FlickerSubject.() -> Unit)
        return doRunAssertion(assertionData)
    }

    /**
     * Execute [assertion] on a SF trace
     *
     * @param assertion Assertion predicate
     */
    fun assertLayers(assertion: LayersTraceSubject.() -> Unit): Throwable? {
        val assertionData =
            layersAssertionFactory.traceAssertion(
                assertion as (FlickerTraceSubject<FlickerSubject>) -> Unit
            )
        return doRunAssertion(assertionData)
    }

    /**
     * Execute [assertion] on a user defined moment ([tag]) of a SF trace
     *
     * @param assertion Assertion predicate
     */
    fun assertLayersTag(tag: String, assertion: LayerTraceEntrySubject.() -> Unit): Throwable? {
        val assertionData =
            layersAssertionFactory.tagAssertion(tag, assertion as FlickerSubject.() -> Unit)
        return doRunAssertion(assertionData)
    }

    /**
     * Execute [assertion] on the visible region of a component on the layers trace matching
     * [componentMatcher]
     *
     * @param componentMatcher Components to search
     * @param useCompositionEngineRegionOnly If true, uses only the region calculated from the
     * Composition Engine (CE) -- visibleRegion in the proto definition. Otherwise, calculates the
     * visible region when the information is not available from the CE
     * @param assertion Assertion predicate
     */
    @JvmOverloads
    fun assertLayersVisibleRegion(
        componentMatcher: IComponentMatcher,
        useCompositionEngineRegionOnly: Boolean = true,
        assertion: RegionTraceSubject.() -> Unit
    ): Throwable? {
        val assertionData =
            buildLayersVisibleRegionAssertion(
                componentMatcher,
                useCompositionEngineRegionOnly,
                assertion
            )
        return doRunAssertion(assertionData)
    }

    /**
     * Execute [assertion] on a sequence of event logs
     *
     * @param assertion Assertion predicate
     */
    fun assertEventLog(assertion: EventLogSubject.() -> Unit): Throwable? {
        val assertionData =
            eventLogAssertionFactory.tagAssertion(
                AssertionTag.ALL,
                assertion as FlickerSubject.() -> Unit
            )
        return doRunAssertion(assertionData)
    }

    private fun doRunAssertion(assertion: AssertionData): Throwable? {
        require(!scenario.isEmpty) { "Scenario shouldn't be empty" }
        return runnerProvider.invoke(scenario).runAssertion(assertion)
    }

    private fun buildWmVisibleRegionAssertion(
        componentMatcher: IComponentMatcher,
        assertion: RegionTraceSubject.() -> Unit
    ): AssertionData {
        val closedAssertion: (WindowManagerTraceSubject) -> Unit = {
            require(it.isAssertionsEmpty()) { "Subject was already used to execute assertions" }
            // convert WindowManagerTraceSubject to RegionTraceSubject
            val regionTraceSubject = it.visibleRegion(componentMatcher)
            // add assertions to the regionTraceSubject's AssertionChecker
            assertion(regionTraceSubject)
            // loop through all entries to validate assertions
            regionTraceSubject.forAllEntries()
        }

        return wmAssertionFactory.traceAssertion(
            closedAssertion as (FlickerTraceSubject<FlickerSubject>) -> Unit
        )
    }

    private fun buildLayersVisibleRegionAssertion(
        componentMatcher: IComponentMatcher,
        useCompositionEngineRegionOnly: Boolean = true,
        assertion: RegionTraceSubject.() -> Unit
    ): AssertionData {
        val closedAssertion: LayersTraceSubject.() -> Unit = {
            this.clear()
            // convert LayersTraceSubject to RegionTraceSubject
            val regionTraceSubject = visibleRegion(componentMatcher, useCompositionEngineRegionOnly)

            // add assertions to the regionTraceSubject's AssertionChecker
            assertion(regionTraceSubject)
            // loop through all entries to validate assertions
            regionTraceSubject.forAllEntries()
        }

        return layersAssertionFactory.traceAssertion(
            closedAssertion as (FlickerTraceSubject<*>) -> Unit
        )
    }
}
