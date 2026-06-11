package com.example.scanview.visualization

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import com.example.scanview.data.BackgroundState
import com.example.scanview.data.ViewContent
import com.example.scanview.data.ViewNode

internal object ViewNodeRenderer {

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        textAlign = Paint.Align.CENTER
    }

    fun draw(canvas: Canvas, node: ViewNode, borderColor: Int = Color.argb(200, 0, 191, 255), depth: Int = 0) {
        if (node.visibility != 0) return

        val rect = RectF(
            node.bounds.left.toFloat(),
            node.bounds.top.toFloat(),
            node.bounds.right.toFloat(),
            node.bounds.bottom.toFloat()
        )
        if (rect.width() < 1f || rect.height() < 1f) return

        drawBackground(canvas, node.background, rect, depth)
        drawContent(canvas, node.content, rect, depth)
        drawBorder(canvas, rect, borderColor, depth)

        node.children.forEach { child -> draw(canvas, child, borderColor, depth + 1) }
    }

    private fun drawBackground(canvas: Canvas, background: BackgroundState?, rect: RectF, depth: Int) {
        val baseAlpha = (200 - depth * 35).coerceIn(30, 200)
        fillPaint.color = when (background) {
            is BackgroundState.Color -> {
                val c = background.color
                Color.argb(baseAlpha, Color.red(c), Color.green(c), Color.blue(c))
            }
            is BackgroundState.Unknown -> Color.argb(baseAlpha, 70, 70, 80)
            null -> Color.argb(baseAlpha / 2, 40, 40, 55)
        }
        val r = if (depth == 0) 12f else 6f
        canvas.drawRoundRect(rect, r, r, fillPaint)
    }

    private fun drawBorder(canvas: Canvas, rect: RectF, color: Int, depth: Int) {
        val alpha = (220 - depth * 40).coerceIn(50, 220)
        val strokeW = if (depth == 0) 3f else maxOf(1f, 2.5f - depth * 0.4f)
        strokePaint.color = Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))
        strokePaint.strokeWidth = strokeW
        val r = if (depth == 0) 12f else 6f
        canvas.drawRoundRect(rect, r, r, strokePaint)
    }

    private fun drawContent(canvas: Canvas, content: ViewContent?, rect: RectF, depth: Int) {
        content ?: return
        // Текст и иконки видны только до определённой глубины
        if (depth > 3) return
        when (content) {
            is ViewContent.Text -> drawText(canvas, content, rect)
            is ViewContent.ImagePlaceholder -> drawImagePlaceholder(canvas, content, rect)
        }
    }

    private fun drawText(canvas: Canvas, content: ViewContent.Text, rect: RectF) {
        if (rect.width() < 20f || rect.height() < 14f) return
        val maxTextSize = rect.height() * 0.45f
        textPaint.textSize = minOf(content.textSizePx, maxTextSize).coerceIn(11f, 40f)
        textPaint.color = content.textColor
        textPaint.typeface = if (content.isBold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT

        val cx = rect.centerX()
        val cy = rect.centerY() - (textPaint.descent() + textPaint.ascent()) / 2f

        val available = rect.width() - 10f
        canvas.drawText(fitText(content.text, available), cx, cy, textPaint)
    }

    private fun drawImagePlaceholder(canvas: Canvas, content: ViewContent.ImagePlaceholder, rect: RectF) {
        strokePaint.color = content.tint ?: Color.argb(140, 150, 150, 160)
        strokePaint.strokeWidth = 2f
        canvas.drawLine(rect.left + 4, rect.top + 4, rect.right - 4, rect.bottom - 4, strokePaint)
        canvas.drawLine(rect.right - 4, rect.top + 4, rect.left + 4, rect.bottom - 4, strokePaint)
    }

    private fun fitText(text: String, availableWidth: Float): String {
        if (text.isEmpty()) return text
        if (textPaint.measureText(text) <= availableWidth) return text
        var cut = text.length - 1
        while (cut > 0 && textPaint.measureText(text.substring(0, cut) + "…") > availableWidth) {
            cut--
        }
        return text.substring(0, cut) + "…"
    }
}
