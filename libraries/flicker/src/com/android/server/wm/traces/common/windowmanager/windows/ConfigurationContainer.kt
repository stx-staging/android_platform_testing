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

package com.android.server.wm.traces.common.windowmanager.windows

import kotlin.js.JsName

/**
 * Represents the configuration of an element in the window manager hierarchy
 *
 * This is a generic object that is reused by both Flicker and Winscope and cannot access internal
 * Java/Android functionality
 */
open class ConfigurationContainer(
    @JsName("overrideConfiguration") val overrideConfiguration: Configuration?,
    @JsName("fullConfiguration") val fullConfiguration: Configuration?,
    @JsName("mergedOverrideConfiguration") val mergedOverrideConfiguration: Configuration?
) {
    constructor(
        configurationContainer: ConfigurationContainer
    ) : this(
        configurationContainer.overrideConfiguration,
        configurationContainer.fullConfiguration,
        configurationContainer.mergedOverrideConfiguration
    )

    @JsName("windowingMode")
    val windowingMode: Int
        get() = fullConfiguration?.windowConfiguration?.windowingMode ?: 0

    @JsName("activityType")
    open val activityType: Int
        get() = fullConfiguration?.windowConfiguration?.activityType ?: 0

    @JsName("isEmpty")
    open val isEmpty: Boolean
        get() =
            (overrideConfiguration?.isEmpty
                ?: true) &&
                (fullConfiguration?.isEmpty ?: true) &&
                (mergedOverrideConfiguration?.isEmpty ?: true)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ConfigurationContainer) return false

        if (overrideConfiguration != other.overrideConfiguration) return false
        if (fullConfiguration != other.fullConfiguration) return false
        if (mergedOverrideConfiguration != other.mergedOverrideConfiguration) return false

        return true
    }

    override fun hashCode(): Int {
        var result = overrideConfiguration?.hashCode() ?: 0
        result = 31 * result + (fullConfiguration?.hashCode() ?: 0)
        result = 31 * result + (mergedOverrideConfiguration?.hashCode() ?: 0)
        return result
    }
}
