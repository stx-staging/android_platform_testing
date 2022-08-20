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

package com.android.server.wm.traces.common

import com.android.server.wm.traces.common.layers.BaseLayerTraceEntry
import com.android.server.wm.traces.common.windowmanager.WindowManagerState
import kotlin.js.JsName

/**
 * Represents a state dump optionally containing the [WindowManagerState] and
 * the [BaseLayerTraceEntry] parsed.
 */
open class NullableDeviceStateDump(
    /**
     * Parsed [WindowManagerState]
     */
    @JsName("wmState")
    open val wmState: WindowManagerState?,

    /**
     * Parsed [BaseLayerTraceEntry]
     */
    @JsName("layerState")
    open val layerState: BaseLayerTraceEntry?
)
