package com.example.scanview.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.scanview.R
import com.example.scanview.data.InteractionRecord

class ScanViewResultsActivity : AppCompatActivity() {

    private lateinit var adapter: InteractionAdapter
    
    private var currentHistory: List<InteractionRecord> = emptyList()

    private lateinit var recyclerView: RecyclerView
    private lateinit var tvEmptyState: TextView
    private lateinit var btnClear: Button
    private lateinit var btnClose: Button
    private lateinit var visualizationContainer: com.example.scanview.visualization.ScanViewVisualizationView
    private lateinit var seekBar: android.widget.SeekBar


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scanview_results)


        currentHistory = pendingHistory?.toList() ?: emptyList()
        android.util.Log.d("ScanViewResults", "onCreate: получено ${currentHistory.size} взаимодействий из pendingHistory")
        if (currentHistory.isEmpty() && pendingHistory != null) {
            android.util.Log.w("ScanViewResults", "History не null cgbcjr пуст")
        }
        clearPendingData()

        recyclerView = findViewById(R.id.recyclerViewInteractions)
        tvEmptyState = findViewById(R.id.tvEmptyState)
        btnClear = findViewById(R.id.btnClear)
        btnClose = findViewById(R.id.btnClose)
        visualizationContainer = findViewById(R.id.visualizationContainer)
        seekBar = findViewById(R.id.seekBarTimeline)

        adapter = InteractionAdapter(currentHistory) { interaction ->
            visualizationContainer.centerOn(interaction.gesture.startEvent.x, interaction.gesture.startEvent.y)
        }
        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        recyclerView.adapter = adapter

        btnClear.setOnClickListener {
            clearHistory()
        }

        btnClose.setOnClickListener {
            finish()
        }

        loadData()
        setupSeekBar()

    }

    private fun loadData() {
        val history = currentHistory

        adapter.updateInteractions(history)

        if (history.isNotEmpty()) {
            val screenW = resources.displayMetrics.widthPixels
            val screenH = resources.displayMetrics.heightPixels
            visualizationContainer.setInteractions(history, screenW, screenH)
            visualizationContainer.visibility = View.VISIBLE
            seekBar.visibility = View.VISIBLE
        } else {
            visualizationContainer.visibility = View.GONE
            seekBar.visibility = View.GONE
        }

        if (history.isEmpty()) {
            recyclerView.visibility = View.GONE
            tvEmptyState.visibility = View.VISIBLE
        } else {
            recyclerView.visibility = View.VISIBLE
            tvEmptyState.visibility = View.GONE
        }
        seekBar.progress = 0
        visualizationContainer.setProgress(0f)
    }
    private fun setupSeekBar() {
        val seekBarMax = 1000
        seekBar.max = seekBarMax
        seekBar.setOnSeekBarChangeListener(object :
            android.widget.SeekBar.OnSeekBarChangeListener {

            override fun onProgressChanged(
                seekBar: android.widget.SeekBar?,
                value: Int,
                fromUser: Boolean
            ) {
                val progress = value.toFloat() / seekBarMax
                visualizationContainer.setProgress(progress)
            }

            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
        })
    }

    private fun clearHistory() {
        currentHistory = emptyList()
        loadData()
        Toast.makeText(this, "стория очищена", Toast.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
        adapter.updateInteractions(currentHistory)
    }

    companion object {
        private var pendingHistory: List<InteractionRecord>? = null
        fun setData(history: List<InteractionRecord>) {
            android.util.Log.d("ScanViewResults", "setData  получено ${history.size} взаимодействий")
            if (history.isEmpty()) {
                android.util.Log.w("ScanViewResults", "setData  пустой список")
            } else {
                history.forEachIndexed { index, record ->
                    android.util.Log.d("ScanViewResults", "  [$index] ${record.viewInfo.className} - ${record.gesture.type}")
                }
            }
            pendingHistory = history
            android.util.Log.d("ScanViewResults", "pendingHistory  ${pendingHistory?.size ?: 0} элементов")
        }

        private fun clearPendingData() {
            pendingHistory = null
        }

        fun createIntent(context: android.content.Context): Intent {
            return Intent(context, ScanViewResultsActivity::class.java)
        }
    }

}
