package com.example.scanview.data

/**
 * Результаты диагностических измерений для дипломной работы.
 *
 * M3 — просадка FPS (измеряется через FrameMetrics).
 * Q1 — точность идентификации виджетов (доля с известным idName).
 * Q2 — покрытие временно́й шкалы (макс. пробел между событиями).
 */
data class DiagnosticMetrics(

    // ── M3: FPS ───────────────────────────────────────────────────────────────
    /** Среднее время кадра в мс (FrameMetrics: TOTAL_DURATION). */
    val avgFrameMs: Double,
    /** 95-й перцентиль времени кадра в мс. */
    val p95FrameMs: Double,
    /** Количество jank-кадров (> 16.6 мс бюджет). */
    val jankFrameCount: Int,
    /** Общее число кадров в измерении. */
    val totalFrameCount: Int,

    // ── Q1: точность виджетов ─────────────────────────────────────────────────
    /** Доля взаим-ий с ненулевым idName (0.0..1.0). */
    val idNameResolutionRate: Double,
    /** Абсолютное число взаим-ий с известным idName. */
    val resolvedIdCount: Int,
    /** Общее число взаим-ий. */
    val totalInteractions: Int,

    // ── Q2: покрытие таймлайна ────────────────────────────────────────────────
    /** Максимальный пробел между соседними взаим-иями (мс). */
    val maxTimestampGapMs: Long,
    /** Средний промежуток между взаим-иями (мс). */
    val avgTimestampGapMs: Double,

    // ── Overhead записи ───────────────────────────────────────────────────────
    /** Среднее время обработки одного MotionEvent в мкс. */
    val avgEventHandlingUs: Double,
    /** Максимальное время обработки одного MotionEvent в мкс. */
    val maxEventHandlingUs: Long
)
