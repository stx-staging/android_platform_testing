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

package android.tools.common.traces.wm

import android.tools.common.ITraceEntry
import android.tools.common.Timestamp
import android.tools.common.traces.surfaceflinger.Transaction
import android.tools.common.traces.surfaceflinger.TransactionsTrace
import kotlin.js.JsName

class Transition(
    @JsName("createTime") val createTime: Timestamp,
    @JsName("sendTime") val sendTime: Timestamp,
    @JsName("finishTime") val finishTime: Timestamp,
    @JsName("startTransactionId") val startTransactionId: Long,
    @JsName("finishTransactionId") val finishTransactionId: Long,
    @JsName("type") val type: TransitionType,
    @JsName("changes") val changes: List<TransitionChange>,
    @JsName("played") val played: Boolean,
    @JsName("aborted") val aborted: Boolean
) : ITraceEntry {
    override val timestamp = createTime

    @JsName("getStartTransaction")
    fun getStartTransaction(transactionsTrace: TransactionsTrace): Transaction? {
        return transactionsTrace.allTransactions.firstOrNull { it.id == this.startTransactionId }
    }

    @JsName("getFinishTransaction")
    fun getFinishTransaction(transactionsTrace: TransactionsTrace): Transaction? {
        return transactionsTrace.allTransactions.firstOrNull { it.id == this.finishTransactionId }
    }

    @JsName("isIncomplete")
    val isIncomplete: Boolean
        get() = !played || aborted

    override fun toString(): String =
        "Transition#${hashCode()}" +
            "(\ntype=$type\naborted=$aborted,\nplayed=$played," +
            "\ncreateTime=$createTime,\nsendTime=$sendTime,\nfinishTime=$finishTime," +
            "\nstartTransactionId=$startTransactionId,\nfinishTransactionId=$finishTransactionId," +
            "\nchanges=[\n${changes.joinToString(",\n").prependIndent()}\n])"
}
