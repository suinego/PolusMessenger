package com.example.scanview.internal

import android.app.Activity
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.view.*
import android.widget.TextView
import com.example.scanview.api.ScanViewManager
import com.example.scanview.api.ScanViewManagerDeps
import com.example.scanview.data.*
import com.example.scanview.internal.toDto
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlin.math.sqrt

internal class ScanViewManagerImpl(
    private val deps: ScanViewManagerDeps
) : ScanViewManager {
    companion object {
        private const val TAG = "ScanView" // в будущем убрать
        private const val SWIPE = 100f
        private const val TAP_MAX_DURATION = 300L
        private const val LONG_PRESS_DURATION = 500L
    }

    private val history = mutableListOf<InteractionRecord>() // список всех записанных взаимодействий
    private var isRecording = false
    private var recordingStartTime = 0L
    private var currentGesture: MutableList<TouchEvent>? = null
    private var inmomentTargetView: View? = null

    private var originalCallback: Window.Callback? = null //переназвать

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
        // Начало записи, если уже записываем то return, иначе запускаем перехват касаний через подмену коллбека окна активити
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
/*Останавливаем запись и восстанавливаем оригинальный коллбек окну*/
    override fun stopRecording(){
            if (!isRecording) return

            deps.activityProvider?.invoke()?.window?.let { window ->
                originalCallback?.let { window.callback = it }
            }
            isRecording = false
            currentGesture = null
            inmomentTargetView = null
            val duration = System.currentTimeMillis() - recordingStartTime
            deps.logger?.invoke(TAG, "зАпись остановлена длительность=$duration, взаимодейтсвия=${history.size}")
    }
    override fun isRecording(): Boolean = isRecording

    override fun clearHistory() = history.clear()

    override fun getHistory(): List<InteractionRecord> = history.toList()

    override fun serialize(): String {
        val data = mapOf(
            "statistics" to getStatistics(),
            "interactions" to history.map { it.toDto() },
            "timestamp" to System.currentTimeMillis()
        )
        return gson.toJson(data)
    }

    /*рекурсивно ищем глубокую View в иерархии по координатам нажатия*/
    override  fun findViewAt(view: View?, x: Int, y: Int): View? {
        if (view == null || !view.isShown) return null
        val loc = IntArray(2)
        view.getLocationOnScreen(loc)
        val rect = Rect(loc[0], loc[1], loc[0] + view.width, loc[1] + view.height)
        if (!rect.contains(x, y)) return null

        if (view is ViewGroup) {
            for (i in view.childCount - 1 downTo 0) { // сверху вниз
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
                handleMotionEvent(event)
            }
            return delegate.dispatchTouchEvent(event)
        }
    }

    /*Обработка этапов касания ищем view по которой был ACTION_DOWN*/
    private fun handleMotionEvent(event: MotionEvent) {
        val action = event.actionMasked
        val timestamp = System.currentTimeMillis()

        when (action) {
            MotionEvent.ACTION_DOWN -> {
                currentGesture = mutableListOf()
                inmomentTargetView = findViewAt(deps.activityProvider?.invoke()?.window?.decorView, event.rawX.toInt(), event.rawY.toInt())
                addTouchEvent(event, timestamp)
            }
            MotionEvent.ACTION_MOVE -> {
                addTouchEvent(event, timestamp)
            }
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                addTouchEvent(event, timestamp)
                if (!currentGesture.isNullOrEmpty() && inmomentTargetView != null) {
                    val gesture = createGestureFromEvents(currentGesture!!)
                    recordInteraction(inmomentTargetView!!, gesture)
                }
                currentGesture = null
                inmomentTargetView = null
            }
        }
    }
    // сохраним отдельную точку касания в текущий жест
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
//определимс типа жеста на основе времени и дистанции
    private fun createGestureFromEvents(events: List<TouchEvent>): Gesture {
        val start = events.first()
        val end = events.last()
        val duration = end.timestamp - start.timestamp
        val dx = end.x - start.x
        val dy = end.y - start.y
        val distance = sqrt(dx * dx + dy * dy)
        val swipablex = dpinpixels(SWIPE)
        val maxdist = dpinpixels(20f)
        val type = when {
            duration > LONG_PRESS_DURATION && distance < maxdist ->
                GestureType.LONG_PRESS
            distance > swipablex ->
                GestureType.SWIPE
            duration <= TAP_MAX_DURATION && distance < maxdist ->
                GestureType.TAP
            else -> GestureType.MOVE
        }

        return Gesture(
            type = type,
            startEvent = start,
            endEvent = end,
            posledovatelnostMoveEvent = if (events.size > 2) events.drop(1).dropLast(1) else emptyList() // все event которые находяться между UP DOWN при этом первое и последнее действие удаляем
        )
    }
//создаем запись о взаимодействии и добавляем в историбю
    private fun recordInteraction(view: View, gesture: Gesture) {
        val record = InteractionRecord(
            viewInfo = createViewInfo(view),
            gesture = gesture,
            timestamp = gesture.startEvent.timestamp
        )
        history.add(record)
    }
//информация о view (ID, текст, класс, размеры)
    private fun createViewInfo(view: View): ViewInfo {
        val id = if (view.id != View.NO_ID) view.id else null
        val idName = id?.let {
            runCatching { deps.context.resources.getResourceEntryName(it) }.getOrNull() //
        }
        val bounds = getViewBoundsOnScreen(view)
        val text = if (view is TextView) view.text?.toString() else null
        return ViewInfo(
            className = view.javaClass.simpleName,
            id = id,
            idName = idName,
            bounds = bounds,
            text = text
        )
    }
//абсолютгые координаты view на экране
    private fun getViewBoundsOnScreen(view: View): Rect? {
        if (!view.isAttachedToWindow) return null
        val loc = IntArray(2)
        view.getLocationOnScreen(loc)
        return Rect(loc[0], loc[1], loc[0] + view.width, loc[1] + view.height)
    }

    private fun dpinpixels(dp: Float): Float =
        dp * deps.context.resources.displayMetrics.density
    //итоговая статистика по всем типам наэатий
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