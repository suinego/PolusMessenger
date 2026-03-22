package com.example.scanview.visualization

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View
import androidx.core.graphics.withTranslation
import com.example.scanview.data.InteractionRecord

class ScanViewVisualizationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var drawable: ScanViewDrawable? = null
    private var interactions: List<InteractionRecord> = emptyList()
    private var screenWidth: Int = 1
    private var screenHeight: Int = 1
    private var offsetX = 0f
    private var offsetY = 0f

    fun setInteractions(interactionsList: List<InteractionRecord>, screenW: Int, screenH: Int, config: VisualizationConfig = VisualizationConfig()) {
        interactions = interactionsList
        screenWidth = if (screenW > 0) screenW else 1
        screenHeight = if (screenH > 0) screenH else 1
        offsetX = 0f
        offsetY = 0f
        drawable = ScanViewDrawable(interactions, config)
        invalidate()
    }

    fun centerOn(absX: Float, absY: Float) {
        if (screenWidth <= 0 || screenHeight <= 0) return
        val scaleX = width.toFloat() / screenWidth
        val scaleY = height.toFloat() / screenHeight

        val sx = absX * scaleX
        val sy = absY * scaleY

        val centerX = width / 2f
        val centerY = height / 2f

        offsetX = centerX - sx
        offsetY = centerY - sy
        invalidate()
    }
    fun setProgress(progress: Float) {
        drawable?.setProgress(progress)
        invalidate()
    }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.drawColor(android.graphics.Color.BLACK)

        drawable?.let { d ->
            canvas.save()
            val scaleX = width.toFloat() / screenWidth
            val scaleY = height.toFloat() / screenHeight
            val scale = minOf(scaleX, scaleY)
            canvas.scale(scale, scale)
            canvas.translate(offsetX / scale, offsetY / scale)

            d.draw(canvas)
            canvas.restore()
        }
    }
}
