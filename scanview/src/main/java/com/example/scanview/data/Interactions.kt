package com.example.scanview.data

import android.graphics.Rect

data class ViewInfo(
    val className: String,
    val id: Int?,
    val idName: String?,
    val bounds: Rect?,
    val text: String?,
    val viewNode: ViewNode? = null
)

data class InteractionRecord(
    val screenName: String,
    val viewInfo: ViewInfo,
    val gesture: Gesture,
    val timestamp: Long
)
data class InteractionStatistics(
    val totalInteractions: Int,
    val taps: Int,
    val swipes: Int,
    val moves: Int,
    val longPresses: Int,
    val uniqueViews: Int,
    val recordingDuration: Long? = null
)
