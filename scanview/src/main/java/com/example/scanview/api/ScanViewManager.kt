package com.example.scanview.api

import android.view.View
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
    fun findViewAt(decorView: View?, toInt: Int, toInt2: Int): View?
}
