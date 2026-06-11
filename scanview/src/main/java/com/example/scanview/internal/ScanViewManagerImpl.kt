package com.example.scanview.internal

import android.content.Intent
import android.graphics.Rect
import android.view.*
import android.widget.TextView
import com.example.scanview.api.ScanViewManager
import com.example.scanview.api.ScanViewManagerDeps
import com.example.scanview.data.*
import com.example.scanview.ui.ScanViewResultsActivity
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import kotlin.math.sqrt

internal class ScanViewManagerImpl(
    private val deps: ScanViewManagerDeps
) : ScanViewManager {
    companion object {
        private const val TAG = "ScanView"
        private const val SWIPE = 100f
        private const val TAP_MAX_DURATION = 300L
        private const val LONG_PRESS_DURATION = 500L
    }

    private val history = mutableListOf<InteractionRecord>()
    private var isRecording = false

    private var externalFrameDurationsMs: List<Double> = emptyList()

    private val eventHandlingTimesUs = mutableListOf<Long>()
    private var recordingStartTime = 0L
    private var currentGesture: MutableList<TouchEvent>? = null
    private var targetView: View? = null

    private var originalCallback: Window.Callback? = null

    private val gson: Gson = GsonBuilder()
        .setPrettyPrinting()
        .create()

    override fun startRecording() {
        if (isRecording) return
        val activity = deps.activityProvider?.invoke() ?: return
        val window = activity.window
        val currentCallback = window.callback ?: return
        originalCallback = currentCallback
        window.callback = RecordingWindowCallback(currentCallback)
        history.clear()
        recordingStartTime = System.currentTimeMillis()
        isRecording = true
        deps.logger?.invoke(TAG, "Запись начата")
    }

    override fun stopRecording(){
        if (!isRecording) return

        deps.activityProvider?.invoke()?.window?.let { window ->
            originalCallback?.let { window.callback = it }
        }
        isRecording = false
        currentGesture = null
        targetView = null
        val duration = System.currentTimeMillis() - recordingStartTime
        deps.logger?.invoke(TAG, "Запись остановлена длительность=$duration, взаимодействия=${history.size}")
    }

    override fun isRecording(): Boolean = isRecording

    override fun showVisualization() {
        ScanViewResultsActivity.setData(history.toList())
        val intent = ScanViewResultsActivity.createIntent(deps.context)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        deps.context.startActivity(intent)
    }

    override fun clearHistory() = history.clear()

    override fun getHistory(): List<InteractionRecord> = history.toList()

    override fun deserialize(json: String): List<InteractionRecord> {
        return runCatching {
            val wrapper = gson.fromJson(json, JsonObject::class.java)
            val interactionsElement = wrapper.get("interactions")
            val type = object : TypeToken<List<InteractionRecordDto>>() {}.type
            val dtos: List<InteractionRecordDto> = gson.fromJson(interactionsElement, type)
            dtos.map { it.toDomain() }
        }.onFailure { e ->
            deps.logger?.invoke(TAG, "Ошибка десериализации: ${e.message}")
        }.getOrElse { emptyList() }
    }

    override fun serialize(): String {
        val data = mapOf(
            "statistics" to getStatistics(),
            "interactions" to history.map { it.toDto() },
            "timestamp" to System.currentTimeMillis()
        )
        return gson.toJson(data)
    }

    override fun findViewAt(view: View?, x: Int, y: Int): View? {
        if (view == null || !view.isShown) return null
        val loc = IntArray(2)
        view.getLocationOnScreen(loc)
        val rect = Rect(loc[0], loc[1], loc[0] + view.width, loc[1] + view.height)
        if (!rect.contains(x, y)) return null

        if (view is ViewGroup) {
            for (i in view.childCount - 1 downTo 0) {
                val child = view.getChildAt(i)
                val found = findViewAt(child, x, y)
                if (found != null) return found
            }
        }
        return view
    }

    private inner class RecordingWindowCallback(
        private val delegate: Window.Callback
    ) : Window.Callback by delegate {

        override fun dispatchTouchEvent(event: MotionEvent): Boolean {
            if (isRecording) {
                val t0 = System.nanoTime()
                handleMotionEvent(event)
                val elapsedUs = (System.nanoTime() - t0) / 1000L
                eventHandlingTimesUs.add(elapsedUs)
            }
            return delegate.dispatchTouchEvent(event)
        }
    }

    override fun captureViewNode(view: View, depth: Int): ViewNode? =
        runCatching { ViewStateExtractor.extract(view, depth) }.getOrNull()

    override fun setFrameMetricsData(frameDurationsMs: List<Double>) {
        externalFrameDurationsMs = frameDurationsMs
    }

    override fun computeDiagnostics(): DiagnosticMetrics {
        val frames = externalFrameDurationsMs
        val avgFrame = if (frames.isEmpty()) 0.0 else frames.average()
        val p95Frame = if (frames.isEmpty()) 0.0 else {
            val sorted = frames.sorted()
            sorted[(sorted.size * 0.95).toInt().coerceAtMost(sorted.lastIndex)]
        }
        val jankCount = frames.count { it > 16.6 }

        val resolved = history.count { it.viewInfo.idName != null }
        val q1Rate = if (history.isEmpty()) 0.0 else resolved.toDouble() / history.size

        val gaps = if (history.size < 2) emptyList()
        else history.zipWithNext { a, b -> b.timestamp - a.timestamp }
        val maxGap = gaps.maxOrNull() ?: 0L
        val avgGap = if (gaps.isEmpty()) 0.0 else gaps.average()

        val avgOverhead = if (eventHandlingTimesUs.isEmpty()) 0.0
                         else eventHandlingTimesUs.average()
        val maxOverhead = eventHandlingTimesUs.maxOrNull() ?: 0L

        return DiagnosticMetrics(
            avgFrameMs = avgFrame,
            p95FrameMs = p95Frame,
            jankFrameCount = jankCount,
            totalFrameCount = frames.size,
            idNameResolutionRate = q1Rate,
            resolvedIdCount = resolved,
            totalInteractions = history.size,
            maxTimestampGapMs = maxGap,
            avgTimestampGapMs = avgGap,
            avgEventHandlingUs = avgOverhead,
            maxEventHandlingUs = maxOverhead
        )
    }

    private fun handleMotionEvent(event: MotionEvent) {
        val action = event.actionMasked
        val timestamp = System.currentTimeMillis()

        when (action) {
            MotionEvent.ACTION_DOWN -> {
                currentGesture = mutableListOf()
                targetView = findViewAt(deps.activityProvider?.invoke()?.window?.decorView, event.rawX.toInt(), event.rawY.toInt())
                addTouchEvent(event, timestamp)
            }
            MotionEvent.ACTION_MOVE -> {
                addTouchEvent(event, timestamp)
            }
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                addTouchEvent(event, timestamp)
                if (!currentGesture.isNullOrEmpty() && targetView != null) {
                    val gesture = createGestureFromEvents(currentGesture!!)
                    recordInteraction(targetView!!, gesture)
                }
                currentGesture = null
                targetView = null
            }
        }
    }

    private fun addTouchEvent(event: MotionEvent, timestamp: Long) {
        currentGesture?.add(
            TouchEvent(
                action = MotionEvent.actionToString(event.actionMasked),
                x = event.rawX,
                y = event.rawY,
                localX = event.x,
                localY = event.y,
                timestamp = timestamp
            )
        )
    }

    private fun createGestureFromEvents(events: List<TouchEvent>): Gesture {
        val start = events.first()
        val end = events.last()
        val duration = end.timestamp - start.timestamp
        val dx = end.x - start.x
        val dy = end.y - start.y
        val distance = sqrt(dx * dx + dy * dy)
        val swipeThreshold = dpToPx(SWIPE)
        val maxTapDistance = dpToPx(20f)
        val type = when {
            duration > LONG_PRESS_DURATION && distance < maxTapDistance ->
                GestureType.LONG_PRESS
            distance > swipeThreshold ->
                GestureType.SWIPE
            duration <= TAP_MAX_DURATION && distance < maxTapDistance ->
                GestureType.TAP
            else -> GestureType.MOVE
        }

        val moveEvents = if (events.size > 2) events.drop(1).dropLast(1) else emptyList()
        val sampledMoveEvents = sampleMoveEvents(moveEvents, maxPoints = deps.config.maxMoveEventSamples)

        return Gesture(
            type = type,
            startEvent = start,
            endEvent = end,
            moveEvents = sampledMoveEvents
        )
    }

    private fun recordInteraction(view: View, gesture: Gesture) {
        val screenName = deps.screenNameProvider?.invoke()
            ?: deps.activityProvider?.invoke()?.javaClass?.simpleName
            ?: "Unknown"

        val record = InteractionRecord(
            screenName = screenName,
            viewInfo = createViewInfo(view),
            gesture = gesture,
            timestamp = gesture.startEvent.timestamp
        )
        history.add(record)
    }

    private fun createViewInfo(view: View): ViewInfo {
        val id = if (view.id != View.NO_ID) view.id else null
        val idName = id?.let {
            runCatching { deps.context.resources.getResourceEntryName(it) }.getOrNull()
        }
        val bounds = getViewBoundsOnScreen(view)
        val text = if (view is TextView) view.text?.toString() else null

        val viewNode = runCatching {
            ViewStateExtractor.extract(view, maxDepth = deps.config.maxViewNodeDepth)
        }.getOrNull()

        return ViewInfo(
            className = view.javaClass.simpleName,
            id = id,
            idName = idName,
            bounds = bounds,
            text = text,
            viewNode = viewNode
        )
    }

    private fun getViewBoundsOnScreen(view: View): Rect? {
        if (!view.isAttachedToWindow) return null
        val loc = IntArray(2)
        view.getLocationOnScreen(loc)
        return Rect(loc[0], loc[1], loc[0] + view.width, loc[1] + view.height)
    }

    private fun sampleMoveEvents(events: List<TouchEvent>, maxPoints: Int): List<TouchEvent> {
        if (events.size <= maxPoints) return events
        val step = events.size.toDouble() / maxPoints
        return (0 until maxPoints).map { i -> events[(i * step).toInt()] }
    }

    private fun dpToPx(dp: Float): Float =
        dp * deps.context.resources.displayMetrics.density

    override fun getStatistics(): InteractionStatistics {
        val stats = history.groupBy { it.gesture.type }
        return InteractionStatistics(
            totalInteractions = history.size,
            taps = stats[GestureType.TAP]?.size ?: 0,
            swipes = stats[GestureType.SWIPE]?.size ?: 0,
            moves = stats[GestureType.MOVE]?.size ?: 0,
            longPresses = stats[GestureType.LONG_PRESS]?.size ?: 0,
            uniqueViews = history.distinctBy { "${it.viewInfo.className}_${it.viewInfo.id}" }.size,
            recordingDuration = if (isRecording) System.currentTimeMillis() - recordingStartTime else null
        )
    }
}
