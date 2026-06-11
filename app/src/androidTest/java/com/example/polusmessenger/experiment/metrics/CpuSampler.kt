package com.example.polusmessenger.experiment.metrics

import android.os.Process
import android.os.SystemClock
import android.util.Log

class CpuSampler(private val intervalMs: Long = 500L) {

    companion object {
        const val TIMELINE_TAG = "CPU_TIMELINE"
    }

    private val samples = mutableListOf<Double>()
    private var running = false
    private var thread: Thread? = null
    private var runLabel = ""

    fun start(label: String) {
        runLabel = label
        samples.clear()
        running = true
        thread = Thread {
            var prevCpu  = Process.getElapsedCpuTime()
            var prevWall = SystemClock.elapsedRealtime()
            Thread.sleep(intervalMs)
            var idx = 0
            while (running) {
                val currCpu  = Process.getElapsedCpuTime()
                val currWall = SystemClock.elapsedRealtime()

                val deltaCpu  = (currCpu  - prevCpu).toDouble()
                val deltaWall = (currWall - prevWall).toDouble()
                val pct = if (deltaWall > 0)
                    (deltaCpu / deltaWall * 100.0).coerceAtLeast(0.0)
                else 0.0

                synchronized(samples) { samples.add(pct) }
                Log.i(TIMELINE_TAG, "$runLabel,$idx,%.2f".format(pct))

                prevCpu  = currCpu
                prevWall = currWall
                idx++
                Thread.sleep(intervalMs)
            }
        }.also { it.isDaemon = true; it.start() }
    }

    fun stop(): CpuResult {
        running = false
        thread?.join(intervalMs * 2)
        val snap = synchronized(samples) { samples.toList() }
        return CpuResult(
            label       = runLabel,
            avgCpuPct   = if (snap.isEmpty()) 0.0 else snap.average(),
            maxCpuPct   = snap.maxOrNull() ?: 0.0,
            sampleCount = snap.size,
            samples     = snap
        )
    }

    data class CpuResult(
        val label: String,
        val avgCpuPct: Double,
        val maxCpuPct: Double,
        val sampleCount: Int,
        val samples: List<Double>
    )
}
