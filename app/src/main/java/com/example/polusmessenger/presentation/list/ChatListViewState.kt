package com.example.polusmessenger.presentation.list

import com.example.polusmessenger.domain.Chat

data class ChatListViewState(
    val isLoading: Boolean = false,
    val chats: List<Chat> = emptyList(),
    val error: String? = null
)
