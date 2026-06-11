package com.example.polusmessenger.experiment.base

import com.example.polusmessenger.MainActivity
import com.example.polusmessenger.experiments.BitmapCaptureRecorder
import com.example.polusmessenger.experiments.PeriodicViewNodeRecorder

data class StrategyMetrics(
    val dataKB: Double,
    val captureCount: Int,
    val durationMs: Long
)

sealed class RecordingStrategy(val name: String) {

    abstract fun start(activity: MainActivity)
    abstract fun stop(activity: MainActivity): StrategyMetrics

    object Baseline : RecordingStrategy("Baseline") {
        private var startMs = 0L

        override fun start(activity: MainActivity) {
            activity.scanViewManager.stopRecording()
            startMs = System.currentTimeMillis()
        }

        override fun stop(activity: MainActivity): StrategyMetrics = StrategyMetrics(
            dataKB       = 0.0,
            captureCount = 0,
            durationMs   = System.currentTimeMillis() - startMs
        )
    }

    class S1Bitmap(private val fps: Int = 2) : RecordingStrategy("S1_Bitmap") {
        private var recorder: BitmapCaptureRecorder? = null

        override fun start(activity: MainActivity) {
            activity.scanViewManager.stopRecording()
            recorder = BitmapCaptureRecorder(
                rootViewProvider = { activity.window.decorView.rootView },
                fps = fps
            ).also { it.start() }
        }

        override fun stop(activity: MainActivity): StrategyMetrics {
            val m = recorder!!.stop().also { recorder = null }
            return StrategyMetrics(
                dataKB       = m.estimatedCompressedKB,
                captureCount = m.captureCount,
                durationMs   = m.durationMs
            )
        }
    }

    class S2ViewNode(private val fps: Int = 2) : RecordingStrategy("S2_ViewNode") {
        private var recorder: PeriodicViewNodeRecorder? = null

        override fun start(activity: MainActivity) {
            activity.scanViewManager.stopRecording()
            recorder = PeriodicViewNodeRecorder(
                rootViewProvider = { activity.window.decorView.rootView },
                manager = activity.scanViewManager,
                fps = fps
            ).also { it.start() }
        }

        override fun stop(activity: MainActivity): StrategyMetrics {
            val m = recorder!!.stop().also { recorder = null }
            return StrategyMetrics(
                dataKB       = m.totalJsonKB,
                captureCount = m.captureCount,
                durationMs   = m.durationMs
            )
        }
    }

    object S3ScanView : RecordingStrategy("S3_ScanView") {
        private var startMs = 0L

        override fun start(activity: MainActivity) {
            activity.scanViewManager.apply {
                stopRecording()
                clearHistory()
                startMs = System.currentTimeMillis()
                startRecording()
            }
        }

        override fun stop(activity: MainActivity): StrategyMetrics {
            val manager = activity.scanViewManager
            manager.stopRecording()
            val history = manager.getHistory()
            val dataKB = manager.serialize().toByteArray(Charsets.UTF_8).size / 1024.0
            return StrategyMetrics(
                dataKB       = dataKB,
                captureCount = history.size,
                durationMs   = System.currentTimeMillis() - startMs
            )
        }
    }
}
