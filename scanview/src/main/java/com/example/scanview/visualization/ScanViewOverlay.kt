package com.example.scanview.visualization

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import com.example.scanview.data.InteractionRecord

class ScanViewOverlay @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    init {
        isClickable = false
        isFocusable = false
    }

    private var drawable: ScanViewDrawable? = null
    private var onDismiss: (() -> Unit)? = null

    fun setOnDismissListener(listener: (() -> Unit)?) {
        onDismiss = listener
        isClickable = listener != null
        isFocusable = listener != null
    }
    private val backgroundPaint = Paint().apply {
        color = 0x00000000
        style = Paint.Style.FILL
    }

    fun setInteractions(interactions: List<InteractionRecord>, config: VisualizationConfig = VisualizationConfig()) {
        drawable = ScanViewDrawable(interactions, config)
        invalidate()
    }


    fun clear() {
        drawable = null
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)
        
        drawable?.let { drawable ->
            drawable.setBounds(0, 0, width, height)
            drawable.draw(canvas)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_UP && onDismiss != null) {
            onDismiss?.invoke()
            return true
        }
        return super.onTouchEvent(event)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val parent = parent as? ViewGroup
        if (parent != null) {
            setMeasuredDimension(parent.width, parent.height)
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        }
    }
}
