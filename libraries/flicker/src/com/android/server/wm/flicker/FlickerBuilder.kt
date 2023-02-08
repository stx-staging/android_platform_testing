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

package com.android.server.wm.flicker

import android.app.Instrumentation
import androidx.test.uiautomator.UiDevice
import com.android.server.wm.flicker.helpers.isShellTransitionsEnabled
import com.android.server.wm.flicker.io.TraceType
import com.android.server.wm.flicker.monitor.EventLogMonitor
import com.android.server.wm.flicker.monitor.ITransitionMonitor
import com.android.server.wm.flicker.monitor.LayersTraceMonitor
import com.android.server.wm.flicker.monitor.NoTraceMonitor
import com.android.server.wm.flicker.monitor.ScreenRecorder
import com.android.server.wm.flicker.monitor.TransactionsTraceMonitor
import com.android.server.wm.flicker.monitor.TransitionsTraceMonitor
import com.android.server.wm.flicker.monitor.WindowManagerTraceMonitor
import com.android.server.wm.traces.common.layers.BaseLayerTraceEntry
import com.android.server.wm.traces.common.layers.LayersTrace
import com.android.server.wm.traces.common.transactions.TransactionsTrace
import com.android.server.wm.traces.common.transition.TransitionsTrace
import com.android.server.wm.traces.common.windowmanager.WindowManagerState
import com.android.server.wm.traces.common.windowmanager.WindowManagerTrace
import com.android.server.wm.traces.parser.windowmanager.WindowManagerStateHelper
import java.io.File

