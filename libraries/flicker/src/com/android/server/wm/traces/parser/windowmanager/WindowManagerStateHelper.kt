/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.server.wm.traces.parser.windowmanager

import android.app.ActivityTaskManager
import android.app.Instrumentation
import android.app.WindowConfiguration
import android.graphics.Rect
import android.os.SystemClock
import android.util.Log
import android.view.Display
import androidx.test.platform.app.InstrumentationRegistry
import com.android.server.wm.traces.common.Condition
import com.android.server.wm.traces.common.DUMP
import com.android.server.wm.traces.common.DeviceStateDump
import com.android.server.wm.traces.common.FlickerComponentName
import com.android.server.wm.traces.common.FlickerComponentName.Companion.IME
import com.android.server.wm.traces.common.FlickerComponentName.Companion.LAUNCHER
import com.android.server.wm.traces.common.FlickerComponentName.Companion.SNAPSHOT
import com.android.server.wm.traces.common.FlickerComponentName.Companion.SPLASH_SCREEN
import com.android.server.wm.traces.common.WaitCondition
import com.android.server.wm.traces.common.WindowManagerConditionsFactory
import com.android.server.wm.traces.common.layers.BaseLayerTraceEntry
import com.android.server.wm.traces.common.region.Region
import com.android.server.wm.traces.common.windowmanager.WindowManagerState
import com.android.server.wm.traces.common.windowmanager.windows.ConfigurationContainer
import com.android.server.wm.traces.common.windowmanager.windows.WindowState
import com.android.server.wm.traces.parser.LOG_TAG
import com.android.server.wm.traces.parser.getCurrentStateDump

