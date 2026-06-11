package com.example.polusmessenger.presentation.list

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.example.polusmessenger.R
import com.example.polusmessenger.domain.Chat
import java.net.URLEncoder

class ChatListAdapter(private val onClick: (Chat) -> Unit) :
    ListAdapter<Chat, ChatListAdapter.VH>(DIFF) {

    private var fullList: List<Chat> = emptyList()

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<Chat>() {
            override fun areItemsTheSame(oldItem: Chat, newItem: Chat) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: Chat, newItem: Chat) = oldItem == newItem
        }
    }

    fun setChats(list: List<Chat>) {
        fullList = list
        submitList(list)
    }

    fun filter(query: String) {
        val filtered = if (query.isBlank()) fullList
        else fullList.filter { it.name.contains(query, ignoreCase = true) }
        submitList(filtered)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_chat, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    private val avatarColors = listOf(
        "6200EE", "0077B6", "E63946", "2A9D8F", "E76F51", "457B9D", "6A4C93", "1B4332"
    )

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        private val tvName: TextView = view.findViewById(R.id.chatName)
        private val tvSubtitle: TextView = view.findViewById(R.id.chatSubtitle)
        private val ivAvatar: ImageView = view.findViewById(R.id.chatAvatar)
        private val dot: View = view.findViewById(R.id.chatDot)

        init {
            view.setOnClickListener { getItem(bindingAdapterPosition)?.let(onClick) }
        }

        fun bind(chat: Chat) {
            tvName.text = chat.name
            tvSubtitle.text = "Чат #${chat.id}"

            val bg = avatarColors[chat.id % avatarColors.size]
            val encodedName = URLEncoder.encode(chat.name.take(2), "UTF-8")
            val avatarUrl = "https://ui-avatars.com/api/?name=$encodedName&size=128&background=$bg&color=fff&bold=true"
            Glide.with(ivAvatar)
                .load(avatarUrl)
                .transform(CircleCrop())
                .placeholder(R.drawable.ic_chat_placeholder)
                .into(ivAvatar)

            val dotColors = listOf(0xFF6200EE, 0xFF00BFA5, 0xFFFF6D00, 0xFF2979FF)
            dot.background.setTint(dotColors[chat.id % dotColors.size].toInt())
        }
    }
}
