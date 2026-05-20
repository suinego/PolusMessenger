package com.example.scanview.visualization

import android.graphics.*
import android.graphics.drawable.Drawable
import com.example.scanview.data.*

/**
 * Пошаговая визуализация — один шаг на экране, без наложений.
 * progress (0..1) → индекс взаимодействия.
 */
class ScanViewDrawable(
    private val interactions: List<InteractionRecord>,
    private val config: VisualizationConfig = VisualizationConfig()
) : Drawable() {

    private var progress: Float = 0f

    fun setProgress(value: Float) {
        progress = value.coerceIn(0f, 1f)
        invalidateSelf()
    }

    private fun currentIndex() =
        if (interactions.isEmpty()) 0
        else (progress * (interactions.lastIndex)).toInt().coerceIn(0, interactions.lastIndex)

    // ── Paints ────────────────────────────────────────────────────────────────

    private val p = Paint(Paint.ANTI_ALIAS_FLAG)

    // ── Цвета по типу жеста ───────────────────────────────────────────────────

    private fun gestureColor(type: GestureType): Int = when (type) {
        GestureType.TAP        -> 0xFF00E676.toInt()  // яркий зелёный
        GestureType.SWIPE      -> 0xFFFF6D00.toInt()  // оранжевый
        GestureType.LONG_PRESS -> 0xFFD500F5.toInt()  // фиолетовый
        GestureType.MOVE       -> 0xFF00B0FF.toInt()  // голубой
    }

    // ── Главный draw ──────────────────────────────────────────────────────────

    override fun draw(canvas: Canvas) {
        if (interactions.isEmpty()) return
        val idx = currentIndex()
        val record = interactions[idx]
        val color = gestureColor(record.gesture.type)

        // 1. ViewNode / bounds
        record.viewInfo.viewNode?.let { ViewNodeRenderer.draw(canvas, it, color) }
            ?: record.viewInfo.bounds?.let { drawBoundsRect(canvas, it, color) }

        // 2. Жест
        when (record.gesture.type) {
            GestureType.SWIPE -> drawSwipeTrail(canvas, record.gesture, color)
            GestureType.LONG_PRESS -> drawLongPressRings(canvas, record.gesture.startEvent, color)
            else -> Unit
        }

        // 3. Точка касания
        val tx = record.gesture.startEvent.x
        val ty = record.gesture.startEvent.y
        drawRippleTouchPoint(canvas, tx, ty, color)

        // Прогресс и панель рисуются в ScanViewVisualizationView вне трансформа
    }

    // ── Bounds fallback ───────────────────────────────────────────────────────

    private fun drawBoundsRect(canvas: Canvas, bounds: Rect, color: Int) {
        val rect = RectF(bounds)
        val r = Color.red(color); val g = Color.green(color); val b = Color.blue(color)
        p.color = Color.argb(30, r, g, b)
        p.style = Paint.Style.FILL
        p.pathEffect = null
        canvas.drawRoundRect(rect, 12f, 12f, p)
        p.color = Color.argb(180, r, g, b)
        p.style = Paint.Style.STROKE
        p.strokeWidth = 3f
        canvas.drawRoundRect(rect, 12f, 12f, p)
    }

    // ── Ripple touch point ────────────────────────────────────────────────────

    private fun drawRippleTouchPoint(canvas: Canvas, x: Float, y: Float, color: Int) {
        val r = Color.red(color); val g = Color.green(color); val b = Color.blue(color)
        val radius = config.tapPointRadius
        p.style = Paint.Style.FILL
        p.pathEffect = null
        p.shader = null

        // Ripple кольца (3 затухающих)
        for (i in 3 downTo 1) {
            p.color = Color.argb(18 * i, r, g, b)
            canvas.drawCircle(x, y, radius * (1f + i * 0.9f), p)
        }
        // Белое кольцо
        p.color = Color.argb(200, 255, 255, 255)
        p.style = Paint.Style.STROKE
        p.strokeWidth = 3f
        canvas.drawCircle(x, y, radius + 3f, p)
        // Цветной центр
        p.color = color
        p.style = Paint.Style.FILL
        canvas.drawCircle(x, y, radius, p)
        // Белый центральный блик
        p.color = Color.argb(180, 255, 255, 255)
        canvas.drawCircle(x, y, radius * 0.28f, p)
    }

    // ── Swipe trail — градиентные нарастающие точки ───────────────────────────

    private fun drawSwipeTrail(canvas: Canvas, gesture: Gesture, color: Int) {
        val start = gesture.startEvent
        val end = gesture.endEvent ?: return
        val r = Color.red(color); val g = Color.green(color); val b = Color.blue(color)
        val steps = 20

        p.style = Paint.Style.FILL
        p.shader = null
        p.pathEffect = null

        for (i in 0..steps) {
            val t = i.toFloat() / steps
            val x = start.x + (end.x - start.x) * t
            val y = start.y + (end.y - start.y) * t
            val dotR = 3f + t * 10f        // растёт 3→13px (направление движения)
            val alpha = (40 + t * 215).toInt() // темнеет 40→255
            p.color = Color.argb(alpha, r, g, b)
            canvas.drawCircle(x, y, dotR, p)
        }

        // Стрелка на конце
        drawArrowHead(canvas, start, end, color)

        // Точка конца (менее яркая)
        p.color = Color.argb(160, 255, 255, 255)
        p.style = Paint.Style.STROKE
        p.strokeWidth = 2f
        canvas.drawCircle(end.x, end.y, config.tapPointRadius * 0.7f + 2f, p)
    }

    private fun drawArrowHead(canvas: Canvas, start: TouchEvent, end: TouchEvent, color: Int) {
        val angle = kotlin.math.atan2((end.y - start.y).toDouble(), (end.x - start.x).toDouble())
        val spread = kotlin.math.PI / 5.5
        val len = config.arrowLength * 1.4f
        val r = Color.red(color); val g = Color.green(color); val b = Color.blue(color)

        p.color = Color.argb(230, r, g, b)
        p.style = Paint.Style.STROKE
        p.strokeWidth = config.swipeLineWidth
        p.strokeCap = Paint.Cap.ROUND
        p.shader = null; p.pathEffect = null

        canvas.drawLine(
            end.x, end.y,
            (end.x - len * kotlin.math.cos(angle - spread)).toFloat(),
            (end.y - len * kotlin.math.sin(angle - spread)).toFloat(), p
        )
        canvas.drawLine(
            end.x, end.y,
            (end.x - len * kotlin.math.cos(angle + spread)).toFloat(),
            (end.y - len * kotlin.math.sin(angle + spread)).toFloat(), p
        )
    }

    // ── Long press — пульсирующие кольца ─────────────────────────────────────

    private fun drawLongPressRings(canvas: Canvas, event: TouchEvent, color: Int) {
        val r = Color.red(color); val g = Color.green(color); val b = Color.blue(color)
        p.style = Paint.Style.STROKE
        p.pathEffect = null; p.shader = null

        for (i in 1..4) {
            val ringR = config.tapPointRadius * (1.2f + i * 0.7f)
            p.color = Color.argb((80 - i * 16).coerceAtLeast(10), r, g, b)
            p.strokeWidth = 2.5f - i * 0.4f
            canvas.drawCircle(event.x, event.y, ringR, p)
        }
    }



    // ── Публичный доступ для overlay ──────────────────────────────────────────

    fun getCurrentRecord(): InteractionRecord? = interactions.getOrNull(currentIndex())
    fun getCurrentIndex(): Int = currentIndex()
    fun getTotalCount(): Int = interactions.size
    fun gestureColorPublic(type: GestureType): Int = gestureColor(type)

    // ── Drawable boilerplate ──────────────────────────────────────────────────

    override fun setAlpha(alpha: Int) {}
    override fun setColorFilter(cf: ColorFilter?) {}
    @Suppress("OVERRIDE_DEPRECATION")
    override fun getOpacity() = PixelFormat.TRANSLUCENT
}
