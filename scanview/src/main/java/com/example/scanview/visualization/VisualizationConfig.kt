package com.example.scanview.visualization

/**
 * Настройки визуализации вынесены в отдельный файл для стабильности компиляции
 */
data class VisualizationConfig(
    val tapPointRadius: Float = 22f,
    val swipeLineWidth: Float = 5f,
    val arrowLength: Float = 24f
)
