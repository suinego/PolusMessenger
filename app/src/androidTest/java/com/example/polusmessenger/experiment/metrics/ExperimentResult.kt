package com.example.polusmessenger.experiment.metrics

data class ExperimentResult(
    val strategy: String,
    val scenario: String,
    val run: Int,

    val avgFrameMs: Double,
    val p95FrameMs: Double,
    val jankCount: Int,
    val totalFrames: Int,

    val dataKB: Double,
    val captureCount: Int,

    val durationMs: Long
)
