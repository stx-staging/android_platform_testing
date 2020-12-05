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

package com.android.server.wm.flicker.traces.windowmanager

import com.android.server.wm.flicker.assertions.AssertionResult
import com.android.server.wm.traces.common.Region
import com.android.server.wm.traces.common.windowmanager.WindowManagerTrace
import com.android.server.wm.traces.common.windowmanager.WindowManagerState
import com.android.server.wm.traces.common.windowmanager.windows.WindowState
import com.android.server.wm.traces.parser.toAndroidRegion
import java.nio.file.Paths

val WindowManagerTrace.sourcePath
    get() = if (this.source == "") {
        null
    } else {
        Paths.get(this.source)
    }

/**
 * Checks if the first window with title containing [windowTitle] has a visible area of at
 * least [testRegion], that is, if its area of the window frame covers each point in
 * the region.
 *
 * @param windowTitle Name of the layer to search
 * @param testRegion Expected visible area of the window
 */
fun WindowManagerState.coversAtLeastRegion(
    windowTitle: String,
    testRegion: Region
): AssertionResult {
    return coversAtLeastRegion(windowTitle, testRegion.toAndroidRegion())
}

/**
 * Checks if the first window with title containing [windowTitle] has a visible area of at
 * least [testRegion], that is, if its area of the window frame covers each point in
 * the region.
 *
 * @param windowTitle Name of the layer to search
 * @param testRegion Expected visible area of the window
 */
fun WindowManagerState.coversAtLeastRegion(
    windowTitle: String,
    testRegion: android.graphics.Region
): AssertionResult {
    return covers(windowTitle) { windowRegion ->
        val testRect = testRegion.bounds
        val intersection = windowRegion.toAndroidRegion()
        val covers = intersection.op(testRect, android.graphics.Region.Op.INTERSECT) &&
            !intersection.op(testRect, android.graphics.Region.Op.XOR)

        val reason = if (covers) {
            "$windowTitle covers region $testRegion"
        } else {
            "Region to test: $testRegion\nUncovered region: $intersection"
        }

        AssertionResult(reason, "coversAtLeastRegion", timestamp, success = covers)
    }
}

/**
 * Checks if the first window with title containing [windowTitle] has a visible area of at
 * most [testRegion], that is, if the region covers each point in the window frame.
 *
 * @param windowTitle Name of the layer to search
 * @param testRegion Expected visible area of the window
 */
fun WindowManagerState.coversAtMostRegion(
    windowTitle: String,
    testRegion: Region
): AssertionResult {
    return coversAtMostRegion(windowTitle, testRegion.toAndroidRegion())
}

/**
 * Checks if the first window with title containing [windowTitle] has a visible area of at
 * most [testRegion], that is, if the region covers each point in the window frame.
 *
 * @param windowTitle Name of the layer to search
 * @param testRegion Expected visible area of the window
 */
fun WindowManagerState.coversAtMostRegion(
    windowTitle: String,
    testRegion: android.graphics.Region
):
    AssertionResult {
    return covers(windowTitle) { windowRegion ->
        val testRect = testRegion.bounds
        val intersection = windowRegion.toAndroidRegion()
        val covers = intersection.op(testRect, android.graphics.Region.Op.INTERSECT) &&
            !intersection.op(windowRegion.toAndroidRegion(), android.graphics.Region.Op.XOR)

        val reason = if (covers) {
            "$windowTitle covers region $testRegion"
        } else {
            "Region to test: $testRegion\nOut-of-bounds region: $intersection"
        }

        AssertionResult(reason, "coversAtMostRegion", timestamp, success = covers)
    }
}

/** Checks if any of the given windows overlap with each other. */
fun WindowManagerState.noWindowsOverlap(
    partialWindowTitles: Set<String>
): AssertionResult {
    val foundWindows = partialWindowTitles.associateWith { title ->
        windowStates.find { it.title.contains(title) }
    }
        // keep entries only for windows that we actually found by removing nulls
        .filterValues { it != null }
        .mapValues { (_, v) -> v!!.frameRegion }

    val assertionName = "noWindowsOverlap"

    // ensure we found all required windows
    if (foundWindows.size < partialWindowTitles.size) {
        val notFound = partialWindowTitles - foundWindows.keys
        return AssertionResult(
            reason = "Could not find windows containing: [${notFound.joinToString(", ")}]",
            assertionName = assertionName,
            timestamp = timestamp,
            success = false
        )
    }

    val regions = foundWindows.entries.toList()
    for (i in regions.indices) {
        val (ourTitle, ourRegion) = regions[i]
        for (j in i + 1 until regions.size) {
            val (otherTitle, otherRegion) = regions[j]
            if (ourRegion.toAndroidRegion().op(otherRegion.toAndroidRegion(),
                    android.graphics.Region.Op.INTERSECT)) {
                return AssertionResult(
                    reason = "At least two windows overlap: $ourTitle, $otherTitle",
                    assertionName = assertionName,
                    timestamp = timestamp,
                    success = false
                )
            }
        }
    }

    return AssertionResult("No windows overlap", assertionName, timestamp, success = true)
}

