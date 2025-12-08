package com.example.scanview

import android.graphics.Rect

data class ViewNode(
    val idName: String?, //android:id="@+id/recyclerMessages"
    val id: Int?,// id вьюшки
    val className: String,
    val visibility: String, //видно?
    val clickable: Boolean,
    val enabled: Boolean,
    val bounds: Rect?, //расположение
    val text: String?, //для конопок и текствьюшек
    val children: List<ViewNode> = emptyList()
)
