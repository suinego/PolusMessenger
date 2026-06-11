package com.example.polusmessenger.presentation.profile

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.polusmessenger.MainActivity
import com.example.polusmessenger.R
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class ProfileFragment : Fragment() {

    private lateinit var tvNoSessions: TextView
    private lateinit var btnSave: Button
    private lateinit var sessionsContainer: LinearLayout

    private val displayFmt = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("ru"))

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_profile, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvNoSessions = view.findViewById(R.id.tvNoSessions)
        btnSave = view.findViewById(R.id.btnSaveSession)
        sessionsContainer = view.findViewById(R.id.sessionsContainer)

        btnSave.setOnClickListener { saveCurrentSession() }
        setupExperiments(view)
    }

    override fun onResume() {
        super.onResume()
        refreshData()
        updateExperimentStatus()
    }

    private lateinit var tvExperimentStatus: TextView

    private fun setupExperiments(view: View) {
        tvExperimentStatus = view.findViewById(R.id.tvExperimentStatus)
        val exp = (requireActivity() as? MainActivity)?.experimentManager

        view.findViewById<Button>(R.id.btnStartS1).setOnClickListener {
            exp?.startS1()
            updateExperimentStatus()
        }
        view.findViewById<Button>(R.id.btnStartS2).setOnClickListener {
            exp?.startS2()
            updateExperimentStatus()
        }
        view.findViewById<Button>(R.id.btnStopExperiment).setOnClickListener {
            exp?.stopAndLog()
            updateExperimentStatus()
            Toast.makeText(requireContext(), "Результаты в Logcat (теги Experiment/S1, S2)", Toast.LENGTH_LONG).show()
        }
    }

    private fun updateExperimentStatus() {
        if (!::tvExperimentStatus.isInitialized) return
        val exp = (requireActivity() as? MainActivity)?.experimentManager
        tvExperimentStatus.text = when {
            exp == null -> "ExperimentManager недоступен"
            exp.isRunning -> "● Запущен: ${exp.activeStrategy.name}  —  нажмите ■ Стоп"
            else -> "Нет активного эксперимента. Результаты в Logcat."
        }
        tvExperimentStatus.setTextColor(
            if (exp?.isRunning == true) 0xFF00E676.toInt() else 0xFF555577.toInt()
        )
    }

    private fun saveCurrentSession() {
        val manager = (requireActivity() as? MainActivity)?.scanViewManager ?: return
        val history = manager.getHistory()
        if (history.isEmpty()) {
            Toast.makeText(requireContext(), "Нет данных — сначала запишите взаимодействия", Toast.LENGTH_SHORT).show()
            return
        }
        runCatching {
            val sdf = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss-SSS", Locale.getDefault())
            val filename = "scanview_${sdf.format(Date())}.json"
            val file = File(requireContext().filesDir, filename)
            file.writeText(manager.serialize())
            Log.d("ProfileFragment", "Сохранено: ${file.absolutePath}")
            Toast.makeText(requireContext(), "Сохранено ✓ (${history.size} событий)", Toast.LENGTH_SHORT).show()
            refreshData()
        }.onFailure { e ->
            Toast.makeText(requireContext(), "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun refreshData() {
        val files = savedFiles()
        sessionsContainer.removeAllViews()

        if (files.isEmpty()) {
            tvNoSessions.visibility = View.VISIBLE
            sessionsContainer.visibility = View.GONE
        } else {
            tvNoSessions.visibility = View.GONE
            sessionsContainer.visibility = View.VISIBLE
            files.forEach { file -> sessionsContainer.addView(buildSessionView(file)) }
        }
    }

    private fun buildSessionView(file: File): View {
        val view = LayoutInflater.from(requireContext())
            .inflate(R.layout.item_profile_session, sessionsContainer, false)

        view.findViewById<TextView>(R.id.tvSessionDate).text = parseDisplayDate(file.name)
        val kb = file.length() / 1024
        view.findViewById<TextView>(R.id.tvSessionMeta).text = "${kb} KB · ${file.name}"

        view.findViewById<TextView>(R.id.btnOpenSession).setOnClickListener {
            (requireActivity() as? MainActivity)?.openSession(file)
        }
        view.findViewById<ImageButton>(R.id.btnDeleteSession).setOnClickListener {
            file.delete()
            Toast.makeText(requireContext(), "Удалено", Toast.LENGTH_SHORT).show()
            refreshData()
        }

        return view
    }

    private fun parseDisplayDate(name: String): String =
        runCatching {
            val raw = name.removePrefix("scanview_").removeSuffix(".json")
            val sdf = if (raw.length > 19)
                SimpleDateFormat("yyyy-MM-dd_HH-mm-ss-SSS", Locale.getDefault())
            else
                SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
            displayFmt.format(sdf.parse(raw)!!)
        }.getOrElse { name }

    private fun savedFiles(): List<File> =
        requireContext().filesDir
            .listFiles { _, n -> n.startsWith("scanview_") && n.endsWith(".json") }
            ?.sortedByDescending { it.name }
            ?: emptyList()
}
