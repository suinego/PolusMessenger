package com.example.polusmessenger.presentation.results

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.polusmessenger.MainActivity
import com.example.polusmessenger.R
import com.example.scanview.data.InteractionRecord
import com.example.scanview.ui.InteractionAdapter
import com.example.scanview.visualization.ScanViewVisualizationView
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class ScanResultsFragment : Fragment() {

    private lateinit var visualizationView: ScanViewVisualizationView
    private lateinit var seekBar: SeekBar
    private lateinit var recyclerInteractions: RecyclerView
    private lateinit var tvEmptyState: TextView
    private lateinit var tvInteractionCount: TextView
    private lateinit var btnClear: Button
    private lateinit var btnSave: Button

    private lateinit var interactionAdapter: InteractionAdapter

    private var currentHistory: List<InteractionRecord> = emptyList()
    // null = текущая запись в памяти, файл = загруженная сессия
    var loadedFromFile: File? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_scan_results, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        visualizationView = view.findViewById(R.id.visualizationView)
        seekBar = view.findViewById(R.id.seekBarTimeline)
        recyclerInteractions = view.findViewById(R.id.recyclerInteractions)
        tvEmptyState = view.findViewById(R.id.tvEmptyState)
        tvInteractionCount = view.findViewById(R.id.tvInteractionCount)
        btnClear = view.findViewById(R.id.btnClearHistory)
        btnSave = view.findViewById(R.id.btnSaveSession)

        setupInteractionList()
        setupSeekBar()

        btnSave.setOnClickListener { saveCurrentSession() }

        btnClear.setOnClickListener {
            if (loadedFromFile != null) {
                loadedFromFile?.delete()
                loadedFromFile = null
                Toast.makeText(requireContext(), "Файл удалён", Toast.LENGTH_SHORT).show()
            } else {
                (requireActivity() as? MainActivity)?.scanViewManager?.clearHistory()
                Toast.makeText(requireContext(), "История очищена", Toast.LENGTH_SHORT).show()
            }
            loadData()
            updateBottomBar()
        }
    }

    override fun onResume() {
        super.onResume()
        // Если из Профиля открыли конкретный файл — загружаем его
        val pending = (requireActivity() as? MainActivity)?.pendingSessionFile
        if (pending != null) {
            loadedFromFile = pending
            (requireActivity() as? MainActivity)?.pendingSessionFile = null
        }
        loadData()
    }

    // ── Сохранение ────────────────────────────────────────────────────────────

    private fun saveCurrentSession() {
        val manager = (requireActivity() as? MainActivity)?.scanViewManager ?: return
        if (manager.getHistory().isEmpty()) {
            Toast.makeText(requireContext(), "Нечего сохранять", Toast.LENGTH_SHORT).show()
            return
        }
        runCatching {
            val sdf = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss-SSS", Locale.getDefault())
            val filename = "scanview_${sdf.format(Date())}.json"
            File(requireContext().filesDir, filename).writeText(manager.serialize())
            Log.d("ScanResultsFragment", "Сохранено: $filename")
            Toast.makeText(requireContext(), "Сохранено ✓", Toast.LENGTH_SHORT).show()
        }.onFailure { e ->
            Toast.makeText(requireContext(), "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // ── Список взаимодействий ─────────────────────────────────────────────────

    private fun setupInteractionList() {
        interactionAdapter = InteractionAdapter(emptyList()) { record ->
            val idx = currentHistory.indexOf(record)
            if (idx >= 0) {
                val total = currentHistory.size
                visualizationView.goToStep(idx, total)
                seekBar.progress = idx
                recyclerInteractions.scrollToPosition(idx)
            }
        }
        recyclerInteractions.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        recyclerInteractions.adapter = interactionAdapter
    }

    // ── Seekbar ───────────────────────────────────────────────────────────────

    private fun setupSeekBar() {
        seekBar.max = 1
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, value: Int, fromUser: Boolean) {
                if (!fromUser) return
                val total = currentHistory.size
                if (total <= 1) return
                visualizationView.setProgress(value.toFloat() / (total - 1))
                recyclerInteractions.scrollToPosition(value)
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })
    }

    // ── Загрузка данных ───────────────────────────────────────────────────────

    private fun loadData() {
        val history: List<InteractionRecord> = if (loadedFromFile != null) {
            val manager = (requireActivity() as? MainActivity)?.scanViewManager
            runCatching { manager?.deserialize(loadedFromFile!!.readText()) ?: emptyList() }
                .getOrElse { emptyList() }
        } else {
            (requireActivity() as? MainActivity)?.scanViewManager?.getHistory() ?: emptyList()
        }

        currentHistory = history
        interactionAdapter.updateInteractions(history)

        if (history.isNotEmpty()) {
            val m = resources.displayMetrics
            visualizationView.setInteractions(history, m.widthPixels, m.heightPixels)
            visualizationView.visibility = View.VISIBLE
            seekBar.visibility = View.VISIBLE
            seekBar.max = (history.size - 1).coerceAtLeast(1)
            seekBar.progress = 0
            visualizationView.setProgress(0f)
            recyclerInteractions.visibility = View.VISIBLE
            tvEmptyState.visibility = View.GONE
            tvInteractionCount.visibility = View.VISIBLE
            tvInteractionCount.text = "${history.size} шагов" +
                if (loadedFromFile != null) " · ${loadedFromFile!!.name.take(20)}" else " · текущая сессия"
        } else {
            visualizationView.visibility = View.INVISIBLE
            seekBar.visibility = View.INVISIBLE
            recyclerInteractions.visibility = View.GONE
            tvEmptyState.visibility = View.VISIBLE
            tvInteractionCount.visibility = View.GONE
        }

        updateBottomBar()
    }

    private fun updateBottomBar() {
        val isCurrentSession = loadedFromFile == null
        val hasData = (requireActivity() as? MainActivity)
            ?.scanViewManager?.getHistory()?.isNotEmpty() == true
        btnSave.visibility = if (isCurrentSession && hasData) View.VISIBLE else View.GONE
        btnClear.text = if (isCurrentSession) "Очистить" else "Удалить"
    }
}
