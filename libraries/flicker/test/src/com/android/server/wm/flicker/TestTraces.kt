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

import com.android.server.wm.flicker.io.TraceTime
import com.android.server.wm.flicker.traces.eventlog.FocusEvent

object TestTraces {
    object LayerTrace {
        const val ASSET = "layers_trace.winscope"
        const val START_TIME = 1618663562444
        const val SLICE_TIME = 1618715108595
        const val END_TIME = 1620770824112
        val FILE
            get() = readAssetAsFile(ASSET)
    }

    object WMTrace {
        const val ASSET = "wm_trace.winscope"
        const val START_TIME = 1618650751245
        const val SLICE_TIME = 1618730362295
        const val END_TIME = 1620756218174
        val FILE
            get() = readAssetAsFile(ASSET)
    }

    object TransactionTrace {
        const val ASSET = "transactions_trace.winscope"
        const val START_TIME = 1556111744859
        const val VALID_SLICE_TIME = 1556147625539
        const val INVALID_SLICE_TIME = 0L
        const val END_TIME = 1622127714039
        val FILE
            get() = readAssetAsFile(ASSET)
    }

    object TransitionTrace {
        const val ASSET = "transition_trace.winscope"
        const val START_TIME = 1619596081652
        const val VALID_SLICE_TIME = 1620068193157
        const val INVALID_SLICE_TIME = 0L
        const val END_TIME = 1620120359204
        val FILE
            get() = readAssetAsFile(ASSET)
    }

    val TIME_5 = TraceTime(5, 5, 5)
    val TIME_10 = TraceTime(10, 10, 10)

    val TEST_EVENT_LOG =
        listOf(
            FocusEvent(0, "WinB", FocusEvent.Focus.GAINED, "test"),
            FocusEvent(5, "test WinA window", FocusEvent.Focus.LOST, "test"),
            FocusEvent(6, "WinB", FocusEvent.Focus.LOST, "test"),
            FocusEvent(10, "test WinC", FocusEvent.Focus.GAINED, "test"),
            FocusEvent(12, "test WinD", FocusEvent.Focus.GAINED, "test")
        )

    val TEST_TRACE_CONFIG =
        TraceConfigs(
            wmTrace =
                TraceConfig(required = false, allowNoChange = false, usingExistingTraces = false),
            layersTrace =
                TraceConfig(required = false, allowNoChange = false, usingExistingTraces = false),
            transitionsTrace =
                TraceConfig(required = false, allowNoChange = false, usingExistingTraces = false),
            transactionsTrace =
                TraceConfig(required = false, allowNoChange = false, usingExistingTraces = false)
        )
}
