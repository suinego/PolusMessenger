package com.example.polusmessenger.experiments

import android.util.Log
import android.view.View
import com.example.scanview.api.ScanViewManager

/**
 * Управляет дипломными экспериментами S1/S2.
 * S3 (ScanView) всегда активен через ScanViewManager.
 *
 * Использование:
 *   val exp = ExperimentManager(scanViewManager) { window.decorView }
 *   exp.startS1()   // запустить bitmap-стратегию
 *   // ... пользователь взаимодействует ...
 *   exp.stopAndLog() // остановить и вывести метрики в Logcat
 */
class ExperimentManager(
    private val manager: ScanViewManager,
    private val rootViewProvider: () -> View?
) {
    enum class Strategy { NONE, S1_BITMAP, S2_VIEWNODE }

    private var currentStrategy = Strategy.NONE
    private var s1: BitmapCaptureRecorder? = null
    private var s2: PeriodicViewNodeRecorder? = null

    fun startS1(fps: Int = 2) {
        stopCurrent()
        currentStrategy = Strategy.S1_BITMAP
        s1 = BitmapCaptureRecorder(rootViewProvider, fps).also { it.start() }
        Log.i("Experiment", "▶ S1 Bitmap started (${fps}fps)")
    }

    fun startS2(fps: Int = 2, depth: Int = 3) {
        stopCurrent()
        currentStrategy = Strategy.S2_VIEWNODE
        s2 = PeriodicViewNodeRecorder(rootViewProvider, manager, fps, depth).also { it.start() }
        Log.i("Experiment", "▶ S2 PeriodicViewNode started (${fps}fps, depth=$depth)")
    }

    fun stopAndLog() {
        when (currentStrategy) {
            Strategy.S1_BITMAP -> {
                val m = s1?.stop() ?: return
                Log.i("Experiment/S1", "═══ S1 Bitmap результаты ═══")
                Log.i("Experiment/S1", "Снимков:         ${m.captureCount}")
                Log.i("Experiment/S1", "Длительность:    ${m.durationMs / 1000} с")
                Log.i("Experiment/S1", "Сырые данные:    %.1f МБ (${m.rawBytesPerCapture / 1024} КБ/снимок)".format(m.totalRawMB))
                Log.i("Experiment/S1", "PNG сжатие:      %.1f КБ всего".format(m.estimatedCompressedKB))
                Log.i("Experiment/S1", "M4 эквивалент:   %.1f КБ/мин".format(m.kbPerMin))
                s1 = null
            }
            Strategy.S2_VIEWNODE -> {
                val m = s2?.stop() ?: return
                Log.i("Experiment/S2", "═══ S2 PeriodicViewNode результаты ═══")
                Log.i("Experiment/S2", "Снимков:         ${m.captureCount}")
                Log.i("Experiment/S2", "Длительность:    ${m.durationMs / 1000} с")
                Log.i("Experiment/S2", "Ср. JSON/снимок: %.1f КБ".format(m.avgJsonKbPerCapture))
                Log.i("Experiment/S2", "Итого JSON:      %.1f КБ".format(m.totalJsonKB))
                Log.i("Experiment/S2", "M4 эквивалент:   %.1f КБ/мин".format(m.kbPerMin))
                s2 = null
            }
            Strategy.NONE -> Log.w("Experiment", "Нет активного эксперимента")
        }
        currentStrategy = Strategy.NONE
    }

    private fun stopCurrent() {
        s1?.stop(); s1 = null
        s2?.stop(); s2 = null
        currentStrategy = Strategy.NONE
    }

    val isRunning get() = currentStrategy != Strategy.NONE
    val activeStrategy get() = currentStrategy
}
