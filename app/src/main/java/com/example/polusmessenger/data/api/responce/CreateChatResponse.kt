package com.example.polusmessenger.data.api.responce

import com.google.gson.annotations.SerializedName

data class CreateChatResponse(
    @SerializedName("chat") val chat: ChatResponse,
    @SerializedName("chats") val chats: List<ChatResponse>
)
