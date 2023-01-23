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

package com.android.server.wm.flicker.service.extractors

import com.android.server.wm.flicker.helpers.MILLISECOND_AS_NANOSECONDS
import com.android.server.wm.flicker.io.IReader
import com.android.server.wm.flicker.service.ScenarioInstance
import com.android.server.wm.flicker.service.config.FaasScenarioType
import com.android.server.wm.traces.common.events.CujType
import com.android.server.wm.traces.common.transition.Transition

class TaggedScenarioExtractor(
    val targetTag: CujType,
    private val transitionFilter: (Transition) -> Boolean,
    val type: FaasScenarioType,
    val associatedTransitionRequired: Boolean = true
) : IScenarioExtractor {
    override fun extract(reader: IReader): List<ScenarioInstance> {

        val wmTrace = reader.readWmTrace() ?: error("Missing window manager trace")
        val layersTrace = reader.readLayersTrace() ?: error("Missing layers trace")
        val transitionsTrace = reader.readTransitionsTrace() ?: error("Missing transitions trace")
        val cujTrace = reader.readCujTrace() ?: error("Missing CUJ trace")

        val targetCujEntries =
            cujTrace.entries.filter { it.cuj === targetTag }.filter { !it.canceled }

        if (targetCujEntries.isEmpty()) {
            // No scenarios to extract here
            return emptyList()
        }

        val relevantTransitions =
            transitionsTrace.entries
                .filter { !it.isIncomplete }
                .filter { transitionFilter(it) }
                .toMutableSet()

        if (associatedTransitionRequired) {
            require(relevantTransitions.isNotEmpty()) {
                "Required an associated transition for ${targetTag.name} but none was found in " +
                    "[\n${transitionsTrace.entries.joinToString(",\n").prependIndent()}\n]!"
            }
        }

        return targetCujEntries.map { cujEntry ->
            val associatedTransitions =
                relevantTransitions.filter { transition ->
                    require(
                        cujEntry.startTimestamp.unixNanos !=
                            com.android.server.wm.traces.common.Timestamp.NULL_TIMESTAMP
                    )
                    require(
                        cujEntry.endTimestamp.unixNanos !=
                            com.android.server.wm.traces.common.Timestamp.NULL_TIMESTAMP
                    )
                    require(
                        transition.start.unixNanos !=
                            com.android.server.wm.traces.common.Timestamp.NULL_TIMESTAMP
                    ) { "transition.start = ${transition.start} has no unix timestamp" }
                    require(
                        transition.end.unixNanos !=
                            com.android.server.wm.traces.common.Timestamp.NULL_TIMESTAMP
                    ) { "transition.end = ${transition.end} has no unix timestamp" }

                    // Compare at millisecond precision
                    // TODO: Might make sense to create a function on Timestamp for that directly.
                    val cujContainedWithinTransition =
                        transition.start.unixNanos / MILLISECOND_AS_NANOSECONDS <=
                            cujEntry.startTimestamp.unixNanos / MILLISECOND_AS_NANOSECONDS &&
                            cujEntry.endTimestamp.unixNanos / MILLISECOND_AS_NANOSECONDS <=
                                transition.end.unixNanos / MILLISECOND_AS_NANOSECONDS
                    val transitionStartsDuringCuj =
                        cujEntry.startTimestamp.unixNanos / MILLISECOND_AS_NANOSECONDS <=
                            transition.start.unixNanos / MILLISECOND_AS_NANOSECONDS
                    val transitionEndsDuringCuj =
                        transition.end.unixNanos / MILLISECOND_AS_NANOSECONDS <=
                            cujEntry.endTimestamp.unixNanos / MILLISECOND_AS_NANOSECONDS

                    // TODO: Doesn't feel robust enough...
                    return@filter cujContainedWithinTransition ||
                        transitionStartsDuringCuj ||
                        transitionEndsDuringCuj
                }

            if (associatedTransitionRequired) {
                require(associatedTransitions.isNotEmpty()) {
                    "Required an associated transition for " +
                        "${targetTag.name}(${cujEntry.startTimestamp.unixNanos}," +
                        "${cujEntry.endTimestamp.unixNanos}) but no relevant transition " +
                        "happens in the right time range " +
                        "[\n${relevantTransitions.joinToString(",\n").prependIndent()}\n]!"
                }
            }

            if (associatedTransitions.isNotEmpty()) {
                require(associatedTransitions.size == 1) {
                    "Should have only one matching transition for cuj " +
                        "${cujEntry.cuj}(${cujEntry.startTimestamp.unixNanos}," +
                        "${cujEntry.endTimestamp.unixNanos}) but " +
                        "${associatedTransitions.size} matched " +
                        "(\n${associatedTransitions.joinToString(",\n").prependIndent()}\n)!"
                }
            }

            val associatedTransition =
                if (associatedTransitions.isNotEmpty()) associatedTransitions[0] else null

            ScenarioInstance(
                type,
                // TODO: Use new viewer API to get associated start and end
                startRotation =
                    wmTrace
                        .getLastEntryByUnixTimestamp(cujEntry.startTimestamp.unixNanos)
                        .policy
                        ?.rotation
                        ?: error("missing rotation in policy"),
                endRotation =
                    wmTrace
                        .getLastEntryByUnixTimestamp(cujEntry.endTimestamp.unixNanos)
                        .policy
                        ?.rotation
                        ?: error("missing rotation in policy"),
                startTimestamp = cujEntry.startTimestamp,
                endTimestamp = cujEntry.endTimestamp,
                associatedCuj = cujEntry.cuj,
                associatedTransition = associatedTransition,
                reader = reader.slice(cujEntry.startTimestamp, cujEntry.endTimestamp)
            )
        }
    }
}
