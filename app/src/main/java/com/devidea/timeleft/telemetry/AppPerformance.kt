package com.devidea.timeleft.telemetry

import com.google.firebase.perf.FirebasePerformance
import com.google.firebase.perf.metrics.Trace
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Closed trace names and outcomes: never accept schedule content, dates, IDs or arbitrary labels. */
object AppPerformance {
    private val initializationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val initializationStarted = AtomicBoolean(false)

    enum class Outcome(internal val value: String) {
        Success("success"),
        Fallback("fallback"),
        Error("error"),
        Cancelled("cancelled"),
    }

    /** Collection setup must not delay Application.onCreate or wait for a network response. */
    fun initialize() {
        if (!initializationStarted.compareAndSet(false, true)) return
        initializationScope.launch {
            sdkOrNull { FirebasePerformance.getInstance().setPerformanceCollectionEnabled(true) }
        }
    }

    /** Subscription to the first prepared home data state, not Compose drawing or first frame. */
    fun startHomeDataLoad(): TraceHandle = start("home_data_load")

    /** Repository lookup/calculation through RemoteViews submission, not launcher drawing. */
    fun startWidgetUpdate(): TraceHandle = start("widget_update_submit")

    private fun start(name: String): TraceHandle = TraceHandle(
        sdkOrNull { FirebasePerformance.getInstance().newTrace(name).also { it.start() } }
    )

    class TraceHandle internal constructor(private var trace: Trace?) {
        private val finished = AtomicBoolean(false)

        /** Safe to call from normal completion and finally; the first outcome wins. */
        fun finish(outcome: Outcome) {
            if (!finished.compareAndSet(false, true)) return
            val activeTrace = trace ?: return
            trace = null
            try {
                sdkOrNull { activeTrace.putAttribute("outcome", outcome.value) }
            } finally {
                sdkOrNull { activeTrace.stop() }
            }
        }
    }

    // Only SDK operations belong here. Product work executes outside this exception boundary.
    // In local unit tests FirebaseApp can be absent; instrumentation then becomes a no-op.
    private inline fun <T> sdkOrNull(action: () -> T): T? = try {
        action()
    } catch (error: CancellationException) {
        throw error
    } catch (_: Exception) {
        null
    }
}
