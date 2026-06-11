package com.example.scanview.data

data class DiagnosticMetrics(

    val avgFrameMs: Double,
    val p95FrameMs: Double,
    val jankFrameCount: Int,
    val totalFrameCount: Int,

    val idNameResolutionRate: Double,
    val resolvedIdCount: Int,
    val totalInteractions: Int,

    val maxTimestampGapMs: Long,
    val avgTimestampGapMs: Double,

    val avgEventHandlingUs: Double,
    val maxEventHandlingUs: Long
)
