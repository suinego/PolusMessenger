package com.example.scanview.data

import android.graphics.Rect

//общие модели данных

//минимальная информация о View тронул пользователь


data class ViewInfo(
    val className: String,
    val id: Int?,
    val idName: String?,
    val bounds: Rect?,
    val text: String?,  // для TextView/Button
    val viewNode: ViewNode? = null
)

//запись
data class InteractionRecord(
    val screenName: String, // Название экрана (Activity)
    val viewInfo: ViewInfo,
    val gesture: Gesture,
    val timestamp: Long
)
//статистика
data class InteractionStatistics(
    val totalInteractions: Int,
    val taps: Int,
    val swipes: Int,
    val moves: Int,
    val longPresses: Int,
    val uniqueViews: Int,
    val recordingDuration: Long? = null
)
