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

class FlickerComponentName(
    _components: Array<FlickerComponent>
) {
    private val components = _components.toMutableList()

    val packageNames: Array<String> get() = components.map { it.packageName }.toTypedArray()
    val classNames: Array<String> get() = components.map { it.className }.toTypedArray()

    init {
        require(_components.isNotEmpty()) { "No component specified" }
    }

    constructor(packageName: String, className: String) :
        this(arrayOf(FlickerComponent(packageName, className)))

    fun or(other: FlickerComponentName): FlickerComponentName {
        val newComponents = components.toMutableList()
            .also { it.addAll(other.components) }
        return FlickerComponentName(newComponents.toTypedArray())
    }

    private fun <T> matchesAnyOf(
        values: Array<T>,
        valueProducer: (T) -> String,
        regexProducer: (FlickerComponent) -> Regex,
    ): Boolean = components
        .map { regexProducer.invoke(it) }
        .any { component ->
            val targets = values.map { valueProducer.invoke(it) }
            targets.any { value ->
                component.matches(value)
            }
        }

    fun toActivityRecordFilter(): Regex {
        require(components.size == 1) { "SHould have a single component, instead was $this" }
        return components.first().toActivityRecordFilter()
    }

    fun windowMatchesAnyOf(values: WindowState): Boolean =
        windowMatchesAnyOf(arrayOf(values))

    fun windowMatchesAnyOf(values: Collection<WindowState>): Boolean =
        windowMatchesAnyOf(values.toTypedArray())

    fun windowMatchesAnyOf(values: Array<WindowState>): Boolean =
        matchesAnyOf(values,
            { it.title },
            { it.toWindowNameRegex() }
        )

    fun activityMatchesAnyOf(values: Activity): Boolean =
        activityMatchesAnyOf(arrayOf(values))

    fun activityMatchesAnyOf(values: Collection<Activity>): Boolean =
        activityMatchesAnyOf(values.toTypedArray())

    fun activityMatchesAnyOf(values: Array<Activity>): Boolean =
        matchesAnyOf(values,
            { it.name },
            { it.toActivityNameRegex() })

    fun layerMatchesAnyOf(values: Layer): Boolean =
        layerMatchesAnyOf(arrayOf(values))

    fun layerMatchesAnyOf(values: Collection<Layer>): Boolean =
        layerMatchesAnyOf(values.toTypedArray())

    fun layerMatchesAnyOf(values: Array<Layer>): Boolean =
        matchesAnyOf(values,
            { it.name },
            { it.toLayerNameRegex() }
        )

    fun toActivityName(): String = components.joinToString("or") { it.toActivityName() }
    fun toWindowName(): String = components.joinToString("or") { it.toWindowName() }
    fun toLayerName(): String = components.joinToString("or") { it.toLayerName() }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FlickerComponentName) return false
        return components == other.components
    }

    override fun hashCode(): Int = components.hashCode()

    override fun toString(): String = components.joinToString("or")

    companion object {
        val NAV_BAR = FlickerComponentName("", "NavigationBar0")
        val STATUS_BAR = FlickerComponentName("", "StatusBar")
        val ROTATION = FlickerComponentName("", "RotationLayer")
        val BACK_SURFACE = FlickerComponentName("", "BackColorSurface")
        val IME = FlickerComponentName("", "InputMethod")
        val IME_SNAPSHOT = FlickerComponentName("", "IME-snapshot-surface")
        val SPLASH_SCREEN = FlickerComponentName("", "Splash Screen")
        val SNAPSHOT = FlickerComponentName("", "SnapshotStartingWindow")
        val LETTERBOX = FlickerComponentName("", "Letterbox")
        val WALLPAPER_BBQ_WRAPPER =
                FlickerComponentName("", "Wallpaper BBQ wrapper")
        val PIP_CONTENT_OVERLAY = FlickerComponentName("", "PipContentOverlay")
        val LAUNCHER = FlickerComponentName("com.google.android.apps.nexuslauncher",
            "com.google.android.apps.nexuslauncher.NexusLauncherActivity")

        fun unflattenFromString(str: String): FlickerComponentName {
            val sep = str.indexOf('/')
            if (sep < 0 || sep + 1 >= str.length) {
                error("Missing package/class separator")
            }
            val pkg = str.substring(0, sep)
            var cls = str.substring(sep + 1)
            if (cls.isNotEmpty() && cls[0] == '.') {
                cls = pkg + cls
            }
            return FlickerComponentName(pkg, cls)
        }
    }
}