/** Build Flicker tests using Flicker DSL */
@FlickerDslMarker
class FlickerBuilder(
    private val instrumentation: Instrumentation,
    private val outputDir: File = getDefaultFlickerOutputDir(),
    private val wmHelper: WindowManagerStateHelper =
        WindowManagerStateHelper(instrumentation, clearCacheAfterParsing = false),
    private val setupCommands: MutableList<IFlickerTestData.() -> Any> = mutableListOf(),
    private val transitionCommands: MutableList<IFlickerTestData.() -> Any> = mutableListOf(),
    private val teardownCommands: MutableList<IFlickerTestData.() -> Any> = mutableListOf(),
    val device: UiDevice = UiDevice.getInstance(instrumentation),
    private val traceMonitors: MutableList<ITransitionMonitor> =
        mutableListOf<ITransitionMonitor>().also {
            it.add(WindowManagerTraceMonitor())
            it.add(LayersTraceMonitor())
            if (isShellTransitionsEnabled) {
                // Transition tracing only works if shell transitions are enabled.
                it.add(TransitionsTraceMonitor())
            }
            it.add(TransactionsTraceMonitor())
            it.add(ScreenRecorder(instrumentation.targetContext))
            it.add(EventLogMonitor())
        }
) {
    private var usingExistingTraces = false

    /**
     * Configure a [WindowManagerTraceMonitor] to obtain [WindowManagerTrace]
     *
     * By default, the tracing is always active. To disable tracing return null
     *
     * If this tracing is disabled, the assertions for [WindowManagerTrace] and [WindowManagerState]
     * will not be executed
     */
    fun withWindowManagerTracing(traceMonitor: () -> WindowManagerTraceMonitor?): FlickerBuilder =
        apply {
            traceMonitors.removeIf { it is WindowManagerTraceMonitor }
            addMonitor(traceMonitor())
        }

    /** Disable [LayersTraceMonitor]. */
    fun withoutLayerTracing(): FlickerBuilder = apply { withLayerTracing { null } }

    /**
     * Configure a [LayersTraceMonitor] to obtain [LayersTrace].
     *
     * By default the tracing is always active. To disable tracing return null
     *
     * If this tracing is disabled, the assertions for [LayersTrace] and [BaseLayerTraceEntry] will
     * not be executed
     */
    fun withLayerTracing(traceMonitor: () -> LayersTraceMonitor?): FlickerBuilder = apply {
        traceMonitors.removeIf { it is LayersTraceMonitor }
        addMonitor(traceMonitor())
    }

    /** Disable [TransitionsTraceMonitor]. */
    fun withoutTransitionTracing(): FlickerBuilder = apply { withTransitionTracing { null } }

    /**
     * Configure a [TransitionsTraceMonitor] to obtain [TransitionsTrace].
     *
     * By default, shell transition tracing is disabled.
     */
    fun withTransitionTracing(traceMonitor: () -> TransitionsTraceMonitor?): FlickerBuilder =
        apply {
            traceMonitors.removeIf { it is TransitionsTraceMonitor }
            addMonitor(traceMonitor())
        }

    /** Disable [TransactionsTraceMonitor]. */
    fun withoutTransactionsTracing(): FlickerBuilder = apply { withTransactionsTracing { null } }

    /**
     * Configure a [TransactionsTraceMonitor] to obtain [TransactionsTrace].
     *
     * By default, shell transition tracing is disabled.
     */
    fun withTransactionsTracing(traceMonitor: () -> TransactionsTraceMonitor?): FlickerBuilder =
        apply {
            traceMonitors.removeIf { it is TransactionsTraceMonitor }
            addMonitor(traceMonitor())
        }

    /**
     * Configure a [ScreenRecorder].
     *
     * By default, the tracing is always active. To disable tracing return null
     */
    fun withScreenRecorder(screenRecorder: () -> ScreenRecorder?): FlickerBuilder = apply {
        traceMonitors.removeIf { it is ScreenRecorder }
        addMonitor(screenRecorder())
    }

    fun withoutScreenRecorder(): FlickerBuilder = apply {
        traceMonitors.removeIf { it is ScreenRecorder }
    }

    /** Defines the setup commands executed before the [transitions] to test */
    fun setup(commands: IFlickerTestData.() -> Unit): FlickerBuilder = apply {
        setupCommands.add(commands)
    }

    /** Defines the teardown commands executed after the [transitions] to test */
    fun teardown(commands: IFlickerTestData.() -> Unit): FlickerBuilder = apply {
        teardownCommands.add(commands)
    }

    /** Defines the commands that trigger the behavior to test */
    fun transitions(command: IFlickerTestData.() -> Unit): FlickerBuilder = apply {
        require(!usingExistingTraces) {
            "Can't update transition after calling usingExistingTraces"
        }
        transitionCommands.add(command)
    }

    data class TraceFiles(
        val wmTrace: File,
        val layersTrace: File,
        val transactions: File,
        val transitions: File,
        val eventLog: File
    )

    /** Use pre-executed results instead of running transitions to get the traces */
    fun usingExistingTraces(_traceFiles: () -> TraceFiles): FlickerBuilder = apply {
        val traceFiles = _traceFiles()
        // Remove all trace monitor and use only monitor that read from existing trace file
        this.traceMonitors.clear()
        addMonitor(NoTraceMonitor { it.addTraceResult(TraceType.WM, traceFiles.wmTrace) })
        addMonitor(NoTraceMonitor { it.addTraceResult(TraceType.SF, traceFiles.layersTrace) })
        addMonitor(
            NoTraceMonitor { it.addTraceResult(TraceType.TRANSACTION, traceFiles.transactions) }
        )
        addMonitor(
            NoTraceMonitor { it.addTraceResult(TraceType.TRANSITION, traceFiles.transitions) }
        )
        addMonitor(NoTraceMonitor { it.addTraceResult(TraceType.EVENT_LOG, traceFiles.eventLog) })

        // Remove all transitions execution
        this.transitionCommands.clear()
        this.usingExistingTraces = true
    }

    /** Creates a new Flicker runner based on the current builder configuration */
    fun build(): IFlickerTestData {
        return FlickerTestData(
            instrumentation,
            device,
            outputDir,
            traceMonitors,
            setupCommands,
            transitionCommands,
            teardownCommands,
            wmHelper
        )
    }

    /** Returns a copy of the current builder with the changes of [block] applied */
    fun copy(block: FlickerBuilder.() -> Unit) =
        FlickerBuilder(
                instrumentation,
                outputDir.absoluteFile,
                wmHelper,
                setupCommands.toMutableList(),
                transitionCommands.toMutableList(),
                teardownCommands.toMutableList(),
                device,
                traceMonitors.toMutableList(),
            )
            .apply(block)

    private fun addMonitor(newMonitor: ITransitionMonitor?) {
        require(!usingExistingTraces) { "Can't add monitors after calling usingExistingTraces" }

        if (newMonitor != null) {
            traceMonitors.add(newMonitor)
        }
    }
}
