package com.example.polusmessenger

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.polusmessenger.R
import com.example.polusmessenger.di.AppModule
import com.example.polusmessenger.presentation.chat.ChatFragment
import com.example.polusmessenger.presentation.list.ChatListFragment
import com.example.scanview.api.ScanViewManager
import com.example.scanview.api.ScanViewManagerFactory
import com.example.scanview.api.ScanViewManagerDeps
import com.example.scanview.visualization.ScanViewVisualizer
import com.example.scanview.visualization.ScanViewOverlay
import com.example.scanview.ui.ScanViewResultsActivity
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.File

class MainActivity : AppCompatActivity(R.layout.activity_main) {
    private val store by lazy { AppModule.store }

    private val scanViewManager: ScanViewManager by lazy {
        val deps = object : ScanViewManagerDeps { //передаем зависимости
            override val context = this@MainActivity //активити контекст
            override val rootViewProvider: () -> View? = //корневой вью
                { window.decorView.rootView }
            override val activityProvider: (() -> android.app.Activity)? = //callback,  лямбда так как будет иначе держать активити
                   { this@MainActivity }
            override val logger: ((String, String) -> Unit)? = //логер
                { tag, msg -> Log.d(tag, msg) }
        }

        ScanViewManagerFactory.create(deps)
    }
    private var visualizationOverlay: ScanViewOverlay? = null // для визуализации
//Доработать
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            supportFragmentManager.commit {  // фрагмент списка при первой загрузке
                replace(R.id.container, ChatListFragment())
            }
        }

        // просмотр scanview кнопка

        findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fabScanViewResults)
            ?.setOnClickListener {
                val history = scanViewManager.getHistory()
                Log.d(
                    "MainActivity","Открытие экрана результатов: передано ${history.size} взаимодействий"
                )
                if (history.isEmpty()) {
                    Log.w("MainActivity", "нет истории ")
                } else {
                    history.forEachIndexed { index, record ->
                        Log.d(
                            "MainActivity", "  [$index] ${record.viewInfo.className} - ${record.gesture.type} at (${record.gesture.startEvent.x}, ${record.gesture.startEvent.y})"
                        )
                    }
                }
                ScanViewResultsActivity.setData(history)
                startActivity(ScanViewResultsActivity.createIntent(this))
            }
    // cледим за выбранным чатом в хранилище и подгружаем соответствующий фрагмент
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                store.states
                    .map { it.selectedChatId }
                    .distinctUntilChanged()
                    .collect { selectedChatId ->
                        val top = supportFragmentManager.findFragmentById(R.id.container)

                        if (selectedChatId != null && top !is ChatFragment) {
                            val chatId = selectedChatId
                            supportFragmentManager.commit {
                                replace(R.id.container, ChatFragment().apply {
                                    arguments = Bundle().apply {
                                        putInt("chat_id", chatId)
                                    }
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


    override fun onResume() {
        super.onResume()
        scanViewManager.startRecording() //Запускаем наше взаимодествия
        Log.d("MainActivity", "запись начата")

        val rootView = window.decorView.rootView as? ViewGroup //view есть в корне и overlay создали
        rootView?.let {
            Log.d("MainActivity", "rootView детей: ${it.childCount}")
            for (i in 0 until it.childCount) {
                val child = it.getChildAt(i)
                Log.d("MainActivity", "  дети $i: ${child.javaClass.simpleName}")
            }
        }
    }

    override fun onPause() {
        super.onPause()
        scanViewManager.stopRecording() //остановка
        try {
            val json = scanViewManager.serialize() //jason сохраняем
            val file = File(filesDir, "scanview_history.json")
            file.writeText(json)
            Log.d("MainActivity", "JSON сохранён: ${file.absolutePath}")
        } catch (e: Exception) {
            Log.e("MainActivity", "Ошибка сериализации: ${e.message}", e)
        }
        val stats = scanViewManager.getStatistics()
        val interactions = scanViewManager.getHistory()

        Log.d(
            "MainActivity", "onPause: ScanViewManager статистика:\n" +
                    "  Всего взаимодействий: ${stats.totalInteractions}\n" +
                    "  Тапы: ${stats.taps}, Свайпы: ${stats.swipes}, Движения: ${stats.moves}, Долгие нажатия: ${stats.longPresses}\n" +
                    "  Уникальных View: ${stats.uniqueViews}\n" +
                    "  История содержит: ${interactions.size} записей"
        )

        if (interactions.isEmpty()) {
            Log.w("MainActivity", "история пустая после stopRecord")
        } else {
            Log.d("MainActivity", "История взаимодействий")
            interactions.forEachIndexed { index, record ->
                Log.d(
                    "MainActivity",
                    "  [$index] ${record.viewInfo.className} (${record.viewInfo.idName ?: "NO_ID"}) - ${record.gesture.type} at (${record.gesture.startEvent.x}, ${record.gesture.startEvent.y})"
                )
            }
        }
        //добавляем визуализацию на экран
        if (interactions.isNotEmpty()) {
            val rootView = window.decorView.rootView as? ViewGroup
            rootView?.let { viewGroup: ViewGroup ->
                visualizationOverlay = ScanViewVisualizer.attachOverlay(
                    parent = viewGroup,
                    interactions = interactions
                )
                // закрытие
                visualizationOverlay?.setOnDismissListener {
                    ScanViewVisualizer.detachOverlay(viewGroup, visualizationOverlay!!)
                    visualizationOverlay = null
                }
                Log.d("MainActivity", "Визуализация добавлена: ${interactions.size} взаимодействий")
            }
        }
    }
}