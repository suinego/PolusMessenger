package com.example.polusmessenger.data.api.responce

import com.google.gson.annotations.SerializedName

data class MessageResponse(
    @SerializedName("id") val id: Int,
    @SerializedName("text") val text: String
)

data class MessagesResponse(
    val messages: List<MessageResponse> = emptyList()
)