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

package com.android.server.wm.flicker.runner

import com.android.server.wm.flicker.io.ResultData
import com.android.server.wm.flicker.io.TraceTime
import com.google.common.truth.Truth

object TestUtils {
    internal fun validateTransitionTime(result: ResultData) {
        val startTime = result.transitionTimeRange.start
        val endTime = result.transitionTimeRange.end
        validateTimeGreaterThan(startTime, "Start time", TraceTime.MIN)
        validateTimeGreaterThan(endTime, "End time", TraceTime.MIN)
        validateTimeGreaterThan(TraceTime.MAX, "End time", endTime)
    }

    internal fun validateTransitionTimeIsEmpty(result: ResultData) {
        val startTime = result.transitionTimeRange.start
        val endTime = result.transitionTimeRange.end
        validateEqualTo(startTime, "Start time", TraceTime.MIN)
        validateEqualTo(endTime, "End time", TraceTime.MAX)
    }

    private fun validateEqualTo(time: TraceTime, name: String, expectedValue: TraceTime) {
        Truth.assertWithMessage("$name - systemTime")
            .that(time.systemTime)
            .isEqualTo(expectedValue.systemTime)
        Truth.assertWithMessage("$name - unixTimeNanos")
            .that(time.unixTimeNanos)
            .isEqualTo(expectedValue.unixTimeNanos)
        Truth.assertWithMessage("$name - elapsedRealtimeNanos")
            .that(time.elapsedRealtimeNanos)
            .isEqualTo(expectedValue.elapsedRealtimeNanos)
    }

    private fun validateTimeGreaterThan(time: TraceTime, name: String, minValue: TraceTime) {
        Truth.assertWithMessage("$name - systemTime")
            .that(time.systemTime)
            .isGreaterThan(minValue.systemTime)
        Truth.assertWithMessage("$name - unixTimeNanos")
            .that(time.unixTimeNanos)
            .isGreaterThan(minValue.unixTimeNanos)
        Truth.assertWithMessage("$name - elapsedRealtimeNanos")
            .that(time.elapsedRealtimeNanos)
            .isGreaterThan(minValue.elapsedRealtimeNanos)
    }
}
