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

package com.android.server.wm.flicker.service.assertors

import com.android.server.wm.traces.common.layers.LayersTrace
import com.android.server.wm.traces.common.prettyTimestamp
import com.android.server.wm.traces.common.transition.Transition
import com.android.server.wm.traces.common.windowmanager.WindowManagerTrace
import com.google.common.truth.Fact

data class FaasData(
    val transition: Transition,
    val entireWmTrace: WindowManagerTrace,
    val entireLayersTrace: LayersTrace
) {
    fun toFacts(): Collection<Fact> {
        val unclippedFirstTimestamp = entireWmTrace.firstOrNull()?.timestamp ?: 0L
        val unclippedLastTimestamp = entireWmTrace.lastOrNull()?.timestamp ?: 0L
        val unclippedTraceFirst = "${prettyTimestamp(unclippedFirstTimestamp)} " +
            "(timestamp=$unclippedFirstTimestamp)"
        val unclippedTraceLast = "${prettyTimestamp(unclippedLastTimestamp)} " +
            "(timestamp=$unclippedLastTimestamp)"

        return listOf(
            Fact.fact("Transition type", transition.type),
            Fact.fact(
                "Transition start",
                "${prettyTimestamp(transition.start)} (timestamp=${transition.start})"
            ),
            Fact.fact(
                "Transition end",
                "${prettyTimestamp(transition.end)} (timestamp=${transition.end})"
            ),
            Fact.fact("Transition type", transition.type),
            Fact.fact(
                "Transition changes",
                transition.changes
                    .joinToString("\n  -", "\n  -") {
                        "${it.transitMode} ${it.windowName}"
                    }
            ),
            Fact.fact("Extracted from trace start", unclippedTraceFirst),
            Fact.fact("Extracted from trace end", unclippedTraceLast)
        )
    }
}
