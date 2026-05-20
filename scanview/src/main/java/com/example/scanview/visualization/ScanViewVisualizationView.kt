package com.example.scanview.visualization

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import com.example.scanview.data.InteractionRecord
import java.util.Locale

class ScanViewVisualizationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var drawable: ScanViewDrawable? = null
    private var interactions: List<InteractionRecord> = emptyList()
    private var screenWidth: Int = 1
    private var screenHeight: Int = 1

    private val dp = context.resources.displayMetrics.density

    // ── Overlay paints ────────────────────────────────────────────────────────

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(225, 6, 6, 20)
        style = Paint.Style.FILL
    }
    private val tp = Paint(Paint.ANTI_ALIAS_FLAG)
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

    // ── Public API ────────────────────────────────────────────────────────────

    fun setInteractions(list: List<InteractionRecord>, screenW: Int, screenH: Int,
                        config: VisualizationConfig = VisualizationConfig()) {
        interactions = list
        screenWidth = if (screenW > 0) screenW else 1
        screenHeight = if (screenH > 0) screenH else 1
        drawable = ScanViewDrawable(list, config)
        invalidate()
    }

    fun setProgress(progress: Float) {
        drawable?.setProgress(progress)
        invalidate()
    }

    fun goToStep(index: Int, total: Int) {
        if (total <= 1) return
        setProgress(index.toFloat() / (total - 1).toFloat())
    }

    fun centerOn(absX: Float, absY: Float) { invalidate() }

    // ── Drawing ───────────────────────────────────────────────────────────────

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.BLACK)

        val d = drawable ?: return
        val scaleX = width.toFloat() / screenWidth
        val scaleY = height.toFloat() / screenHeight
        val scale = minOf(scaleX, scaleY)

        // Контент в масштабированных координатах
        canvas.save()
        canvas.scale(scale, scale)
        d.draw(canvas)
        canvas.restore()

        // Overlay — всегда в view-координатах
        val record = d.getCurrentRecord() ?: return
        val idx = d.getCurrentIndex()
        val color = d.gestureColorPublic(record.gesture.type)

        drawProgressIndicator(canvas, idx, d.getTotalCount())
        drawInfoPanel(canvas, record, idx, color)
    }

    // ── Прогресс-индикатор  ─────────────────────────

    private fun drawProgressIndicator(canvas: Canvas, idx: Int, total: Int) {
        val w = width.toFloat()
        if (total <= 0) return

        if (total <= 18) {
            val dotR = 4.5f * dp
            val gap = 9f * dp
            val totalW = total * (dotR * 2 + gap) - gap
            var x = w / 2f - totalW / 2f + dotR
            val y = 20f * dp

            for (i in 0 until total) {
                dotPaint.color = if (i == idx) Color.WHITE else Color.argb(70, 255, 255, 255)
                val r = if (i == idx) dotR * 1.5f else dotR
                canvas.drawCircle(x, y, r, dotPaint)
                x += dotR * 2 + gap
            }
        } else {
            val barH = 3f * dp
            val barY = 14f * dp
            val margin = 20f * dp
            dotPaint.color = Color.argb(50, 255, 255, 255)
            canvas.drawRoundRect(RectF(margin, barY, w - margin, barY + barH), 2f, 2f, dotPaint)
            val prog = (idx + 1f) / total
            dotPaint.color = Color.WHITE
            canvas.drawRoundRect(
                RectF(margin, barY, margin + (w - margin * 2) * prog, barY + barH),
                2f, 2f, dotPaint
            )
        }
    }

    // ── Информационная панель (view-координаты) ────────────────────────────

    private fun drawInfoPanel(canvas: Canvas, rec: InteractionRecord, idx: Int, color: Int) {
        val w = width.toFloat()
        val h = height.toFloat()
        val panelH = 108f * dp
        val panelY = h - panelH
        val cornerR = 18f * dp

        // Фон
        canvas.drawRoundRect(RectF(0f, panelY, w, h), cornerR, cornerR, bgPaint)
        canvas.drawRect(RectF(0f, panelY + cornerR, w, h), bgPaint)

        // Верхняя цветная линия
        tp.color = color; tp.style = Paint.Style.STROKE; tp.strokeWidth = 2f * dp
        canvas.drawLine(24f * dp, panelY + dp, w - 24f * dp, panelY + dp, tp)

        // Экран (зелёный)
        tp.style = Paint.Style.FILL; tp.typeface = Typeface.DEFAULT_BOLD
        tp.textSize = 13f * dp; tp.color = Color.rgb(0, 230, 118); tp.textAlign = Paint.Align.LEFT
        canvas.drawText(rec.screenName.uppercase(Locale.getDefault()), 22f * dp, panelY + 36f * dp, tp)

        // Счётчик (справа)
        tp.color = Color.argb(130, 200, 200, 220)
        tp.textSize = 11f * dp; tp.textAlign = Paint.Align.RIGHT
        canvas.drawText("${idx + 1} / ${interactions.size}", w - 22f * dp, panelY + 36f * dp, tp)

        // Тип жеста + цель
        val viewLabel = rec.viewInfo.idName
            ?: rec.viewInfo.text?.take(22)
            ?: rec.viewInfo.className
        tp.color = color; tp.textSize = 12f * dp
        tp.typeface = Typeface.DEFAULT_BOLD; tp.textAlign = Paint.Align.LEFT
        canvas.drawText("${rec.gesture.type.name}  ·  $viewLabel", 22f * dp, panelY + 68f * dp, tp)

        // Время
        val relSec = (rec.timestamp - (interactions.firstOrNull()?.timestamp ?: rec.timestamp)) / 1000.0
        val timeStr = if (idx == 0) "Начало сессии" else "+%.1f с".format(relSec)
        tp.color = Color.argb(100, 180, 180, 200)
        tp.textSize = 10f * dp; tp.typeface = Typeface.DEFAULT; tp.textAlign = Paint.Align.RIGHT
        canvas.drawText(timeStr, w - 22f * dp, panelY + 96f * dp, tp)
    }
}
