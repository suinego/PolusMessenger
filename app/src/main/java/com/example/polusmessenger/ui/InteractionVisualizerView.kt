package com.example.polusmessenger.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import com.example.scanview.InteractionData
import androidx.core.graphics.withTranslation

class InteractionVisualizerView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 28f * resources.displayMetrics.density
    }
    
    private val coordinatePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.CYAN
        textSize = 18f * resources.displayMetrics.density
    }

    private var interactions: List<InteractionData> = emptyList()
    private var screenWidth: Int = 1
    private var screenHeight: Int = 1

    private var offsetX = 0f
    private var offsetY = 0f

    fun loadInteractions(interactionsList: List<InteractionData>, screenW: Int, screenH: Int) {
        interactions = interactionsList
        screenWidth = if (screenW > 0) screenW else 1
        screenHeight = if (screenH > 0) screenH else 1
        offsetX = 0f; offsetY = 0f
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

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.withTranslation(offsetX, offsetY) {
            if (interactions.isEmpty()) return@withTranslation

            val scaleX = width.toFloat() / screenWidth
            val scaleY = height.toFloat() / screenHeight

            interactions.forEachIndexed { index, it ->
                drawInteraction(this, it, index, scaleX, scaleY)
            }
        }
    }

    private fun drawInteraction(
        canvas: Canvas,
        interaction: InteractionData,
        index: Int,
        scaleX: Float,
        scaleY: Float
    ) {
        val color = when (interaction.action) {
            "ACTION_DOWN" -> Color.GREEN
            "ACTION_UP" -> Color.CYAN
            "ACTION_MOVE" -> Color.YELLOW
            else -> Color.LTGRAY
        }
        paint.color = color
        fillPaint.color = color

        val bounds = if (interaction.viewBoundsLeft != null &&
            interaction.viewBoundsTop != null &&
            interaction.viewBoundsRight != null &&
            interaction.viewBoundsBottom != null
        ) {
            Rect(
                (interaction.viewBoundsLeft!! * scaleX).toInt(),
                (interaction.viewBoundsTop!! * scaleY).toInt(),
                (interaction.viewBoundsRight!! * scaleX).toInt(),
                (interaction.viewBoundsBottom!! * scaleY).toInt()
            )
        } else null

        if (bounds != null) {
            canvas.drawRect(bounds, paint)
            val label = "${index + 1}: ${interaction.viewClassName} ${interaction.viewIdName ?: ""}"
            canvas.drawText(label, bounds.left + 8f, bounds.top + 28f, textPaint)
            
            // Подпись координат bounds
            val boundsText = "[${interaction.viewBoundsLeft}, ${interaction.viewBoundsTop}]"
            coordinatePaint.color = Color.CYAN
            canvas.drawText(boundsText, bounds.left + 8f, bounds.top + 50f, coordinatePaint)
        }

        val px = interaction.screenX * scaleX
        val py = interaction.screenY * scaleY

        canvas.drawCircle(px, py, 12f, fillPaint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        canvas.drawCircle(px, py, 12f, paint)
        paint.style = Paint.Style.STROKE

        canvas.drawText("${index + 1}", px + 16f, py - 8f, textPaint)
        
        val touchText = "(${String.format("%.0f", interaction.screenX)}, ${String.format("%.0f", interaction.screenY)})"
        coordinatePaint.color = Color.WHITE
        canvas.drawText(touchText, px + 16f, py + 20f, coordinatePaint)
        
        val localText = "local: (${String.format("%.1f", interaction.localX)}, ${String.format("%.1f", interaction.localY)})"
        coordinatePaint.color = Color.YELLOW
        canvas.drawText(localText, px + 16f, py + 40f, coordinatePaint)
    }
}
