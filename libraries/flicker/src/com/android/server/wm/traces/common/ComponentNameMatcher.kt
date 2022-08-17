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

package com.android.server.wm.traces.common

import com.android.server.wm.traces.common.layers.Layer
import com.android.server.wm.traces.common.windowmanager.windows.Activity
import com.android.server.wm.traces.common.windowmanager.windows.WindowState
import kotlin.js.JsName

class ComponentNameMatcher(
    private val _components: Array<ComponentName>
) : IComponentMatcher {
    val components get() = _components.toMutableList()

    val packageNames: Array<String> get() =
        components.map { it.packageName }.toTypedArray()
    val classNames: Array<String> get() =
        components.map { it.className }.toTypedArray()

    init {
        require(_components.isNotEmpty()) { "No component specified" }
    }

    @JsName("matchesAnyOf")
    constructor(packageName: String, className: String) :
        this(arrayOf(ComponentName(packageName, className)))

    private fun <T> matchesAnyOf(
        values: Array<T>,
        valueProducer: (T) -> String,
        regexProducer: (ComponentName) -> Regex,
    ): Boolean = components
        .map { regexProducer.invoke(it) }
        .any { component ->
            val targets = values.map { valueProducer.invoke(it) }
            targets.any { value ->
                component.matches(value)
            }
        }

    /** {@inheritDoc} */
    override fun windowMatchesAnyOf(windows: Array<WindowState>): Boolean =
        matchesAnyOf(windows,
            { it.title },
            { it.toWindowNameRegex() }
        )

    /** {@inheritDoc} */
    override fun activityMatchesAnyOf(activities: Array<Activity>): Boolean =
        matchesAnyOf(activities,
            { it.name },
            { it.toActivityNameRegex() })

    /** {@inheritDoc} */
    override fun layerMatchesAnyOf(layers: Array<Layer>): Boolean =
        matchesAnyOf(layers,
            { it.name },
            { it.toLayerNameRegex() }
        )

    /** {@inheritDoc} */
    override fun toWindowIdentifier(): String =
        components.joinToString(" or ") { it.toWindowName() }

    /** {@inheritDoc} */
    override fun toLayerIdentifier(): String =
            components.joinToString(" or ") { it.toLayerName() }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ComponentNameMatcher) return false
        return components == other.components
    }

    override fun hashCode(): Int = components.hashCode()

    override fun toString(): String = components.joinToString(" or ")

    companion object {
        @JsName("NAV_BAR")
        val NAV_BAR = ComponentMatcher("", "NavigationBar0")
        @JsName("TASK_BAR")
        val TASK_BAR = ComponentMatcher("", "Taskbar")
        @JsName("STATUS_BAR")
        val STATUS_BAR = ComponentMatcher("", "StatusBar")
        @JsName("ROTATION")
        val ROTATION = ComponentMatcher("", "RotationLayer")
        @JsName("BACK_SURFACE")
        val BACK_SURFACE = ComponentMatcher("", "BackColorSurface")
        @JsName("IME")
        val IME = ComponentMatcher("", "InputMethod")
        @JsName("IME_SNAPSHOT")
        val IME_SNAPSHOT = ComponentMatcher("", "IME-snapshot-surface")
        @JsName("SPLASH_SCREEN")
        val SPLASH_SCREEN = ComponentMatcher("", "Splash Screen")
        @JsName("SNAPSHOT")
        val SNAPSHOT = ComponentMatcher("", "SnapshotStartingWindow")
        @JsName("LETTERBOX")
        val LETTERBOX = ComponentMatcher("", "Letterbox")
        @JsName("WALLPAPER_BBQ_WRAPPER")
        val WALLPAPER_BBQ_WRAPPER =
                ComponentMatcher("", "Wallpaper BBQ wrapper")
        @JsName("PIP_CONTENT_OVERLAY")
        val PIP_CONTENT_OVERLAY = ComponentMatcher("", "PipContentOverlay")
        @JsName("LAUNCHER")
        val LAUNCHER = ComponentMatcher("com.google.android.apps.nexuslauncher",
                "com.google.android.apps.nexuslauncher.NexusLauncherActivity")
        @JsName("SPLIT_DIVIDER")
        val SPLIT_DIVIDER = ComponentMatcher("", "StageCoordinatorSplitDivider")

        /**
         * Creates a component matcher from a window or layer name.
         *
         * Requires the [str] to contain both the package and class name (with a / separator)
         *
         * @param str Value to parse
         */
        @JsName("unflattenFromString")
        fun unflattenFromString(str: String): IComponentMatcher {
            val sep = str.indexOf('/')
            if (sep < 0 || sep + 1 >= str.length) {
                error("Missing package/class separator")
            }
            val pkg = str.substring(0, sep)
            var cls = str.substring(sep + 1)
            if (cls.isNotEmpty() && cls[0] == '.') {
                cls = pkg + cls
            }
            return ComponentNameMatcher(pkg, cls)
        }
    }
}
