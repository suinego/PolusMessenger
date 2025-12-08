package com.example.polusmessenger.presentation.chat

import com.example.polusmessenger.domain.Message

data class ChatViewState(
    val chatName: String?,
    val messages: List<Message>,
    val isLoading: Boolean,
    val error: String?
)
