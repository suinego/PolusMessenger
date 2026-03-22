package com.example.scanview.visualization

import android.graphics.*
import android.graphics.drawable.Drawable
import com.example.scanview.data.*


data class VisualizationConfig(
    val viewBoundsColor: Int = Color.parseColor("#00BFFF"),
    val viewBoundsAlpha: Int = 150,
    val viewBoundsStrokeWidth: Float = 10f,
    val tapStartColor: Int = Color.parseColor("#00FF00"),
    val tapEndColor: Int = Color.parseColor("#FF0000"),
    val tapIntermediateColor: Int = Color.parseColor("#FFFF00"),
    val tapPointRadius: Float = 20f,
    val showIntermediatePoints: Boolean = true,
    val swipeLineColor: Int = Color.parseColor("#FF6B6B"),
    val swipeLineWidth: Float = 4f,
    val arrowLength: Float = 20f,
    val showLabels: Boolean = true,
    val textSize: Float = 24f,
    val textColor: Int = Color.WHITE,
    val labelBackgroundColor: Int = Color.parseColor("#80000000"),
    val labelPadding: Int = 8
)

class ScanViewDrawable(
    private val interactions: List<InteractionRecord>,
    private val config: VisualizationConfig = VisualizationConfig()
) : Drawable() {
    private var progress: Float = 0f

    private val minTime: Long
    private val maxTime: Long
    private val totalDuration: Long

    init {
        val allTimestamps = interactions.flatMap { record ->
            buildList {
                add(record.gesture.startEvent.timestamp)
                addAll(record.gesture.posledovatelnostMoveEvent.map { it.timestamp })
                record.gesture.endEvent?.let { add(it.timestamp) }
            }
        }
        minTime = allTimestamps.minOrNull() ?: 0L
        maxTime = allTimestamps.maxOrNull() ?: 0L
        totalDuration = maxTime - minTime
    }

    fun setProgress(value: Float) {
        progress = value.coerceIn(0f, 1f)
        invalidateSelf()
    }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    override fun draw(canvas: Canvas) {
        if (interactions.isEmpty()) return

        val currentTime = if (totalDuration > 0L) {
            minTime + (progress * totalDuration).toLong()
        } else {
            if (progress >= 1f) maxTime else minTime - 1L
        }

        interactions.forEachIndexed { index, record ->
            if (record.gesture.startEvent.timestamp <= currentTime) {
                drawInteractionAtTime(canvas, record, index, currentTime)
            }
        }
    }

    private fun drawInteractionAtTime(canvas: Canvas, record: InteractionRecord, index: Int, currentTime: Long) {
        val viewInfo = record.viewInfo
        val gesture = record.gesture

        viewInfo.bounds?.let { drawViewBounds(canvas, it, viewInfo, index) }

        drawTouchPoint(canvas, gesture.startEvent, config.tapStartColor, config.tapPointRadius)

        if (config.showIntermediatePoints) {
            gesture.posledovatelnostMoveEvent
                .filter { it.timestamp <= currentTime }
                .forEach { event ->
                    drawTouchPoint(canvas, event, config.tapIntermediateColor, config.tapPointRadius * 0.6f)
                }
        }

        gesture.endEvent?.let { endEvent ->
            if (endEvent.timestamp <= currentTime) {
                drawTouchPoint(canvas, endEvent, config.tapEndColor, config.tapPointRadius)
                if (gesture.type == GestureType.SWIPE) drawSwipeLine(canvas, gesture)
            }
        }
    }
    private fun drawViewBounds(canvas: Canvas, bounds: Rect, @Suppress("UNUSED_PARAMETER") viewInfo: ViewInfo, @Suppress("UNUSED_PARAMETER") index: Int) {
        val rectF = RectF(bounds)

        paint.color = config.viewBoundsColor
        paint.style = Paint.Style.FILL
        paint.alpha = config.viewBoundsAlpha
        canvas.drawRect(rectF, paint)

        paint.color = Color.MAGENTA
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 6f
        paint.alpha = 255
        canvas.drawRect(rectF, paint)
    }

    private fun drawTouchPoint(
        canvas: Canvas,
        event: TouchEvent,
        color: Int,
        radius: Float
    ) {
        val x = event.x
        val y = event.y

        paint.color = Color.WHITE
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 4f
        canvas.drawCircle(x, y, radius + 4, paint)

        paint.color = color
        paint.style = Paint.Style.FILL
        canvas.drawCircle(x, y, radius, paint)

    }

    private fun drawSwipeLine(canvas: Canvas, gesture: Gesture) {
        val endEvent = gesture.endEvent ?: return
        paint.color = config.swipeLineColor
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = config.swipeLineWidth
        paint.strokeCap = Paint.Cap.ROUND
        paint.pathEffect = DashPathEffect(floatArrayOf(10f, 5f), 0f)

        canvas.drawLine(
            gesture.startEvent.x,
            gesture.startEvent.y,
            endEvent.x,
            endEvent.y,
            paint
        )

        drawArrow(canvas, gesture.startEvent, endEvent)
    }

    private fun drawArrow(canvas: Canvas, start: TouchEvent, end: TouchEvent) {
        val angle = kotlin.math.atan2(
            end.y - start.y,
            end.x - start.x
        )

        val arrowLength = config.arrowLength
        val arrowAngle = kotlin.math.PI / 6

        val x1 = end.x - arrowLength * kotlin.math.cos(angle - arrowAngle)
        val y1 = end.y - arrowLength * kotlin.math.sin(angle - arrowAngle)
        val x2 = end.x - arrowLength * kotlin.math.cos(angle + arrowAngle)
        val y2 = end.y - arrowLength * kotlin.math.sin(angle + arrowAngle)

        paint.color = config.swipeLineColor
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = config.swipeLineWidth
        paint.strokeCap = Paint.Cap.ROUND

        canvas.drawLine(end.x, end.y, x1.toFloat(), y1.toFloat(), paint)
        canvas.drawLine(end.x, end.y, x2.toFloat(), y2.toFloat(), paint)
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
        invalidateSelf()
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
        invalidateSelf()
    }


    override fun getOpacity(): Int {
        return PixelFormat.TRANSLUCENT
    }

    override fun getIntrinsicWidth(): Int {
        return interactions.maxOfOrNull { record ->
            record.viewInfo.bounds?.right ?: 0
        } ?: 0
    }

    override fun getIntrinsicHeight(): Int {
        return interactions.maxOfOrNull { record ->
            record.viewInfo.bounds?.bottom ?: 0
        } ?: 0
    }
}
