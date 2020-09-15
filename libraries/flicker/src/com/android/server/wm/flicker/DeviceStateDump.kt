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

package com.android.server.wm.flicker

import com.android.server.wm.flicker.traces.layers.LayersTrace
import com.android.server.wm.flicker.traces.windowmanager.WindowManagerTrace

/**
 * Represents a state dump containing the [WindowManagerTrace] and the [LayersTrace] both parsed
 * and in raw (byte) data.
 */
class DeviceStateDump(
    /**
     * [WindowManagerTrace] content
     */
    val wmTraceData: ByteArray,
    /**
     * [LayersTrace] content
     */
    val layersTraceData: ByteArray
) {
    /**
     * Parsed [WindowManagerTrace]
     */
    val wmTrace: WindowManagerTrace by lazy { WindowManagerTrace.parseFromDump(wmTraceData) }
    /**
     * Parsed [LayersTrace]
     */
    val layersTrace: LayersTrace by lazy { LayersTrace.parseFromDump(layersTraceData) }
}
