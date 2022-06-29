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

package com.android.server.wm.flicker.traces.windowmanager

import com.android.server.wm.flicker.assertions.Assertion
import com.android.server.wm.flicker.traces.FlickerFailureStrategy
import com.android.server.wm.flicker.traces.FlickerTraceSubject
import com.android.server.wm.flicker.traces.region.RegionTraceSubject
import com.android.server.wm.traces.common.ComponentMatcher
import com.android.server.wm.traces.common.IComponentMatcher
import com.android.server.wm.traces.common.region.RegionTrace
import com.android.server.wm.traces.common.windowmanager.WindowManagerTrace
import com.android.server.wm.traces.common.windowmanager.windows.WindowState
import com.google.common.truth.FailureMetadata
import com.google.common.truth.FailureStrategy
import com.google.common.truth.StandardSubjectBuilder
import com.google.common.truth.Subject
import com.google.common.truth.Subject.Factory

/**
 * Truth subject for [WindowManagerTrace] objects, used to make assertions over behaviors that
 * occur throughout a whole trace.
 *
 * To make assertions over a trace it is recommended to create a subject using
 * [WindowManagerTraceSubject.assertThat](myTrace). Alternatively, it is also possible to use
 * Truth.assertAbout(WindowManagerTraceSubject.FACTORY), however it will provide less debug
 * information because it uses Truth's default [FailureStrategy].
 *
 * Example:
 *    val trace = WindowManagerTraceParser.parseFromTrace(myTraceFile)
 *    val subject = WindowManagerTraceSubject.assertThat(trace)
 *        .contains("ValidWindow")
 *        .notContains("ImaginaryWindow")
 *        .showsAboveAppWindow("NavigationBar")
 *        .forAllEntries()
 *
 * Example2:
 *    val trace = WindowManagerTraceParser.parseFromTrace(myTraceFile)
 *    val subject = WindowManagerTraceSubject.assertThat(trace) {
 *        check("Custom check") { myCustomAssertion(this) }
 *    }
 */
