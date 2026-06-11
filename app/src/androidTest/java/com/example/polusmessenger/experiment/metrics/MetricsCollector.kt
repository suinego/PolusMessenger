package com.example.polusmessenger.experiment.metrics

import android.os.Handler
import android.os.Looper
import android.view.FrameMetrics
import android.view.Window
import java.util.Collections

class MetricsCollector(private val window: Window) {

    private val frameDurations: MutableList<Double> =
        Collections.synchronizedList(mutableListOf())

    private val listener = Window.OnFrameMetricsAvailableListener { _, metrics, _ ->
        frameDurations.add(metrics.getMetric(FrameMetrics.TOTAL_DURATION) / 1_000_000.0)
    }

    fun start() {
        frameDurations.clear()
        window.addOnFrameMetricsAvailableListener(
            listener,
            Handler(Looper.getMainLooper())
        )
    }

    fun stop(): FrameResult {
        window.removeOnFrameMetricsAvailableListener(listener)
        val snapshot: List<Double>
        synchronized(frameDurations) { snapshot = frameDurations.toList() }
        val sorted = snapshot.sorted()
        return FrameResult(
            avgFrameMs = if (sorted.isEmpty()) 0.0 else sorted.average(),
            p95FrameMs = if (sorted.isEmpty()) 0.0
                         else sorted[(sorted.size * 0.95).toInt().coerceAtMost(sorted.lastIndex)],
            jankCount  = sorted.count { it > 16.6 },
            totalFrames = sorted.size
        )
    }

    data class FrameResult(
        val avgFrameMs: Double,
        val p95FrameMs: Double,
        val jankCount: Int,
        val totalFrames: Int
    )
}
