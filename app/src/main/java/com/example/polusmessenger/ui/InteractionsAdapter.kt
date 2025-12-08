package com.example.polusmessenger.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.polusmessenger.R
import com.example.scanview.InteractionData
import java.text.DateFormat
import java.util.*

class InteractionsAdapter(
    private val onClick: (InteractionData) -> Unit
) : ListAdapter<InteractionData, InteractionsAdapter.Holder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_interaction, parent, false)
        return Holder(view)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(getItem(position), position, onClick)
    }

    class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvLine1: TextView = itemView.findViewById(R.id.tvLine1)
        private val tvLine2: TextView = itemView.findViewById(R.id.tvLine2)

        fun bind(data: InteractionData, pos: Int, onClick: (InteractionData) -> Unit) {
            val df = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
            tvLine1.text = "${pos + 1}. ${data.viewClassName} ${data.viewIdName ?: ""} - ${data.action}"
            tvLine2.text = "screen=(${data.screenX.toInt()},${data.screenY.toInt()}) local=(${String.format("%.1f", data.localX)},${String.format("%.1f", data.localY)}) time=${df.format(Date(data.timestamp))}"
            itemView.setOnClickListener { onClick(data) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<InteractionData>() {
        override fun areItemsTheSame(oldItem: InteractionData, newItem: InteractionData): Boolean {
            return oldItem.timestamp == newItem.timestamp && oldItem.action == newItem.action
        }

        override fun areContentsTheSame(oldItem: InteractionData, newItem: InteractionData): Boolean {
            return oldItem == newItem
        }
    }
}

