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

import com.android.server.wm.flicker.Utils
import com.android.server.wm.traces.common.Timestamp
import com.android.server.wm.traces.common.io.RunStatus
import com.android.server.wm.traces.common.io.TransitionTimeRange
import java.io.File

/**
 * Contents of a flicker run (e.g. files, status, event log)
 *
 * @param _artifact Path to the artifact file
 * @param _transitionTimeRange Transition start and end time
 * @param _executionError Transition execution error (if any)
 * @param _runStatus Status of the run
 */
open class ResultData(
    _artifact: File,
    _transitionTimeRange: TransitionTimeRange,
    _executionError: Throwable?,
    _runStatus: RunStatus
) : IResultData {
    final override var artifact: File = _artifact
        private set
    final override var transitionTimeRange: TransitionTimeRange = _transitionTimeRange
        private set
    final override var executionError: Throwable? = _executionError
        private set
    final override var runStatus: RunStatus = _runStatus
        private set

    /** {@inheritDoc} */
    override fun getArtifactBytes(): ByteArray = artifact.readBytes()

    /** {@inheritDoc} */
    override fun slice(startTimestamp: Timestamp, endTimestamp: Timestamp) = apply {
        require(startTimestamp.hasAllTimestamps)
        require(endTimestamp.hasAllTimestamps)
        return ResultData(
            artifact,
            TransitionTimeRange(startTimestamp, endTimestamp),
            executionError,
            runStatus
        )
    }

    override fun toString(): String = buildString {
        append(artifact)
        append(" (status=")
        append(runStatus)
        executionError?.let {
            append(", error=")
            append(it.message)
        }
        append(") ")
    }

    /** {@inheritDoc} */
    override fun updateStatus(newStatus: RunStatus) = apply {
        val currFile = artifact
        require(RunStatus.fromFileName(currFile.name) != RunStatus.UNDEFINED) {
            "File name should start with a value from `RunStatus`, instead it was $currFile"
        }
        val newFile = getNewFilePath(newStatus)
        if (currFile != newFile) {
            Utils.moveFile(currFile, newFile)
            artifact = newFile
            runStatus = newStatus
        }
    }
}
