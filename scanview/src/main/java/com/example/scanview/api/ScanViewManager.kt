package com.example.scanview.api

import android.view.View
import com.example.scanview.data.DiagnosticMetrics
import com.example.scanview.data.InteractionRecord
import com.example.scanview.data.InteractionStatistics
interface ScanViewManager {
    fun startRecording() // начало записи
    fun stopRecording() //стоп
    fun getHistory(): List<InteractionRecord>
    fun getStatistics(): InteractionStatistics
    fun serialize(): String
    fun clearHistory()
    fun isRecording(): Boolean
    fun deserialize(json: String): List<InteractionRecord>
    fun findViewAt(decorView: View?, toInt: Int, toInt2: Int): View?

    /** Вычисляет Q1 и Q2 на основе накопленной истории. */
    fun computeDiagnostics(): DiagnosticMetrics

    /** Устанавливает внешние данные FrameMetrics (M3) в диагностику. */
    fun setFrameMetricsData(frameDurationsMs: List<Double>)

    /**
     * Захватывает ViewNode дерева переданного View.
     * Используется S2-рекордером для периодического обхода без Window.Callback.
     */
    fun captureViewNode(view: View, depth: Int): com.example.scanview.data.ViewNode?
}
