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

package com.android.server.wm.flicker.service.assertors.assertions

import com.android.server.wm.flicker.service.assertors.BaseAssertionBuilder
import com.android.server.wm.flicker.traces.layers.LayersTraceSubject
import com.android.server.wm.traces.common.transition.Transition

/**
 * Checks if the stack space of all displays is fully covered by any visible layer,
 * during the whole transitions
 */
class EntireScreenCoveredAlways : BaseAssertionBuilder() {
    /** {@inheritDoc} */
    override fun doEvaluate(
        transition: Transition,
        layerSubject: LayersTraceSubject
    ) {
        layerSubject.invoke("entireScreenCovered") { entry ->
            val displays = entry.entry.displays
            if (displays.isEmpty()) {
                entry.fail("No displays found")
            }
            displays.forEach { display ->
                entry.visibleRegion().coversAtLeast(display.layerStackSpace)
            }
        }.forAllEntries()
    }

    override fun equals(assertion: Any?): Boolean {
        return assertion is EntireScreenCoveredAlways
    }
}