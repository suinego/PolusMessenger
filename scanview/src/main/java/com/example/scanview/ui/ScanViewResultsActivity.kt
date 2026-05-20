package com.example.scanview.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.scanview.R
import com.example.scanview.data.InteractionRecord
import java.util.ArrayList
import java.util.Locale

class ScanViewResultsActivity : AppCompatActivity() {

    private lateinit var adapter: InteractionAdapter
    private var currentHistory: List<InteractionRecord> = ArrayList<InteractionRecord>()

    private lateinit var recyclerView: RecyclerView
    private lateinit var tvEmptyState: TextView
    private lateinit var tvCurrentScreenHeader: TextView
    private lateinit var btnClear: Button
    private lateinit var btnClose: Button
    private lateinit var visualizationContainer: com.example.scanview.visualization.ScanViewVisualizationView
    private lateinit var seekBar: SeekBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scanview_results)

        val pending = pendingHistory
        if (pending != null) {
            val newList = ArrayList<InteractionRecord>()
            for (item in pending) {
                newList.add(item)
            }
            currentHistory = newList
        }

        Log.d("ScanViewResults", "onCreate: получено " + currentHistory.size + " взаимодействий")
        clearPendingData()

        recyclerView = findViewById(R.id.recyclerViewInteractions)
        tvEmptyState = findViewById(R.id.tvEmptyState)
        tvCurrentScreenHeader = findViewById(R.id.tvCurrentScreenHeader)
        btnClear = findViewById(R.id.btnClear)
        btnClose = findViewById(R.id.btnClose)
        visualizationContainer = findViewById(R.id.visualizationContainer)
        seekBar = findViewById(R.id.seekBarTimeline)

        adapter = InteractionAdapter(currentHistory) { interaction ->
            visualizationContainer.centerOn(interaction.gesture.startEvent.x, interaction.gesture.startEvent.y)
            updateHeader(interaction.screenName)
            syncSeekBarWithInteraction(interaction)
        }
        
        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        recyclerView.adapter = adapter

        btnClear.setOnClickListener { clearHistory() }
        btnClose.setOnClickListener { finish() }

        loadData()
        setupSeekBar()
    }

    private fun syncSeekBarWithInteraction(interaction: InteractionRecord) {
        if (currentHistory.isEmpty()) return
        
        var minT = Long.MAX_VALUE
        var maxT = Long.MIN_VALUE
        
        for (record in currentHistory) {
            val start = record.gesture.startEvent.timestamp
            val endEvent = record.gesture.endEvent
            val end = if (endEvent != null) endEvent.timestamp else start
            if (start < minT) minT = start
            if (end > maxT) maxT = end
        }
        
        val total = maxT - minT
        if (total > 0) {
            val progress = (((interaction.gesture.startEvent.timestamp - minT).toFloat() / total.toFloat()) * 1000).toInt()
            seekBar.progress = progress
        }
    }

    private fun loadData() {
        adapter.updateInteractions(currentHistory)

        if (!currentHistory.isEmpty()) {
            val screenW = resources.displayMetrics.widthPixels
            val screenH = resources.displayMetrics.heightPixels
            visualizationContainer.setInteractions(currentHistory, screenW, screenH)
            visualizationContainer.visibility = View.VISIBLE
            seekBar.visibility = View.VISIBLE
            
            val first = currentHistory[0]
            updateHeader(first.screenName)
            
            visualizationContainer.post {
                visualizationContainer.centerOn(first.gesture.startEvent.x, first.gesture.startEvent.y)
            }
        } else {
            visualizationContainer.visibility = View.GONE
            seekBar.visibility = View.GONE
            updateHeader("Нет данных")
        }

        if (currentHistory.isEmpty()) {
            recyclerView.visibility = View.GONE
            tvEmptyState.visibility = View.VISIBLE
        } else {
            recyclerView.visibility = View.VISIBLE
            tvEmptyState.visibility = View.GONE
        }
        seekBar.progress = 0
        visualizationContainer.setProgress(0f)
    }

    private fun updateHeader(screenName: String?) {
        if (screenName != null) {
            tvCurrentScreenHeader.text = screenName.uppercase(Locale.getDefault())
        } else {
            tvCurrentScreenHeader.text = "UNKNOWN SCREEN"
        }
    }

    private fun setupSeekBar() {
        seekBar.max = 1000
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, value: Int, fromUser: Boolean) {
                val progress = value.toFloat() / 1000f
                visualizationContainer.setProgress(progress)
                updateHeaderAtProgress(progress)
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })
    }

    private fun updateHeaderAtProgress(progress: Float) {
        if (currentHistory.isEmpty()) return
        
        var minT = Long.MAX_VALUE
        var maxT = Long.MIN_VALUE
        for (record in currentHistory) {
            val start = record.gesture.startEvent.timestamp
            val endEvent = record.gesture.endEvent
            val end = if (endEvent != null) endEvent.timestamp else start
            if (start < minT) minT = start
            if (end > maxT) maxT = end
        }
        
        val currentTime = minT + (progress * (maxT - minT).toFloat()).toLong()
        
        var active: InteractionRecord? = null
        for (record in currentHistory) {
            if (record.gesture.startEvent.timestamp <= currentTime) {
                active = record
            }
        }
        
        if (active != null) {
            updateHeader(active.screenName)
        }
    }

    private fun clearHistory() {
        currentHistory = ArrayList<InteractionRecord>()
        loadData()
        Toast.makeText(this, "История очищена", Toast.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
        adapter.updateInteractions(currentHistory)
    }

    companion object {
        private var pendingHistory: List<InteractionRecord>? = null
        fun setData(history: List<InteractionRecord>) {
            pendingHistory = history
        }
        private fun clearPendingData() {
            pendingHistory = null
        }
        fun createIntent(context: android.content.Context): Intent {
            return Intent(context, ScanViewResultsActivity::class.java)
        }
    }
}