private fun WindowManagerState.isWindowVisible(
    windows: Array<WindowState>,
    assertionName: String,
    windowTitle: String,
    isVisible: Boolean = true
): AssertionResult {
    val foundWindow = windows.filter { it.title.contains(windowTitle) }
    return when {
        windows.isEmpty() -> AssertionResult(
            "No windows found",
            assertionName,
            timestamp,
            success = false)
        foundWindow.isEmpty() -> AssertionResult(
            "$windowTitle cannot be found",
            assertionName,
            timestamp,
            success = false)
        isVisible && foundWindow.none { it.isVisible } -> AssertionResult(
            "$windowTitle is invisible",
            assertionName,
            timestamp,
            success = false)
        !isVisible && foundWindow.any { it.isVisible } -> AssertionResult(
            "$windowTitle is visible",
            assertionName,
            timestamp,
            success = false)
        else -> {
            val reason = if (isVisible) {
                "${foundWindow.first { it.isVisible }.title} is visible"
            } else {
                "${foundWindow.first { !it.isVisible }.title} is invisible"
            }
            AssertionResult(
                success = true,
                reason = reason)
        }
    }
}

/**
 * Checks if the non-app window with title containing [windowTitle] exists above the app
 * windows and if its visibility is equal to [isVisible]
 *
 * @param windowTitle window title to search
 * @param isVisible if the found window should be visible or not
 */
fun WindowManagerState.isAboveAppWindow(
    windowTitle: String,
    isVisible: Boolean = true
): AssertionResult = isWindowVisible(aboveAppWindows,
        "isAboveAppWindow${if (isVisible) "Visible" else "Invisible"}",
        windowTitle,
        isVisible)

/**
 * Checks if the non-app window with title containing [windowTitle] exists below the app
 * windows and if its visibility is equal to [isVisible]
 *
 * @param windowTitle window title to search
 * @param isVisible if the found window should be visible or not
 */
fun WindowManagerState.isBelowAppWindow(
    windowTitle: String,
    isVisible: Boolean = true
): AssertionResult = isWindowVisible(belowAppWindows,
        "isBelowAppWindow${if (isVisible) "Visible" else "Invisible"}",
        windowTitle,
        isVisible)

/**
 * Checks if non-app window with title containing the [windowTitle] exists above or below the
 * app windows and if its visibility is equal to [isVisible]
 *
 * @param windowTitle window title to search
 * @param isVisible if the found window should be visible or not
 */
fun WindowManagerState.hasNonAppWindow(
    windowTitle: String,
    isVisible: Boolean = true
): AssertionResult = isWindowVisible(nonAppWindows,
    "hasNonAppWindow${if (isVisible) "Visible" else "Invisible"}", windowTitle, isVisible)

/**
 * Checks if app window with title containing the [windowTitle] is on top
 *
 * @param windowTitle window title to search
 */
fun WindowManagerState.isVisibleAppWindowOnTop(windowTitle: String): AssertionResult {
    val success = topVisibleAppWindow.contains(windowTitle)
    val reason = "wanted=$windowTitle found=$topVisibleAppWindow"
    return AssertionResult(reason, "isAppWindowOnTop", timestamp, success)
}

/**
 * Checks if app window with title containing the [windowTitle] is visible
 *
 * @param windowTitle window title to search
 */
fun WindowManagerState.isAppWindowVisible(windowTitle: String): AssertionResult =
    isWindowVisible(appWindows, "isAppWindowVisible", windowTitle, isVisible = true)

/**
 * Obtains the region of the first visible window with title containing [windowTitle].
 *
 * @param windowTitle Name of the layer to search
 * @param resultComputation Predicate to compute a result based on the found window's region
 */
fun WindowManagerState.covers(
    windowTitle: String,
    resultComputation: (Region) -> AssertionResult
): AssertionResult {
    val assertionName = "covers"
    val visibilityCheck = isWindowVisible(windowStates, assertionName, windowTitle)
    if (!visibilityCheck.success) {
        return visibilityCheck
    }

    val foundWindow = windowStates.first { it.title.contains(windowTitle) }
    val foundRegion = foundWindow.frameRegion

    return resultComputation(foundRegion)
}

/**
 * Check if the window named [aboveWindowTitle] is above the one named [belowWindowTitle].
 */
fun WindowManagerState.isAboveWindow(
    aboveWindowTitle: String,
    belowWindowTitle: String
): AssertionResult {
    val assertionName = "isAboveWindow"

    // windows are ordered by z-order, from top to bottom
    val aboveZ = windowStates.indexOfFirst { aboveWindowTitle in it.title }
    val belowZ = windowStates.indexOfFirst { belowWindowTitle in it.title }

    val notFound = mutableSetOf<String>().apply {
        if (aboveZ == -1) {
            add(aboveWindowTitle)
        }
        if (belowZ == -1) {
            add(belowWindowTitle)
        }
    }

    if (notFound.isNotEmpty()) {
        return AssertionResult(
            reason = "Could not find ${notFound.joinToString(" and ")}!",
            assertionName = assertionName,
            timestamp = timestamp,
            success = false
        )
    }

    // ensure the z-order
    return AssertionResult(
        reason = "$aboveWindowTitle is above $belowWindowTitle",
        assertionName = assertionName,
        timestamp = timestamp,
        success = aboveZ < belowZ
    )
}
