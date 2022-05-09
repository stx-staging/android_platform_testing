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

package com.android.server.wm.flicker.service.assertors

import android.content.ComponentName
import android.view.WindowManager.TRANSIT_OPEN
import com.android.server.wm.traces.common.FlickerComponentName
import com.android.server.wm.traces.common.transition.Transition

typealias ComponentBuilder = (t: Transition) -> FlickerComponentName

object Components {

    val NAV_BAR = ComponentBuilder(FlickerComponentName.NAV_BAR)
    val STATUS_BAR = ComponentBuilder(FlickerComponentName.STATUS_BAR)
    val LAUNCHER = ComponentBuilder(FlickerComponentName.LAUNCHER)

    val OPENING_APP = { t: Transition -> openingAppFrom(t) }
    val CLOSING_APP = { t: Transition -> closingAppFrom(t) }

    // TODO: Extract out common code between two functions below
    private fun openingAppFrom(transition: Transition): FlickerComponentName {
        val openingWindows = transition.changes.filter { it.transitMode == TRANSIT_OPEN }

        val windowNames = openingWindows.map { it.windowName }.distinct()
        if (windowNames.size > 1) {
            error(
                "Was not expecting more than one opening windowNames got " +
                    windowNames.joinToString()
            )
        }
        if (windowNames.isEmpty()) {
            error("No opening windows for $transition...")
        }

        // TODO: (b/231974873) use windowId instead of window name to match instead
        val openWindowName = openingWindows.first().windowName
        val component = android.content.ComponentName.unflattenFromString(openWindowName)

        return FlickerComponentName(component.getPackageName(), component.getClassName())
    }

    private fun closingAppFrom(transition: Transition): FlickerComponentName {
        val closingWindows = transition.changes.filter { it.transitMode == 2 /* TRANSIT CLOSE */ }

        val windowNames = closingWindows.map { it.windowName }.distinct()
        if (windowNames.size > 1) {
            error(
                "Was not expecting more than one opening windowNames got " +
                    windowNames.joinToString()
            )
        }
        if (windowNames.isEmpty()) {
            error("No closing windows for $transition...")
        }

        // TODO: (b/231974873) use windowId instead of window name to match instead
        val closeWindowName = closingWindows.firstOrNull()?.windowName
        val closeWindowPackage = closeWindowName?.split('/')?.get(0) ?: ""
        val closeWindowClass = closeWindowName?.split('/')?.get(0) ?: ""

        return FlickerComponentName(closeWindowPackage, closeWindowClass)
    }

    private fun ComponentBuilder(component: FlickerComponentName): ComponentBuilder {
        return { _: Transition -> component }
    }
}
