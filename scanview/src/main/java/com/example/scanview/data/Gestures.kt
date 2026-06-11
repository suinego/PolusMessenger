package com.example.scanview.data

data class TouchEvent(
    val action: String,
    val x: Float,
    val y: Float,
    val localX: Float = 0f,
    val localY: Float = 0f,
    val timestamp: Long
)

enum class GestureType {
    TAP,
    SWIPE,
    MOVE,
    LONG_PRESS
}

data class Gesture(
    val type: GestureType,
    val startEvent: TouchEvent,
    val endEvent: TouchEvent?,
    val moveEvents: List<TouchEvent> = emptyList()
) {
    fun getDistance(): Float {
        val end = endEvent ?: return 0f
        val dx = end.x - startEvent.x
        val dy = end.y - startEvent.y
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }
    fun getDuration(): Long {
        val end = endEvent ?: return 0L
        return end.timestamp - startEvent.timestamp
    }
}
