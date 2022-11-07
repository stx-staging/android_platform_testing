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

package com.android.server.wm.flicker.io

import com.android.server.wm.flicker.RunStatus
import com.android.server.wm.flicker.traces.eventlog.FocusEvent
import java.nio.file.Path

/** Contents of a flicker run (e.g. files, status, event log) */
data class ResultData(
    /** Path to the artifact file */
    val artifactPath: Path,
    /**
     * Event log contents
     *
     * TODO: Move to a file in the future
     */
    val eventLog: List<FocusEvent>?,
    /** Transition start and end time */
    val transitionTimeRange: TransitionTimeRange,
    /** Transition execution error (if any) */
    val executionError: Throwable?,
    val runStatus: RunStatus
) {
    override fun toString(): String = buildString {
        append(artifactPath)
        append(" (status=")
        append(runStatus)
        executionError?.let {
            append(", error=")
            append(it.message)
        }
        append(") ")
    }
}
