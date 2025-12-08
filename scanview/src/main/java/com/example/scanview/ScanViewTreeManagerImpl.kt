package com.example.scanview

import android.annotation.SuppressLint
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.TextView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.lang.ref.WeakReference

internal class ScanViewTreeManagerImpl(
    private val deps: ScanViewTreeManagerDeps
) : ScanViewTreeManager {

    private val interceptedViews = mutableSetOf<WeakReference<View>>()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val TAG_KEY = R.id.scanview_touch_listener_tag
    private var isIntercepting = false
    private var rootViewObserver: ViewTreeObserver? = null
    private var globalLayoutListener: ViewTreeObserver.OnGlobalLayoutListener? = null

    override suspend fun scanTree(): ViewNode = withContext(Dispatchers.Main) {
        val root = deps.rootViewProvider() ?: throw IllegalStateException("рут нулевой")
        buildNode(root)
    }

    override fun startIntercepting() {
        mainHandler.post {
            val root = deps.rootViewProvider() ?: run {
                deps.logger?.invoke("ScanViewTree", "ноль в руте  после начала интерсепта")
                return@post
            }

            isIntercepting = true

            setupViewTreeObserver(root)
            connectAndAttach(root)
            val count = getInterceptedViews().size
            deps.logger?.invoke("ScanViewTree", "начало интерсепта $count вьюшек")
        }
    }

    private fun setupViewTreeObserver(root: View) {
        rootViewObserver?.let { observer ->
            globalLayoutListener?.let { listener ->
                if (observer.isAlive) {
                        observer.removeOnGlobalLayoutListener(listener)
                }
            }
        }

        val observer = root.viewTreeObserver
        rootViewObserver = observer

        globalLayoutListener = ViewTreeObserver.OnGlobalLayoutListener {
            if (isIntercepting) {
                val currentRoot = deps.rootViewProvider() ?: return@OnGlobalLayoutListener
                connectAndAttach(currentRoot)
            }
        }

        observer.addOnGlobalLayoutListener(globalLayoutListener)
    }

    override fun stopIntercepting() {
        mainHandler.post {
            isIntercepting = false

            rootViewObserver?.let { observer ->
                globalLayoutListener?.let { listener ->
                    if (observer.isAlive) {
                            observer.removeOnGlobalLayoutListener(listener)
                    }
                }
            }
            rootViewObserver = null
            globalLayoutListener = null

            val alive = interceptedViews.mapNotNull { it.get() }
            alive.forEach { view ->
                val tag = view.getTag(TAG_KEY)
                if (tag == true) {
                    view.setOnTouchListener(null)
                    view.setTag(TAG_KEY, null)
                }
            }
            interceptedViews.clear()
            deps.logger?.invoke("ScanViewTree", "очищена")
        }
    }

    override fun getInterceptedViews(): List<View> {
        val alive = interceptedViews.mapNotNull { it.get() }
        interceptedViews.removeAll { it.get() == null }
        return alive
    }



    private fun buildNode(v: View): ViewNode {
        val id = if (v.id != View.NO_ID) v.id else null
        val idName = id?.let { tryResolveIdName(v, it) }
        val className = v.javaClass.simpleName ?: v.javaClass.name
        val visibility = when (v.visibility) {
            View.VISIBLE -> "VISIBLE"
            View.INVISIBLE -> "INVISIBLE"
            View.GONE -> "GONE"
            else -> "UNKNOWN"
        }
        val clickable = v.isClickable
        val enabled = v.isEnabled
        val bounds = getViewBoundsOnScreen(v)
        val text = (v as? TextView)?.text?.toString()

        val children = if (v is ViewGroup) {
            val list = mutableListOf<ViewNode>()
            for (i in 0 until v.childCount) {
                list.add(buildNode(v.getChildAt(i)))
            }
            list
        } else emptyList()

        return ViewNode(
            idName = idName,
            id = id,
            className = className,
            visibility = visibility,
            clickable = clickable,
            enabled = enabled,
            bounds = bounds,
            text = text,
            children = children
        )
    }

    private fun tryResolveIdName(v: View, id: Int): String? =
        deps.context.resources.getResourceEntryName(id)

    private fun getViewBoundsOnScreen(v: View): Rect? {
        if (!v.isAttachedToWindow) return null
        val loc = IntArray(2)
        v.getLocationOnScreen(loc)
        return Rect(loc[0], loc[1], loc[0] + v.width, loc[1] + v.height)
    }



    private fun connectAndAttach(root: View) {
        if (shouldAttachTo(root)) attachToView(root)
        if (root is ViewGroup) {
            for (i in 0 until root.childCount) connectAndAttach(root.getChildAt(i))
        }
    }

    private fun shouldAttachTo(v: View): Boolean {
        return v.isClickable || v.isLongClickable || (v.contentDescription != null) ||
                (v is TextView && (v.text?.isNotEmpty() == true))
    }

    private fun attachToView(v: View) {
        if (interceptedViews.any { it.get() == v }) return
        val alreadyMarked = v.getTag(TAG_KEY) as? Boolean
        if (alreadyMarked == true) return

        if (!v.isAttachedToWindow) {
            v.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(view: View) {
                    installListener(view)
                    view.removeOnAttachStateChangeListener(this)
                }

                override fun onViewDetachedFromWindow(view: View) {}
            })
            interceptedViews.add(WeakReference(v))
            v.setTag(TAG_KEY, true)
            return
        }

        installListener(v)
        interceptedViews.add(WeakReference(v))
        v.setTag(TAG_KEY, true)
    }

    private fun installListener(v: View) {
        var isDownInside = false

        deps.logger?.invoke("ScanViewTree", "DEBUG: установили листенера на ${v.javaClass.simpleName}")

        val ourListener = View.OnTouchListener { view, motionEvent ->
            deps.logger?.invoke("ScanViewTree", "DEBUG: точ: ${motionEvent.actionMasked}")
            val localX = motionEvent.x
            val localY = motionEvent.y
            val screenX = motionEvent.rawX
            val screenY = motionEvent.rawY

            val inside = localX >= 0 && localX <= view.width &&
                    localY >= 0 && localY <= view.height

            val action = motionEvent.actionMasked
            val actionString = when (action) {
                MotionEvent.ACTION_DOWN -> "ACTION_DOWN"
                MotionEvent.ACTION_UP -> "ACTION_UP"
                MotionEvent.ACTION_MOVE -> "ACTION_MOVE"
                MotionEvent.ACTION_CANCEL -> "ACTION_CANCEL"
                else -> "ACTION_${motionEvent.action}"
            }

            val bounds = getViewBoundsOnScreen(view)
            val info = ViewInteractionInfo(
                view = view,
                viewClassName = view.javaClass.simpleName,
                viewId = if (view.id != View.NO_ID) view.id else null,
                viewIdName = if (view.id != View.NO_ID) tryResolveIdName(view, view.id) else null,
                motionEvent = motionEvent,
                action = actionString,
                localX = motionEvent.x,
                localY = motionEvent.y,
                screenX = motionEvent.rawX,
                screenY = motionEvent.rawY,
                viewBounds = bounds,
                viewWidth = view.width,
                viewHeight = view.height,
                isInsideBounds = inside,
                timestamp = System.currentTimeMillis()
            )

            deps.onViewInteraction?.invoke(info)

            when (action) {
                MotionEvent.ACTION_DOWN -> {
                    isDownInside = inside
                }

                MotionEvent.ACTION_UP -> {
                    if (isDownInside && inside) {
                        view.performClick()
                    }
                    isDownInside = false
                }

                MotionEvent.ACTION_CANCEL -> {
                    isDownInside = false
                }
            }

            false
        }

        v.setOnTouchListener(ourListener)
    }
}