open class WindowManagerStateHelper @JvmOverloads constructor(
    /**
     * Instrumentation to run the tests
     */
    private val instrumentation: Instrumentation = InstrumentationRegistry.getInstrumentation(),
    /**
     * Predicate to supply a new UI information
     */
    private val deviceDumpSupplier:
        () -> DeviceStateDump<WindowManagerState, BaseLayerTraceEntry> =
            {
            val currState = getCurrentStateDump(instrumentation.uiAutomation)
            DeviceStateDump(
                currState.wmState ?: error("Unable to parse WM trace"),
                currState.layerState ?: error("Unable to parse Layers trace")
            )
        },
    /**
     * Number of attempts to satisfy a wait condition
     */
    private val numRetries: Int = WaitCondition.DEFAULT_RETRY_LIMIT,
    /**
     * Interval between wait for state dumps during wait conditions
     */
    private val retryIntervalMs: Long = WaitCondition.DEFAULT_RETRY_INTERVAL_MS
) {
    private var internalState: DeviceStateDump<WindowManagerState, BaseLayerTraceEntry>? = null

    /**
     * Queries the supplier for a new device state
     */
    val currentState: DeviceStateDump<WindowManagerState, BaseLayerTraceEntry>
        get() {
            if (internalState == null) {
                internalState = deviceDumpSupplier.invoke()
            } else {
                StateSyncBuilder().withValidState().waitFor()
            }
            return internalState ?: error("Unable to fetch an internal state")
        }

    protected open fun updateCurrState(
        value: DeviceStateDump<WindowManagerState, BaseLayerTraceEntry>
    ) {
        internalState = value
    }

    /**
     * Obtains a [WindowContainer] from the current device state, or null if the WindowContainer
     * doesn't exist
     */
    fun getWindow(activity: FlickerComponentName): WindowState? {
        val windowName = activity.toWindowName()
        return this.currentState.wmState.windowStates
            .firstOrNull { it.title == windowName }
    }

    /**
     * Obtains the region of a window in the state, or an empty [Rect] is there are none
     */
    fun getWindowRegion(activity: FlickerComponentName): Region {
        val window = getWindow(activity)
        return window?.frameRegion ?: Region.EMPTY
    }

    inner class StateSyncBuilder {
        private val conditionBuilder = createConditionBuilder()
        private var lastMessage = ""

        private fun createConditionBuilder():
            WaitCondition.Builder<DeviceStateDump<WindowManagerState, BaseLayerTraceEntry>> =
            WaitCondition.Builder(deviceDumpSupplier, numRetries)
                .onSuccess { updateCurrState(it) }
                .onFailure { updateCurrState(it) }
                .onLog { msg, isError ->
                    lastMessage = msg
                    if (isError) {
                        Log.e(LOG_TAG, msg)
                    } else {
                        Log.d(LOG_TAG, msg)
                    }
                }
                .onRetry { SystemClock.sleep(retryIntervalMs) }

        fun add(condition: Condition<DUMP>): StateSyncBuilder = apply {
            conditionBuilder.withCondition(condition)
        }

        @JvmOverloads
        fun add(message: String = "", condition: (DUMP) -> Boolean): StateSyncBuilder =
            add(Condition(message, condition))

        fun waitFor(): Boolean {
            val passed = conditionBuilder.build().waitFor()
            // Ensure WindowManagerService wait until all animations have completed
            instrumentation.waitForIdleSync()
            instrumentation.uiAutomation.syncInputTransactions()
            return passed
        }

        fun waitForAndVerify() {
            val success = waitFor()
            require(success) { lastMessage }
        }

        fun withFullScreenApp(component: FlickerComponentName) =
            isAppFullScreen(component)
                .withSnapshotGone()
                .withSplashScreenGone()
                .add(WindowManagerConditionsFactory.isLayerVisible(component))
                .add(WindowManagerConditionsFactory.hasLayersAnimating().negate())
                .add(WindowManagerConditionsFactory.isAppTransitionIdle(Display.DEFAULT_DISPLAY))

        fun withHomeActivityVisible() =
            add(WindowManagerConditionsFactory.isHomeActivityVisible())
                .add(WindowManagerConditionsFactory.isLayerVisible(LAUNCHER))
                .add(WindowManagerConditionsFactory.isAppTransitionIdle(Display.DEFAULT_DISPLAY))
                .add(WindowManagerConditionsFactory.hasLayersAnimating().negate())
                .add(WindowManagerConditionsFactory.isNavBarVisible())
                .add(WindowManagerConditionsFactory.isStatusBarVisible())

        fun withRecentsActivityVisible() =
            add(WindowManagerConditionsFactory.isRecentsActivityVisible())
                .add(WindowManagerConditionsFactory.isLayerVisible(LAUNCHER))
                .add(WindowManagerConditionsFactory.isAppTransitionIdle(Display.DEFAULT_DISPLAY))
                .add(WindowManagerConditionsFactory.hasLayersAnimating().negate())

        /**
         * Wait for specific rotation for the default display. Values are Surface#Rotation
         */
        @JvmOverloads
        fun withRotation(rotation: Int, displayId: Int = Display.DEFAULT_DISPLAY) =
            add(WindowManagerConditionsFactory.isAppTransitionIdle(Display.DEFAULT_DISPLAY))
                .add(WindowManagerConditionsFactory.hasRotation(rotation, displayId))

        fun withActivityState(activity: FlickerComponentName, activityState: String) = add(
            Condition("state of ${activity.toActivityName()} to be $activityState") {
                it.wmState.hasActivityState(activity.toActivityName(), activityState)
            })

        /**
         * Waits until the navigation and status bars are visible (windows and layers)
         */
        fun withNavBarStatusBarVisible() =
            add(WindowManagerConditionsFactory.isNavBarVisible())
                .add(WindowManagerConditionsFactory.isStatusBarVisible())

        fun withActivityRemoved(component: FlickerComponentName) =
            add(WindowManagerConditionsFactory.containsActivity(component).negate())
                .add(WindowManagerConditionsFactory.containsWindow(component).negate())
                .add(WindowManagerConditionsFactory.isAppTransitionIdle(Display.DEFAULT_DISPLAY))

        @JvmOverloads
        fun withAppTransitionIdle(displayId: Int = Display.DEFAULT_DISPLAY) =
            add(WindowManagerConditionsFactory.isAppTransitionIdle(displayId))
                .add(WindowManagerConditionsFactory.hasLayersAnimating().negate())

        fun withWindowSurfaceDisappeared(component: FlickerComponentName) =
            add(WindowManagerConditionsFactory.isWindowVisible(component).negate())
                .add(WindowManagerConditionsFactory.isLayerVisible(component).negate())
                .add(WindowManagerConditionsFactory.isAppTransitionIdle(Display.DEFAULT_DISPLAY))

        fun withWindowSurfaceAppeared(component: FlickerComponentName) =
            add(WindowManagerConditionsFactory.isWindowSurfaceShown(component))
                .add(WindowManagerConditionsFactory.isLayerVisible(component))
                .add(WindowManagerConditionsFactory.isAppTransitionIdle(Display.DEFAULT_DISPLAY))

        /**
         * Waits until the IME window and layer are visible
         */
        fun withImeShown() =
            add(WindowManagerConditionsFactory.isImeShown(Display.DEFAULT_DISPLAY))
                .add(WindowManagerConditionsFactory.isAppTransitionIdle(Display.DEFAULT_DISPLAY))

        /**
         * Waits until the IME layer is no longer visible. Cannot wait for the window as
         * its visibility information is updated at a later state and is not reliable in
         * the trace
         */
        fun withImeGone() =
            add(WindowManagerConditionsFactory.isLayerVisible(IME).negate())
                .add(WindowManagerConditionsFactory.isAppTransitionIdle(Display.DEFAULT_DISPLAY))

        /**
         * Waits until a window is in PIP mode. That is:
         *
         * - wait until a window is pinned ([WindowManagerState.pinnedWindows])
         * - no layers animating
         * - and [FlickerComponentName.PIP_CONTENT_OVERLAY] is no longer visible
         */
        fun withPipShown() =
            add(WindowManagerConditionsFactory.hasLayersAnimating().negate())
                .add(WindowManagerConditionsFactory.hasPipWindow())
                .add(WindowManagerConditionsFactory.isAppTransitionIdle(Display.DEFAULT_DISPLAY))

        /**
         * Waits until a window is no longer in PIP mode. That is:
         *
         * - wait until there are no pinned ([WindowManagerState.pinnedWindows])
         * - no layers animating
         * - and [FlickerComponentName.PIP_CONTENT_OVERLAY] is no longer visible
         */
        fun withPipGone() =
            add(WindowManagerConditionsFactory.hasLayersAnimating().negate())
                .add(WindowManagerConditionsFactory.hasPipWindow().negate())
                .add(WindowManagerConditionsFactory.isAppTransitionIdle(Display.DEFAULT_DISPLAY))

        fun withSnapshotGone() =
            add(WindowManagerConditionsFactory.isLayerVisible(SNAPSHOT).negate())
                .add(WindowManagerConditionsFactory.isAppTransitionIdle(Display.DEFAULT_DISPLAY))

        fun withSplashScreenGone() =
            add(WindowManagerConditionsFactory.isLayerVisible(SPLASH_SCREEN).negate())
                .add(WindowManagerConditionsFactory.isAppTransitionIdle(Display.DEFAULT_DISPLAY))

        fun withoutTopVisibleAppWindows() =
            add("noAppWindowsOnTop") {
                it.wmState.topVisibleAppWindow.isEmpty()
            }

        /**
         * Wait for the activities to appear in proper stacks and for valid state in AM and WM.
         * @param waitForActivityState array of activity states to wait for.
         */
        internal fun withValidState(vararg waitForActivityState: WaitForValidActivityState) =
            waitForValidStateCondition(*waitForActivityState)

        private fun waitForValidStateCondition(vararg waitForCondition: WaitForValidActivityState) =
            apply {
                add(WindowManagerConditionsFactory.isWMStateComplete())
                if (waitForCondition.isNotEmpty()) {
                    add(Condition("!shouldWaitForActivities") {
                        !shouldWaitForActivities(it, *waitForCondition)
                    })
                }
            }

        private fun isAppFullScreen(component: FlickerComponentName) =
            waitForValidStateCondition(WaitForValidActivityState
                .Builder(component)
                .setWindowingMode(WindowConfiguration.WINDOWING_MODE_FULLSCREEN)
                .setActivityType(WindowConfiguration.ACTIVITY_TYPE_STANDARD)
                .build()
            )
    }

    companion object {
        /**
         * @return true if should wait for some activities to become visible.
         */
        private fun shouldWaitForActivities(
            state: DeviceStateDump<WindowManagerState, BaseLayerTraceEntry>,
            vararg waitForActivitiesVisible: WaitForValidActivityState
        ): Boolean {
            if (waitForActivitiesVisible.isEmpty()) {
                return false
            }
            // If the caller is interested in waiting for some particular activity windows to be
            // visible before compute the state. Check for the visibility of those activity windows
            // and for placing them in correct stacks (if requested).
            var allActivityWindowsVisible = true
            var tasksInCorrectStacks = true
            for (activityState in waitForActivitiesVisible) {
                val matchingWindowStates = state.wmState.getMatchingVisibleWindowState(
                    activityState.windowName ?: "")
                val activityWindowVisible = matchingWindowStates.isNotEmpty()

                if (!activityWindowVisible) {
                    Log.i(LOG_TAG, "Activity window not visible: ${activityState.windowName}")
                    allActivityWindowsVisible = false
                } else if (activityState.activityName != null &&
                    !state.wmState.isActivityVisible(activityState.activityName.toActivityName())) {
                    Log.i(LOG_TAG, "Activity not visible: ${activityState.activityName}")
                    allActivityWindowsVisible = false
                } else {
                    // Check if window is already the correct state requested by test.
                    var windowInCorrectState = false
                    for (ws in matchingWindowStates) {
                        if (activityState.stackId != ActivityTaskManager.INVALID_STACK_ID &&
                            ws.stackId != activityState.stackId) {
                            continue
                        }
                        if (!ws.isWindowingModeCompatible(activityState.windowingMode)) {
                            continue
                        }
                        if (activityState.activityType !=
                                WindowConfiguration.ACTIVITY_TYPE_UNDEFINED &&
                            ws.activityType != activityState.activityType) {
                            continue
                        }
                        windowInCorrectState = true
                        break
                    }
                    if (!windowInCorrectState) {
                        Log.i(LOG_TAG, "Window in incorrect stack: $activityState")
                        tasksInCorrectStacks = false
                    }
                }
            }
            return !allActivityWindowsVisible || !tasksInCorrectStacks
        }

        private fun ConfigurationContainer.isWindowingModeCompatible(
            requestedWindowingMode: Int
        ): Boolean {
            return when (requestedWindowingMode) {
                WindowConfiguration.WINDOWING_MODE_UNDEFINED -> true
                else -> windowingMode == requestedWindowingMode
            }
        }
    }
}
