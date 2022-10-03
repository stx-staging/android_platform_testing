package com.android.server.wm.flicker.assertiongenerator.common

import com.android.server.wm.flicker.assertiongenerator.DeviceTraceConfiguration
import com.android.server.wm.flicker.assertiongenerator.layers.LayersLifecycleExtractor
import com.android.server.wm.traces.common.DeviceTraceDump

class LifecycleExtractorFactory {
    companion object {
        private val extractors: Set<ILifecycleExtractor> = setOf(
            LayersLifecycleExtractor()
        )

        fun extract(
            traceDumps: Array<DeviceTraceDump>,
            traceConfigurations: Array<DeviceTraceConfiguration>
        ): List<TraceContent> {
            return traceDumps.flatMapIndexed { index, traceDump ->
                extractors.map { extractor ->
                    val traceLifecycle = extractor.extract(traceDump)
                    val deviceTraceConfiguration = traceConfigurations[index]
                    val traceContent = traceLifecycle?.let {
                            TraceContent.byTraceType(traceLifecycle, deviceTraceConfiguration)
                        }
                    traceContent
                }
            }.filterNotNull()
        }
    }
}