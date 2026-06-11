package com.example.polusmessenger.presentation.redux

import com.example.polusmessenger.domain.Chat
import com.example.polusmessenger.domain.Message

data class AppState(
    val chats: List<Chat> = emptyList(),
    val chatsTotal: Int = 0,
    val messages: Map<Int, List<Message>> = emptyMap(),
    val selectedChatId: Int? = null,
    val loading: Boolean = false,
    val loadingMore: Boolean = false,
    val error: String? = null
)
