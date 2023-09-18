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

package android.tools.common.flicker.config.applaunch

import android.tools.common.flicker.ScenarioInstance
import android.tools.common.flicker.assertors.ComponentTemplate
import android.tools.common.traces.component.FullComponentIdMatcher
import android.tools.common.traces.component.IComponentMatcher
import android.tools.common.traces.surfaceflinger.LayersTrace
import android.tools.common.traces.wm.Transition
import android.tools.common.traces.wm.TransitionType
import android.tools.common.traces.wm.WindowManagerTrace

object Components {
    val OPENING_APP =
        ComponentTemplate("OPENING_APP") { scenarioInstance: ScenarioInstance ->
            openingAppFrom(
                scenarioInstance.associatedTransition ?: error("Missing associated transition"),
                scenarioInstance.reader.readLayersTrace(),
                scenarioInstance.reader.readWmTrace()
            )
        }

    private fun openingAppFrom(
        transition: Transition,
        layersTrace: LayersTrace?,
        wmTrace: WindowManagerTrace?
    ): IComponentMatcher {
        val targetChanges =
            transition.changes.filter {
                it.transitMode == TransitionType.OPEN || it.transitMode == TransitionType.TO_FRONT
            }

        var openingLayerId: Int? = null
        if (layersTrace != null) {
            val openingLayers =
                targetChanges.map {
                    layersTrace.getLayerDescriptorById(it.layerId)
                        ?: error("Failed to find layer with id ${it.layerId}")
                }

            val openingAppLayers = openingLayers.filter { it.isAppLayer }

            require(openingAppLayers.size == 1) {
                "Expected 1 opening app layer but got ${openingAppLayers.size}"
            }

            openingLayerId = openingAppLayers.first().id
        }

        var openingWindowId: Int? = null
        if (wmTrace != null) {
            val openingWindows =
                targetChanges.map {
                    wmTrace.getWindowDescriptorById(it.windowId)
                        ?: error("Failed to find window with id ${it.windowId}")
                }

            val openingAppWindows = openingWindows.filter { it.isAppWindow }

            require(openingAppWindows.size == 1) {
                "Expected 1 opening app window but got ${openingAppWindows.size}"
            }

            openingWindowId = openingAppWindows.first().id
        }

        return FullComponentIdMatcher(openingWindowId ?: -1, openingLayerId ?: -1)
    }
}
