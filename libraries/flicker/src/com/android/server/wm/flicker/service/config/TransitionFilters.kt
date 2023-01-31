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

package com.android.server.wm.flicker.service.config

import com.android.server.wm.traces.common.transition.Transition

object TransitionFilters {
    val OPEN_APP_TRANSITION_FILTER: (Transition) -> Boolean = { t ->
        t.changes.any {
            it.transitMode == Transition.Companion.Type.OPEN || // cold launch
            it.transitMode == Transition.Companion.Type.TO_FRONT // warm launch
        }
    }

    val CLOSE_APP_TO_LAUNCHER_FILTER: (Transition) -> Boolean = { t ->
        t.changes.any {
            it.transitMode == Transition.Companion.Type.CLOSE ||
                it.transitMode == Transition.Companion.Type.TO_BACK
        } &&
            t.changes.any {
                // TODO: Match layer id to launcher layer id
                //                    it.windowName ==
                // ComponentNameMatcher.LAUNCHER.toActivityIdentifier() &&
                it.transitMode == Transition.Companion.Type.TO_FRONT
            }
    }
}
