package com.example.scanview.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.scanview.R
import com.example.scanview.data.InteractionRecord
import java.text.DateFormat
import java.util.*


class InteractionAdapter(
    private var interactions: List<InteractionRecord>,
    private val onClick: ((InteractionRecord) -> Unit)? = null
) : RecyclerView.Adapter<InteractionAdapter.ViewHolder>() {

    private val dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvLine1: TextView = itemView.findViewById(R.id.tvLine1)
        val tvLine2: TextView = itemView.findViewById(R.id.tvLine2)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_interaction, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val interaction = interactions[position]
        val gesture = interaction.gesture
        val viewInfo = interaction.viewInfo
        val line1 = "${position + 1}. ${viewInfo.className} ${viewInfo.idName ?: ""} - ${gesture.type.name}"
        holder.tvLine1.text = line1
        val start = gesture.startEvent
        val end = gesture.endEvent
        val coordinates = if (end != null) {
            "screen=(${start.x.toInt()},${start.y.toInt()}) local=(${String.format("%.1f", start.localX)},${String.format("%.1f", start.localY)}) time=${dateFormat.format(Date(interaction.timestamp))}"
        } else {
            "screen=(${start.x.toInt()},${start.y.toInt()}) local=(${String.format("%.1f", start.localX)},${String.format("%.1f", start.localY)}) time=${dateFormat.format(Date(interaction.timestamp))}"
        }
        holder.tvLine2.text = coordinates

        holder.itemView.setOnClickListener {
            onClick?.invoke(interaction)
        }
    }

    override fun getItemCount(): Int = interactions.size

    fun updateInteractions(newInteractions: List<InteractionRecord>) {
        interactions = newInteractions
        notifyDataSetChanged()
    }
}
