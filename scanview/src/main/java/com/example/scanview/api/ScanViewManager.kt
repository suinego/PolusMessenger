package com.example.scanview.api

import android.view.View
import com.example.scanview.data.DiagnosticMetrics
import com.example.scanview.data.InteractionRecord
import com.example.scanview.data.InteractionStatistics
import com.example.scanview.data.ViewNode


interface ScanView {
    fun startRecording()
    fun stopRecording()
    fun isRecording(): Boolean
    fun showVisualization()
}

//доп настроки

interface ScanViewSession {
    fun getHistory(): List<InteractionRecord>
    fun clearHistory()
    fun serialize(): String
    fun deserialize(json: String): List<InteractionRecord>
}


interface ScanViewDiagnostics {
    fun getStatistics(): InteractionStatistics
    fun computeDiagnostics(): DiagnosticMetrics
    fun setFrameMetricsData(frameDurationsMs: List<Double>)
}

interface ScanViewInspector {
    fun findViewAt(decorView: View?, x: Int, y: Int): View?
    fun captureViewNode(view: View, depth: Int): ViewNode?
}


interface ScanViewManager : ScanView, ScanViewSession, ScanViewDiagnostics, ScanViewInspector
