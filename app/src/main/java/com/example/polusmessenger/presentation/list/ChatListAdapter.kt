package com.example.polusmessenger.presentation.list

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.polusmessenger.R
import com.example.polusmessenger.domain.Chat

class ChatListAdapter(private val onClick: (Chat) -> Unit) :
    ListAdapter<Chat, ChatListAdapter.VH>(DIFF) {

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<Chat>() {
            override fun areItemsTheSame(oldItem: Chat, newItem: Chat) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: Chat, newItem: Chat) = oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_chat, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        private val tvName: TextView = view.findViewById(R.id.chatName)
        init {
            view.setOnClickListener { getItem(bindingAdapterPosition)?.let(onClick) }
        }
        fun bind(chat: Chat) {
            tvName.text = chat.name
        }
    }
}