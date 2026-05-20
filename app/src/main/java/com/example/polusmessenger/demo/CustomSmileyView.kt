package com.example.polusmessenger.demo

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class CustomSmileyView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val facePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFD700")
        style = Paint.Style.FILL
    }
    private val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#CC9900")
        style = Paint.Style.STROKE
        strokeWidth = 6f
    }
    private val featurePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        style = Paint.Style.FILL
    }
    private val smilePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 6f
        strokeCap = Paint.Cap.ROUND
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val cx = width / 2f
        val cy = height / 2f
        val radius = minOf(cx, cy) * 0.85f

        //лицо
        canvas.drawCircle(cx, cy, radius, facePaint)
        canvas.drawCircle(cx, cy, radius, outlinePaint)

        val eyeRadius = radius * 0.12f
        val eyeOffsetX = radius * 0.3f
        val eyeOffsetY = radius * 0.25f

        //глаза
        canvas.drawCircle(cx - eyeOffsetX, cy - eyeOffsetY, eyeRadius, featurePaint)
        canvas.drawCircle(cx + eyeOffsetX, cy - eyeOffsetY, eyeRadius, featurePaint)

        //улыбка — дуга
        val smileRect = RectF(
            cx - radius * 0.5f,
            cy - radius * 0.1f,
            cx + radius * 0.5f,
            cy + radius * 0.55f
        )
        canvas.drawArc(smileRect, 0f, 180f, false, smilePaint)
    }
}
