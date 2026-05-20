package com.example.polusmessenger.experiments

import android.os.Handler
import android.os.Looper
import android.view.View
import com.example.scanview.api.ScanViewManager
import com.google.gson.GsonBuilder

/**
 * S2 — стратегия периодического обхода ViewNode-дерева.
 * Раз в 500мс (2fps) захватывает полное ViewNode-дерево корневого View
 * и сериализует в JSON для измерения M4.
 */
class PeriodicViewNodeRecorder(
    private val rootViewProvider: () -> View?,
    private val manager: ScanViewManager,
    private val fps: Int = 2,
    private val maxDepth: Int = 3
) {
    private val handler = Handler(Looper.getMainLooper())
    private val gson = GsonBuilder().setPrettyPrinting().create()
    private var running = false
    private var startTimeMs = 0L

    private var captureCount = 0
    private var totalJsonBytes = 0L

    data class S2Metrics(
        val captureCount: Int,
        val durationMs: Long,
        val avgJsonKbPerCapture: Double,
        val totalJsonKB: Double,
        val kbPerMin: Double
    )

    fun start() {
        if (running) return
        running = true
        startTimeMs = System.currentTimeMillis()
        captureCount = 0; totalJsonBytes = 0L
        scheduleCapture()
    }

    fun stop(): S2Metrics {
        running = false
        handler.removeCallbacksAndMessages(null)
        val durationMs = System.currentTimeMillis() - startTimeMs
        val durationMin = durationMs / 60_000.0
        return S2Metrics(
            captureCount = captureCount,
            durationMs = durationMs,
            avgJsonKbPerCapture = if (captureCount > 0) totalJsonBytes / 1024.0 / captureCount else 0.0,
            totalJsonKB = totalJsonBytes / 1024.0,
            kbPerMin = if (durationMin > 0) (totalJsonBytes / 1024.0) / durationMin else 0.0
        )
    }

    private fun scheduleCapture() {
        if (!running) return
        handler.postDelayed({ captureFrame(); scheduleCapture() }, 1000L / fps)
    }

    private fun captureFrame() {
        val view = rootViewProvider() ?: return
        val node = manager.captureViewNode(view, maxDepth) ?: return
        val json = gson.toJson(node)
        totalJsonBytes += json.toByteArray(Charsets.UTF_8).size.toLong()
        captureCount++
    }
}
