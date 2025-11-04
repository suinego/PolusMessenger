package com.example.polusmessenger.presentation.redux

import com.example.polusmessenger.domain.Chat
import com.example.polusmessenger.domain.Message

data class AppState(
    val chats: List<Chat> = emptyList(),
    val selectedChatId: Int? = null,
    val messages: Map<Int, List<Message>> = emptyMap(),
    val loading: Boolean = false,
    val error: String? = null
)
