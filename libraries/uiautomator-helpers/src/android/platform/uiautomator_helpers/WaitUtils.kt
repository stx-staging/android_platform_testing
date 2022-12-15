package android.platform.uiautomator_helpers

import android.os.SystemClock.sleep
import android.os.SystemClock.uptimeMillis
import android.os.Trace
import android.util.Log
import java.time.Duration

/**
 * Collection of utilities to ensure a certain conditions is met.
 *
 * Those are meant to make tests more understandable from perfetto traces, and less flaky.
 */
object WaitUtils {
    private val DEFAULT_WAIT = Duration.ofSeconds(10)
    private val POLLING_WAIT = Duration.ofMillis(100)
    private const val TAG = "WaitUtils"
    private const val VERBOSE = true

    /**
     * Ensures that [condition] succeeds within [timeout], or fails with [errorProvider] message.
     *
     * This also logs with atrace each iteration, and its entire execution. Those traces are then
     * visible in perfetto.
     *
     * Example of usage:
     *
     * ```
     * ensureThat("screen is on") { uiDevice.isScreenOn }
     * ```
     */
    fun ensureThat(
        description: String? = null,
        timeout: Duration = DEFAULT_WAIT,
        errorProvider: () -> String = {
            "Error ensuring that \"$description\" within ${timeout.toMillis()}ms"
        },
        traceName: String = description?.let { "Ensuring $it" } ?: "ensure",
        condition: () -> Boolean,
    ) {
        trace(traceName) {
            val startTime = uptimeMillis()
            val timeoutMs = timeout.toMillis()
            val debugLogs = mutableListOf(traceName)
            fun printLogsIfNeeded() {
                if (VERBOSE) {
                    Log.d(TAG, debugLogs.joinToString("\n"))
                }
            }

            fun log(s: String) {
                debugLogs.add("+${uptimeMillis() - startTime}ms $s")
            }

            var i = 1
            while (uptimeMillis() < startTime + timeoutMs) {
                trace("iteration $i") {
                    try {
                        if (condition()) {
                            log("[#$i] Condition true")
                            printLogsIfNeeded()
                            return
                        }
                    } catch (t: Throwable) {
                        log("[#$i] Condition failing with exception")
                        printLogsIfNeeded()
                        throw RuntimeException("[#$i] iteration failed.", t)
                    }

                    log("[#$i] Condition false, might retry.")
                    sleep(POLLING_WAIT.toMillis())
                    i++
                }
            }
            printLogsIfNeeded()
            error(errorProvider())
        }
    }

    private inline fun <T> trace(sectionName: String, block: () -> T): T {
        Trace.beginSection(sectionName)
        try {
            return block()
        } finally {
            Trace.endSection()
        }
    }
}
