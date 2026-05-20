package com.example.polusmessenger.presentation.results

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.polusmessenger.R
import java.io.File

data class SessionItem(
    val file: File?,        // null = текущая запись
    val label: String,
    val subtitle: String
)

class SessionFileAdapter(
    private var items: List<SessionItem>,
    private var selectedIndex: Int = 0,
    private val onSelect: (SessionItem) -> Unit
) : RecyclerView.Adapter<SessionFileAdapter.VH>() {

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val card: CardView = view.findViewById(R.id.sessionCard)
        val tvLabel: TextView = view.findViewById(R.id.tvSessionLabel)
        val tvSubtitle: TextView = view.findViewById(R.id.tvSessionSubtitle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_session_file, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.tvLabel.text = item.label
        holder.tvSubtitle.text = item.subtitle

        val isSelected = position == selectedIndex
        holder.card.setCardBackgroundColor(
            if (isSelected) 0xFF3700B3.toInt() else 0xFF1E1E2E.toInt()
        )
        holder.tvLabel.setTextColor(
            if (isSelected) 0xFFFFFFFF.toInt() else 0xFFBBBBBB.toInt()
        )

        holder.itemView.setOnClickListener {
            val prev = selectedIndex
            selectedIndex = position
            notifyItemChanged(prev)
            notifyItemChanged(position)
            onSelect(item)
        }
    }

    override fun getItemCount() = items.size

}