class WindowManagerTraceSubject private constructor(
    fm: FailureMetadata,
    val trace: WindowManagerTrace,
    override val parent: WindowManagerTraceSubject?
) : FlickerTraceSubject<WindowManagerStateSubject>(fm, trace) {
    override val selfFacts
        get() = super.selfFacts.toMutableList()

    override val subjects by lazy {
        trace.entries.map { WindowManagerStateSubject.assertThat(it, this, this) }
    }

    /** {@inheritDoc} */
    override fun then(): WindowManagerTraceSubject = apply { super.then() }

    /** {@inheritDoc} */
    override fun skipUntilFirstAssertion(): WindowManagerTraceSubject =
        apply { super.skipUntilFirstAssertion() }

    fun isEmpty(): WindowManagerTraceSubject = apply {
        check("Trace is empty").that(trace).isEmpty()
    }

    fun isNotEmpty(): WindowManagerTraceSubject = apply {
        check("Trace is not empty").that(trace).isNotEmpty()
    }

    /**
     * @return List of [WindowStateSubject]s matching [componentMatcher] in the order they
     *      appear on the trace
     *
     * @param componentMatcher Components to search
     */
    fun windowStates(componentMatcher: IComponentMatcher): List<WindowStateSubject> =
        subjects
            .map { it.windowState { windows -> componentMatcher.windowMatchesAnyOf(windows) } }
            .filter { it.isNotEmpty }

    /**
     * @return List of [WindowStateSubject]s matching [predicate] in the order they
     *      appear on the trace
     *
     * @param predicate To search
     */
    fun windowStates(predicate: (WindowState) -> Boolean): List<WindowStateSubject> {
        return subjects
            .map { it.windowState { window -> predicate(window) } }
            .filter { it.isNotEmpty }
    }

    /**
     * Checks that no [WindowState] in the state matches [componentMatcher]
     *
     * @param componentMatcher Components to search
     * @param isOptional If this assertion is optional or must pass
     */
    @JvmOverloads
    fun notContains(
        componentMatcher: IComponentMatcher,
        isOptional: Boolean = false
    ): WindowManagerTraceSubject = apply {
        addAssertion("notContains(${componentMatcher.toWindowName()})", isOptional) {
            it.notContains(componentMatcher)
        }
    }

    /**
     * Checks if the non-app window matching [componentMatcher] exists above the app
     * windows and is visible
     *
     * @param componentMatcher Components to search
     * @param isOptional If this assertion is optional or must pass
     */
    @JvmOverloads
    fun isAboveAppWindowVisible(
        componentMatcher: IComponentMatcher,
        isOptional: Boolean = false
    ): WindowManagerTraceSubject = apply {
        addAssertion("isAboveAppWindowVisible(${componentMatcher.toWindowName()})", isOptional) {
            it.containsAboveAppWindow(componentMatcher)
                .isNonAppWindowVisible(componentMatcher)
        }
    }

    /**
     * Checks if the non-app window matching [componentMatcher] exists above the app
     * windows and is invisible
     *
     * @param componentMatcher Components to search
     * @param isOptional If this assertion is optional or must pass
     */
    @JvmOverloads
    fun isAboveAppWindowInvisible(
        componentMatcher: IComponentMatcher,
        isOptional: Boolean = false
    ): WindowManagerTraceSubject = apply {
        addAssertion("isAboveAppWindowInvisible(${componentMatcher.toWindowName()})", isOptional) {
            it.containsAboveAppWindow(componentMatcher)
                .isNonAppWindowInvisible(componentMatcher)
        }
    }

    /**
     * Checks if the non-app window matching [componentMatcher] exists below the app
     * windows and is visible
     *
     * @param componentMatcher Components to search
     * @param isOptional If this assertion is optional or must pass
     */
    @JvmOverloads
    fun isBelowAppWindowVisible(
        componentMatcher: IComponentMatcher,
        isOptional: Boolean = false
    ): WindowManagerTraceSubject = apply {
        addAssertion("isBelowAppWindowVisible(${componentMatcher.toWindowName()})", isOptional) {
            it.containsBelowAppWindow(componentMatcher)
                .isNonAppWindowVisible(componentMatcher)
        }
    }

    /**
     * Checks if the non-app window matching [componentMatcher] exists below the app
     * windows and is invisible
     *
     * @param componentMatcher Components to search
     * @param isOptional If this assertion is optional or must pass
     */
    @JvmOverloads
    fun isBelowAppWindowInvisible(
        componentMatcher: IComponentMatcher,
        isOptional: Boolean = false
    ): WindowManagerTraceSubject = apply {
        addAssertion("isBelowAppWindowInvisible(${componentMatcher.toWindowName()})", isOptional) {
            it.containsBelowAppWindow(componentMatcher)
                .isNonAppWindowInvisible(componentMatcher)
        }
    }

    /**
     * Checks if non-app window matching [componentMatcher] exists above or
     * below the app windows and is visible
     *
     * @param componentMatcher Components to search
     * @param isOptional If this assertion is optional or must pass
     */
    @JvmOverloads
    fun isNonAppWindowVisible(
        componentMatcher: IComponentMatcher,
        isOptional: Boolean = false
    ): WindowManagerTraceSubject = apply {
        addAssertion("isNonAppWindowVisible(${componentMatcher.toWindowName()})", isOptional) {
            it.isNonAppWindowVisible(componentMatcher)
        }
    }

    /**
     * Checks if non-app window matching [componentMatcher] exists above or
     * below the app windows and is invisible
     *
     * @param componentMatcher Components to search
     * @param isOptional If this assertion is optional or must pass
     */
    @JvmOverloads
    fun isNonAppWindowInvisible(
        componentMatcher: IComponentMatcher,
        isOptional: Boolean = false
    ): WindowManagerTraceSubject = apply {
        addAssertion("isNonAppWindowInvisible(${componentMatcher.toWindowName()})", isOptional) {
            it.isNonAppWindowInvisible(componentMatcher)
        }
    }

    /**
     * Checks if app window matching [componentMatcher] is on top
     *
     * @param componentMatcher Components to search
     * @param isOptional If this assertion is optional or must pass
     */
    @JvmOverloads
    fun isAppWindowOnTop(
        componentMatcher: IComponentMatcher,
        isOptional: Boolean = false
    ): WindowManagerTraceSubject = apply {
        addAssertion("isAppWindowOnTop(${componentMatcher.toWindowName()})", isOptional) {
            it.isAppWindowOnTop(componentMatcher)
        }
    }

    /**
     * Checks if app window matching [componentMatcher] is not on top
     *
     * @param componentMatcher Components to search
     * @param isOptional If this assertion is optional or must pass
     */
    @JvmOverloads
    fun isAppWindowNotOnTop(
        componentMatcher: IComponentMatcher,
        isOptional: Boolean = false
    ): WindowManagerTraceSubject = apply {
        addAssertion("appWindowNotOnTop(${componentMatcher.toWindowName()})", isOptional) {
            it.isAppWindowNotOnTop(componentMatcher)
        }
    }

    /**
     * Checks if app window matching [componentMatcher] is visible
     *
     * @param componentMatcher Components to search
     * @param isOptional If this assertion is optional or must pass
     */
    @JvmOverloads
    fun isAppWindowVisible(
        componentMatcher: IComponentMatcher,
        isOptional: Boolean = false
    ): WindowManagerTraceSubject = apply {
        addAssertion("isAppWindowVisible(${componentMatcher.toWindowName()})", isOptional) {
            it.isAppWindowVisible(componentMatcher)
        }
    }

    /**
     * Checks if there are no visible app windows.
     *
     * @param isOptional If this assertion is optional or must pass
     */
    @JvmOverloads
    fun hasNoVisibleAppWindow(isOptional: Boolean = false): WindowManagerTraceSubject = apply {
        addAssertion("hasNoVisibleAppWindow()", isOptional) {
            it.hasNoVisibleAppWindow()
        }
    }

    /**
     * Checks if the activity matching [componentMatcher] is visible
     *
     * In the case that an app is stopped in the background (e.g. OS stopped it to release memory)
     * the app window will not be immediately visible when switching back to the app. Checking if a
     * snapshotStartingWindow is present for that app instead can decrease flakiness levels of the
     * assertion.
     *
     * @param componentMatcher Components to search
     * @param isOptional If this assertion is optional or must pass
     */
    @JvmOverloads
    fun isAppSnapshotStartingWindowVisibleFor(
        componentMatcher: IComponentMatcher,
        isOptional: Boolean = false
    ): WindowManagerTraceSubject = apply {
        addAssertion(
            "isAppSnapshotStartingWindowVisibleFor(${componentMatcher.toWindowName()})", isOptional
        ) {
            it.isAppSnapshotStartingWindowVisibleFor(componentMatcher)
        }
    }

    /**
     * Checks if app window matching [componentMatcher] is invisible
     *
     * Note: This assertion has issues with the launcher window, because it contains 2 windows
     * with the same name and only 1 is visible at a time. Prefer [isAppWindowOnTop] for launcher
     * instead
     *
     * @param componentMatcher Components to search
     * @param isOptional If this assertion is optional or must pass
     */
    @JvmOverloads
    fun isAppWindowInvisible(
        componentMatcher: IComponentMatcher,
        isOptional: Boolean = false
    ): WindowManagerTraceSubject = apply {
        addAssertion("isAppWindowInvisible(${componentMatcher.toWindowName()})", isOptional) {
            it.isAppWindowInvisible(componentMatcher)
        }
    }

    /**
     * Checks if no app windows matching [componentMatcher] overlap with each other.
     *
     * @param componentMatcher Components to search
     */
    fun noWindowsOverlap(
        vararg componentMatcher: IComponentMatcher
    ): WindowManagerTraceSubject = apply {
        val repr = componentMatcher.joinToString(", ") { it.toWindowName() }
        verify("Must give more than one window to check! (Given $repr)")
            .that(componentMatcher)
            .hasLength(1)
        addAssertion("noWindowsOverlap($repr)") {
            it.doNotOverlap(*componentMatcher)
        }
    }

    /**
     * Checks if the window matching [aboveWindow] is above the one matching [belowWindow] in
     * z-order.
     *
     * @param aboveWindow Expected top window
     * @param belowWindow Expected bottom window
     */
    fun isAboveWindow(
        aboveWindow: IComponentMatcher,
        belowWindow: IComponentMatcher
    ): WindowManagerTraceSubject = apply {
        val aboveWindowTitle = aboveWindow.toWindowName()
        val belowWindowTitle = belowWindow.toWindowName()
        require(aboveWindowTitle != belowWindowTitle)
        addAssertion("$aboveWindowTitle is above $belowWindowTitle") {
            it.isAboveWindow(aboveWindow, belowWindow)
        }
    }

    /**
     * Obtains the trace of regions occupied by all windows matching [componentMatcher]
     *
     * @param componentMatcher Components to search
     */
    fun visibleRegion(componentMatcher: IComponentMatcher): RegionTraceSubject {
        val regionTrace = RegionTrace(componentMatcher, subjects.map {
            it.visibleRegion(componentMatcher).regionEntry
        }.toTypedArray())

        return RegionTraceSubject.assertThat(regionTrace, this)
    }

    /**
     * Checks that all visible layers are shown for more than one consecutive entry
     */
    @JvmOverloads
    fun visibleWindowsShownMoreThanOneConsecutiveEntry(
        ignoreWindows: List<ComponentMatcher> = listOf(
            ComponentMatcher.SPLASH_SCREEN,
            ComponentMatcher.SNAPSHOT
        )
    ): WindowManagerTraceSubject = apply {
        visibleEntriesShownMoreThanOneConsecutiveTime { subject ->
            subject.wmState.windowStates
                .filter { it.isVisible }
                .filter {
                    ignoreWindows.none { windowName -> windowName.toWindowName() in it.title }
                }
                .map { it.name }
                .toSet()
        }
    }

    /**
     * Executes a custom [assertion] on the current subject
     */
    operator fun invoke(
        name: String,
        isOptional: Boolean = false,
        assertion: Assertion<WindowManagerStateSubject>
    ): WindowManagerTraceSubject = apply { addAssertion(name, isOptional, assertion) }

    /**
     * Run the assertions for all trace entries within the specified time range
     */
    fun forRange(startTime: Long, endTime: Long) {
        val subjectsInRange = subjects.filter { it.wmState.timestamp in startTime..endTime }
        assertionsChecker.test(subjectsInRange)
    }

    /**
     * User-defined entry point for the trace entry with [timestamp]
     *
     * @param timestamp of the entry
     */
    fun entry(timestamp: Long): WindowManagerStateSubject =
        subjects.first { it.wmState.timestamp == timestamp }

    companion object {
        /**
         * Boilerplate Subject.Factory for WmTraceSubject
         */
        private fun getFactory(
            parent: WindowManagerTraceSubject?
        ): Factory<Subject, WindowManagerTrace> =
            Factory { fm, subject -> WindowManagerTraceSubject(fm, subject, parent) }

        /**
         * Creates a [WindowManagerTraceSubject] representing a WindowManager trace,
         * which can be used to make assertions.
         *
         * @param trace WindowManager trace
         */
        @JvmStatic
        @JvmOverloads
        fun assertThat(
            trace: WindowManagerTrace,
            parent: WindowManagerTraceSubject? = null
        ): WindowManagerTraceSubject {
            val strategy = FlickerFailureStrategy()
            val subject = StandardSubjectBuilder.forCustomFailureStrategy(strategy)
                .about(getFactory(parent))
                .that(trace) as WindowManagerTraceSubject
            strategy.init(subject)
            return subject
        }

        /**
         * Static method for getting the subject factory (for use with assertAbout())
         */
        @JvmStatic
        fun entries(
            parent: WindowManagerTraceSubject?
        ): Factory<Subject, WindowManagerTrace> = getFactory(parent)
    }
}
