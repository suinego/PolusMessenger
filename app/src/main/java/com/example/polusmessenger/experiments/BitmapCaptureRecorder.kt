package com.example.polusmessenger.experiments

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Handler
import android.os.Looper
import android.view.View

class BitmapCaptureRecorder(
    private val rootViewProvider: () -> View?,
    private val fps: Int = 2
) {
    private val handler = Handler(Looper.getMainLooper())
    private var running = false
    private var startTimeMs = 0L

    private var captureCount = 0
    private var totalBitmapBytes = 0L
    private var totalPngCompressedBytes = 0L
    private val frameDurationsMs = mutableListOf<Double>()

    data class S1Metrics(
        val captureCount: Int,
        val durationMs: Long,
        val rawBytesPerCapture: Long,
        val totalRawMB: Double,
        val estimatedCompressedKB: Double,
        val kbPerMin: Double
    )

    fun start() {
        if (running) return
        running = true
        startTimeMs = System.currentTimeMillis()
        captureCount = 0
        totalBitmapBytes = 0L
        totalPngCompressedBytes = 0L
        scheduleCapture()
    }

    fun stop(): S1Metrics {
        running = false
        handler.removeCallbacksAndMessages(null)
        val durationMs = System.currentTimeMillis() - startTimeMs
        val avgRawPerCapture = if (captureCount > 0) totalBitmapBytes / captureCount else 0L
        val durationMin = durationMs / 60_000.0
        val kbPerMin = if (durationMin > 0)
            (totalPngCompressedBytes / 1024.0) / durationMin else 0.0

        return S1Metrics(
            captureCount = captureCount,
            durationMs = durationMs,
            rawBytesPerCapture = avgRawPerCapture,
            totalRawMB = totalBitmapBytes / (1024.0 * 1024.0),
            estimatedCompressedKB = totalPngCompressedBytes / 1024.0,
            kbPerMin = kbPerMin
        )
    }

    private fun scheduleCapture() {
        if (!running) return
        handler.postDelayed({
            val t0 = System.currentTimeMillis()
            captureFrame()
            frameDurationsMs.add((System.currentTimeMillis() - t0).toDouble())
            scheduleCapture()
        }, 1000L / fps)
    }

    private fun captureFrame() {
        val view = rootViewProvider()
        if (view == null || view.width == 0 || view.height == 0) return

        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        try {
            val canvas = Canvas(bitmap)
            view.draw(canvas)

            totalBitmapBytes += bitmap.byteCount.toLong()
            captureCount++

            val stream = CountingOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            totalPngCompressedBytes += stream.bytesWritten
        } finally {
            bitmap.recycle()
        }
    }

    private class CountingOutputStream : java.io.OutputStream() {
        var bytesWritten = 0L
        override fun write(b: Int) { bytesWritten++ }
        override fun write(b: ByteArray, off: Int, len: Int) { bytesWritten += len }
    }
}
