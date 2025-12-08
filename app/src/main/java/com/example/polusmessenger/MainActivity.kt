package com.example.polusmessenger

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
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
import com.example.scanview.InteractionRecorder
import com.example.polusmessenger.ui.VisualizationActivity
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class MainActivity : AppCompatActivity(R.layout.activity_main) {
    private val store = AppModule.store

    private val interactionRecorder by lazy {
        InteractionRecorder(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AppModule.init(application)

        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                replace(R.id.container, ChatListFragment())
            }
        }

        findViewById<Button>(R.id.buttonvisualize)?.setOnClickListener {
            openVisualizationActivity()
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                store.states
                    .map { it.selectedChatId }
                    .distinctUntilChanged()
                    .collect { selectedChatId ->
                        val top = supportFragmentManager.findFragmentById(R.id.container)

                        if (selectedChatId != null && top !is ChatFragment) {
                            supportFragmentManager.commit {
                                replace(R.id.container, ChatFragment().apply {
                                    arguments = Bundle().apply {
                                        putInt("chat_id", selectedChatId)
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

        AppModule.startGlobalScanWithRootProvider { window?.decorView }

        AppModule.setOnViewInteractionListener { info ->
            interactionRecorder.recordInteraction(info)

            val boundsInfo = info.viewBounds?.let { b ->
                "bounds=[left=${b.left}, top=${b.top}, right=${b.right}, bottom=${b.bottom}, width=${b.width()}, height=${b.height()}]"
            } ?: "ВЬЮ НЕ ПРИСОЕДЕН"

            Log.i(
                "ScanViewTree",
                "--------------------------------------\n" +
                        "Тач: ${info.action}\n" +
                        "-----------------------------------------------\n" +
                        "View: ${info.viewClassName}\n" +
                        "   ID: ${info.viewId ?: "NO_ID"} (${info.viewIdName ?: "нет имени"})\n" +
                        "   размер: ${info.viewWidth} x ${info.viewHeight} px\n" +
                        "   $boundsInfo\n" +
                        "----------------------------------------------------\n" +
                        " Координаты:\n" +
                        "   Локальные вью: X=${String.format("%.2f", info.localX)} Y=${String.format("%.2f", info.localY)}\n" +
                        "   Экранные или абсолютные: X=${String.format("%.2f", info.screenX)} Y=${String.format("%.2f", info.screenY)}\n" +
                        "   Внутри границ: ${if (info.isInsideBounds) "YES" else "NO"}\n" +
                        "--------------------------------------------"
            )
        }

        Log.d("ScanTest", "onResume  в root=${window?.decorView}")
    }

    override fun onPause() {
        AppModule.stopGlobalScan()

        lifecycleScope.launch {
            val file = interactionRecorder.save()
            Log.d("InteractionRecorder", "В JsON: ${file.absolutePath}")
        }

        super.onPause()
    }

    private fun openVisualizationActivity() {
        val intent = Intent(this, VisualizationActivity::class.java)
        startActivity(intent)
    }
}
