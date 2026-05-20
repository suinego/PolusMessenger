package com.example.scanview.data

import android.graphics.Rect

//дерево состояния View, захваченного в момент взаимодействия
data class ViewNode(
    val className: String,
    val bounds: Rect,
    val background: BackgroundState?,
    val alpha: Float,
    val visibility: Int,
    val content: ViewContent?,
    val children: List<ViewNode>
)

//состояние фона — только сериализуемые примитивы, Drawable не храним
sealed class BackgroundState {
    data class Color(val color: Int) : BackgroundState()
    data class Unknown(val drawableClass: String) : BackgroundState()
}

//наполнение View
sealed class ViewContent {
    data class Text(
        val text: String,
        val textColor: Int,
        val textSizePx: Float,
        val isBold: Boolean
    ) : ViewContent()

    data class ImagePlaceholder(val tint: Int?) : ViewContent()
}
