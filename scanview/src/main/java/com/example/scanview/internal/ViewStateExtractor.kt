package com.example.scanview.internal

import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.widget.ImageView
import android.widget.TextView
import android.view.View
import android.view.ViewGroup
import com.example.scanview.data.BackgroundState
import com.example.scanview.data.ViewContent
import com.example.scanview.data.ViewNode

internal object ViewStateExtractor {

    // Ограничиваем глубину, чтобы JSON не раздувался, 
    // но сохраняем структуру самой View и её ближайших детей.
    fun extract(view: View, maxDepth: Int = 3): ViewNode {
        return extractRecursive(view, 0, maxDepth)
    }

    private fun extractRecursive(view: View, currentDepth: Int, maxDepth: Int): ViewNode {
        val loc = IntArray(2)
        view.getLocationOnScreen(loc)
        val bounds = Rect(loc[0], loc[1], loc[0] + view.width, loc[1] + view.height)

        return ViewNode(
            className = view.javaClass.simpleName,
            bounds = bounds,
            background = extractBackground(view.background),
            alpha = view.alpha,
            visibility = view.visibility,
            content = extractContent(view),
            children = if (currentDepth < maxDepth) extractChildren(view, currentDepth, maxDepth) else emptyList()
        )
    }

    private fun extractBackground(drawable: Drawable?): BackgroundState? {
        drawable ?: return null
        return when (drawable) {
            is ColorDrawable -> BackgroundState.Color(drawable.color)
            else -> BackgroundState.Unknown(drawable.javaClass.simpleName)
        }
    }

    private fun extractContent(view: View): ViewContent? {
        return when (view) {
            is TextView -> ViewContent.Text(
                text = view.text?.toString() ?: "",
                textColor = view.currentTextColor,
                textSizePx = view.textSize,
                isBold = view.typeface?.isBold ?: false
            )
            is ImageView -> ViewContent.ImagePlaceholder(
                tint = view.imageTintList?.defaultColor
            )
            else -> null
        }
    }

    private fun extractChildren(view: View, currentDepth: Int, maxDepth: Int): List<ViewNode> {
        if (view !is ViewGroup) return emptyList()
        return (0 until view.childCount).mapNotNull { i ->
            val child = view.getChildAt(i)
            if (child.visibility == View.VISIBLE) {
                extractRecursive(child, currentDepth + 1, maxDepth)
            } else null
        }
    }
}
