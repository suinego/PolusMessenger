package com.example.scanview

import android.content.Context
import android.graphics.Rect
import android.view.View
import android.view.MotionEvent

interface ScanViewTreeManagerDeps {
    val context: Context
    val rootViewProvider: () -> View? //activity.window.decorView.rootView
    val logger: ((tag: String, msg: String) -> Unit)? //Android Log wrapper

    val onViewInteraction: ((ViewInteractionInfo) -> Unit)?
}

data class ViewInteractionInfo(
    val view: View,
    val viewClassName: String,
    val viewId: Int?,
    val viewIdName: String?,
    val motionEvent: MotionEvent,
    val action: String,
    val localX: Float,
    val localY: Float,
    val screenX: Float,
    val screenY: Float,
    val viewBounds: Rect?,               // основной объект bounds (может быть null)
    val viewWidth: Int,
    val viewHeight: Int,
    val isInsideBounds: Boolean,
    val timestamp: Long = System.currentTimeMillis()
) {
    val viewBoundsLeft: Int? get() = viewBounds?.left
    val viewBoundsTop: Int? get() = viewBounds?.top
    val viewBoundsRight: Int? get() = viewBounds?.right
    val viewBoundsBottom: Int? get() = viewBounds?.bottom
}
