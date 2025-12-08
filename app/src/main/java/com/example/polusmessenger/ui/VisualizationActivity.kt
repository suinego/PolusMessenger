package com.example.polusmessenger.ui

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.polusmessenger.R
import com.example.scanview.InteractionRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.LinearLayoutManager


class VisualizationActivity : AppCompatActivity() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var interactionRecorder: InteractionRecorder
    private lateinit var visualizerView: InteractionVisualizerView
    private lateinit var adapter: InteractionsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_visualization)

        interactionRecorder = InteractionRecorder(applicationContext)
        visualizerView = findViewById(R.id.visualizerView)

        val rv = findViewById<RecyclerView>(R.id.rvInteractions)
        adapter = InteractionsAdapter { interaction ->
            visualizerView.centerOn(interaction.screenX, interaction.screenY)
        }
        rv.adapter = adapter
        rv.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        findViewById<Button>(R.id.btnClose).setOnClickListener { finish() }
        findViewById<Button>(R.id.btnClear).setOnClickListener { showClearDialog() }
        findViewById<Button>(R.id.btnSaveNow).setOnClickListener {
            scope.launch {
                val file = interactionRecorder.save()
                Log.d("VisualizationActivity", "сохранили интеракции в файл ${file.absolutePath}")
            }
        }

        loadAndVisualize()
    }

    private fun loadAndVisualize() {
        scope.launch {
            val interactions = interactionRecorder.load()
            if (interactions.isEmpty()) {
                Toast.makeText(this@VisualizationActivity, "нет взаимодействий", Toast.LENGTH_SHORT).show()
                return@launch
            }
            val screenW = resources.displayMetrics.widthPixels
            val screenH = resources.displayMetrics.heightPixels

            visualizerView.loadInteractions(interactions, screenW, screenH)
            adapter.submitList(interactions)
        }
    }

    private fun showClearDialog() {
        AlertDialog.Builder(this)
            .setTitle("Очистить данные")
            .setMessage("Удалить все?")
            .setPositiveButton("Удалить") { _, _ ->
                scope.launch {
                    interactionRecorder.deleteFile()
                    interactionRecorder.clearInMemory()
                    adapter.submitList(emptyList())
                    visualizerView.loadInteractions(emptyList(), 0, 0)
                    Toast.makeText(this@VisualizationActivity, "Данные удалены", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
}
