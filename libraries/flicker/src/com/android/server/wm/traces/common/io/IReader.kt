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

package com.android.server.wm.traces.common.io

import com.android.server.wm.traces.common.AssertionTag
import com.android.server.wm.traces.common.Timestamp
import com.android.server.wm.traces.common.events.CujTrace
import com.android.server.wm.traces.common.events.EventLog
import com.android.server.wm.traces.common.layers.LayersTrace
import com.android.server.wm.traces.common.transactions.TransactionsTrace
import com.android.server.wm.traces.common.transition.TransitionsTrace
import com.android.server.wm.traces.common.windowmanager.WindowManagerTrace

/** Helper class to read results from a flicker artifact */
interface IReader {
    val artifactPath: String
    val executionError: Throwable?
    val runStatus: RunStatus
    val isFailure: Boolean
        get() = runStatus.isFailure

    /** @return a [WindowManagerTrace] from the dump associated to [tag] */
    fun readWmState(tag: String): WindowManagerTrace?

    /** @return a [WindowManagerTrace] for the part of the trace we want to run the assertions on */
    fun readWmTrace(): WindowManagerTrace?

    /** @return a [LayersTrace] for the part of the trace we want to run the assertions on */
    fun readLayersTrace(): LayersTrace?

    /** @return a [LayersTrace] from the dump associated to [tag] */
    fun readLayersDump(tag: String): LayersTrace?

    /** @return a [TransactionsTrace] for the part of the trace we want to run the assertions on */
    fun readTransactionsTrace(): TransactionsTrace?

    /** @return a [TransitionsTrace] for the part of the trace we want to run the assertions on */
    fun readTransitionsTrace(): TransitionsTrace?

    /** @return an [EventLog] for the part of the trace we want to run the assertions on */
    fun readEventLogTrace(): EventLog?

    /** @return a [CujTrace] for the part of the trace we want to run the assertions on */
    fun readCujTrace(): CujTrace?

    /** @return an [IReader] for the subsection of the trace we are reading in this reader */
    fun slice(startTimestamp: Timestamp, endTimestamp: Timestamp): IReader

    /**
     * @return [ByteArray] with the contents of a file from the artifact, or null if the file
     * doesn't exist
     */
    fun readBytes(traceType: TraceType, tag: String = AssertionTag.ALL): ByteArray?
}
