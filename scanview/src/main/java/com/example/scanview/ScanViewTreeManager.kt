package com.example.scanview

import android.view.View

interface ScanViewTreeManager {
    suspend fun scanTree(): ViewNode // вернет корневой viewnode
    fun startIntercepting() // логирование кликов и тд

    fun stopIntercepting()
    fun getInterceptedViews(): List<View>
}
