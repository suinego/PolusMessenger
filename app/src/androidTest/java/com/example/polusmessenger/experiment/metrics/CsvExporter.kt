package com.example.polusmessenger.experiment.metrics

import android.content.Context
import android.os.Environment
import android.util.Log
import java.io.File

object CsvExporter {

    private const val TAG      = "CsvExporter"
    const val LOG_TAG          = "EXP_CSV"
    private const val FILENAME = "experiment_results.csv"

    private val HEADER = "strategy,scenario,run,avgFrameMs,p95FrameMs,jankCount,totalFrames,dataKB,captureCount,durationMs"

    fun append(context: Context, results: List<ExperimentResult>) {
        if (results.isEmpty()) return

        val lines = buildList {
            for (r in results) {
                add("${r.strategy},${r.scenario},${r.run}," +
                    "${"%.3f".format(r.avgFrameMs)},${"%.3f".format(r.p95FrameMs)}," +
                    "${r.jankCount},${r.totalFrames}," +
                    "${"%.3f".format(r.dataKB)},${r.captureCount},${r.durationMs}")
            }
        }

        Log.i(LOG_TAG, HEADER)
        lines.forEach { Log.i(LOG_TAG, it) }
        Log.i(TAG, "Залогировано ${results.size} строк с тегом $LOG_TAG")

        val candidates = listOf(
            File(Environment.getExternalStorageDirectory(), FILENAME),
            File(context.getExternalFilesDir(null), FILENAME),
            File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS), FILENAME),
            File(context.filesDir, FILENAME)
        )

        for (file in candidates) {
            try {
                file.parentFile?.mkdirs()
                val needHeader = !file.exists() || file.length() == 0L
                file.appendText(buildString {
                    if (needHeader) appendLine(HEADER)
                    lines.forEach { appendLine(it) }
                })
                Log.i(TAG, "✓ Файл сохранён: ${file.absolutePath}")
                return
            } catch (e: Exception) {
                Log.w(TAG, "✗ Не удалось записать в ${file.absolutePath}: ${e.message}")
            }
        }

        Log.e(TAG, "Файл не сохранён ни в одном из путей. Используйте Logcat-вариант.")
    }

    fun clear(context: Context) {
        listOf(
            File(Environment.getExternalStorageDirectory(), FILENAME),
            File(context.getExternalFilesDir(null), FILENAME),
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), FILENAME),
            File(context.filesDir, FILENAME)
        ).forEach { it.delete() }
        Log.i(TAG, "Результаты очищены.")
    }
}
