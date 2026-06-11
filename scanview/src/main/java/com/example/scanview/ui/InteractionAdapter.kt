package com.example.scanview.ui

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.scanview.R
import com.example.scanview.data.GestureType
import com.example.scanview.data.InteractionRecord
import java.text.SimpleDateFormat
import java.util.*


class InteractionAdapter(
    private var interactions: List<InteractionRecord>,
    private val onClick: ((InteractionRecord) -> Unit)? = null
) : RecyclerView.Adapter<InteractionAdapter.ViewHolder>() {

    private val timeFmt = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvLine1: TextView = itemView.findViewById(R.id.tvLine1)
        val tvLine2: TextView = itemView.findViewById(R.id.tvLine2)
        val accentBar: View = itemView.findViewById(R.id.accentBar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_interaction, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val record = interactions[position]
        val gesture = record.gesture
        val viewInfo = record.viewInfo

        val typeColor = gestureColor(gesture.type)
        holder.accentBar.setBackgroundColor(typeColor)
        
        holder.tvLine1.text = record.screenName
        holder.tvLine1.setTextColor(Color.WHITE)

        val viewLabel = viewInfo.idName ?: viewInfo.className
        val time = timeFmt.format(Date(record.timestamp))
        
        holder.tvLine2.text = "${position + 1}. ${gesture.type.name} на $viewLabel\n($time)"
        holder.tvLine2.setTextColor(typeColor)

        holder.itemView.setOnClickListener { onClick?.invoke(record) }
    }

    override fun getItemCount(): Int = interactions.size

    fun updateInteractions(newInteractions: List<InteractionRecord>) {
        interactions = newInteractions
        notifyDataSetChanged()
    }

    companion object {
        fun gestureColor(type: GestureType): Int = when (type) {
            GestureType.TAP        -> 0xFF00C853.toInt()
            GestureType.SWIPE      -> 0xFFFF6D00.toInt()
            GestureType.LONG_PRESS -> 0xFFAA00FF.toInt()
            GestureType.MOVE       -> 0xFF0091EA.toInt()
        }
    }
}
