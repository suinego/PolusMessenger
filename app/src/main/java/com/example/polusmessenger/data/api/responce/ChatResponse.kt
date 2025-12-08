package com.example.polusmessenger.data.api.responce

import com.google.gson.annotations.SerializedName

data class ChatResponse(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String
)
data class ChatsResponseDto(
    val chats: List<ChatResponse>
)