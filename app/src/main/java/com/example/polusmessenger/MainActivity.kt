package com.example.polusmessenger

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.polusmessenger.di.AppModule
import com.example.polusmessenger.presentation.chat.ChatFragment
import com.example.polusmessenger.presentation.list.ChatListFragment
import com.example.polusmessenger.experiments.ExperimentManager
import com.example.polusmessenger.presentation.profile.ProfileFragment
import com.example.polusmessenger.presentation.results.ScanResultsFragment
import com.example.scanview.api.ScanViewManager
import com.example.scanview.api.ScanViewManagerDeps
import com.example.scanview.api.ScanViewManagerFactory
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import android.view.FrameMetrics
import android.view.Window
import java.io.File

class MainActivity : AppCompatActivity(R.layout.activity_main) {

    private val store by lazy { AppModule.store }
    private lateinit var bottomNav: BottomNavigationView

    internal val scanViewManager: ScanViewManager by lazy {
        ScanViewManagerFactory.create(object : ScanViewManagerDeps {
            override val context = this@MainActivity
            override val rootViewProvider: () -> View? = { window.decorView.rootView }
            override val activityProvider: (() -> android.app.Activity) = { this@MainActivity }
            override val logger: ((String, String) -> Unit) = { tag, msg -> Log.d(tag, msg) }
            override val screenNameProvider: (() -> String) = {
                // Возвращаем читаемое имя текущего фрагмента
                when (supportFragmentManager.findFragmentById(R.id.container)) {
                    is ChatListFragment -> "Чаты"
                    is ChatFragment -> "Переписка"
                    is ProfileFragment -> "Профиль"
                    is ScanResultsFragment -> "Результаты"
                    else -> "Главный экран"
                }
            }
        })
    }

    // Файл сессии, который нужно открыть в ResultsFragment при следующем onResume
    var pendingSessionFile: File? = null

    /** Менеджер экспериментов S1/S2 для дипломной работы */
    val experimentManager by lazy {
        ExperimentManager(scanViewManager) { window.decorView }
    }

    // M3: накапливаем длительности кадров во время записи
    private val frameDurationsMs = mutableListOf<Double>()
    private val frameListener = Window.OnFrameMetricsAvailableListener { _, metrics, _ ->
        if (scanViewManager.isRecording()) {
            val totalNs = metrics.getMetric(FrameMetrics.TOTAL_DURATION)
            frameDurationsMs.add(totalNs / 1_000_000.0)
        }
    }

    private var currentTab = R.id.nav_chats
    private var recordingStartMs = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                replace(R.id.container, ChatListFragment())
            }
        }

        bottomNav = findViewById(R.id.bottomNav)
        bottomNav.setOnItemSelectedListener { item ->
            currentTab = item.itemId
            when (item.itemId) {
                R.id.nav_chats -> navigateToChats()
                R.id.nav_profile -> supportFragmentManager.commit {
                    replace(R.id.container, ProfileFragment())
                }
                R.id.nav_results -> supportFragmentManager.commit {
                    replace(R.id.container, ScanResultsFragment())
                }
            }
            true
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                store.states
                    .map { it.selectedChatId }
                    .distinctUntilChanged()
                    .collect { selectedChatId ->
                        if (currentTab != R.id.nav_chats) return@collect
                        val top = supportFragmentManager.findFragmentById(R.id.container)
                        if (selectedChatId != null && top !is ChatFragment) {
                            supportFragmentManager.commit {
                                replace(R.id.container, ChatFragment().apply {
                                    arguments = Bundle().apply { putInt("chat_id", selectedChatId) }
                                })
                                addToBackStack(null)
                            }
                        } else if (selectedChatId == null && top !is ChatListFragment) {
                            supportFragmentManager.popBackStack()
                        }
                    }
            }
        }
    }

    /** Открыть сохранённую сессию во вкладке Результаты */
    fun openSession(file: File) {
        pendingSessionFile = file
        bottomNav.selectedItemId = R.id.nav_results
    }

    private fun navigateToChats() {
        val top = supportFragmentManager.findFragmentById(R.id.container)
        if (top !is ChatListFragment && top !is ChatFragment) {
            supportFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
            supportFragmentManager.commit { replace(R.id.container, ChatListFragment()) }
        }
    }

    override fun onResume() {
        super.onResume()
        recordingStartMs = System.currentTimeMillis()
        frameDurationsMs.clear()
        window.addOnFrameMetricsAvailableListener(frameListener, android.os.Handler(mainLooper))
        scanViewManager.startRecording()
    }

    override fun onPause() {
        super.onPause()
        scanViewManager.stopRecording()
        window.removeOnFrameMetricsAvailableListener(frameListener)
        scanViewManager.setFrameMetricsData(frameDurationsMs.toList())
        logMetrics()
    }

    private fun logMetrics() {
        val durationMs = System.currentTimeMillis() - recordingStartMs
        val interactions = scanViewManager.getHistory().size

        // Первая строчка всегда — чтобы знать что функция вызвалась
        Log.d("ScanView", "▼▼▼ METRICS (сессия ${durationMs / 1000} с, $interactions взаим-ий) ▼▼▼")

        runCatching {
            val json     = scanViewManager.serialize()
            val sizeKb   = json.toByteArray(Charsets.UTF_8).size / 1024.0
            val kbPerMin = sizeKb / (durationMs / 60_000.0).coerceAtLeast(0.001)
            val m        = scanViewManager.computeDiagnostics()

            Log.d("ScanView", "╔══════════════════════════════════════════════════════╗")
            Log.d("ScanView", "║  M4 │ JSON: %.1f КБ  │  %.1f КБ/мин".format(sizeKb, kbPerMin))
            Log.d("ScanView", "║  M3 │ Avg: %.1f ms  │  p95: %.1f ms  │  Jank: %d/%d"
                .format(m.avgFrameMs, m.p95FrameMs, m.jankFrameCount, m.totalFrameCount))
            Log.d("ScanView", "║  Q1 │ idName: %d/%d (%.0f%%)"
                .format(m.resolvedIdCount, m.totalInteractions, m.idNameResolutionRate * 100))
            Log.d("ScanView", "║  Q2 │ Max gap: %d ms  │  Avg: %.0f ms"
                .format(m.maxTimestampGapMs, m.avgTimestampGapMs))
            Log.d("ScanView", "║ OVH │ Avg: %.1f µs  │  Max: %d µs"
                .format(m.avgEventHandlingUs, m.maxEventHandlingUs))
            Log.d("ScanView", "╚══════════════════════════════════════════════════════╝")
        }.onFailure { e ->
            Log.e("ScanView", "logMetrics упал: ${e.message}", e)
        }
    }
}
