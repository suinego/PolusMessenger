package com.example.polusmessenger.data.api

import com.google.gson.annotations.SerializedName

data class ChatWithMessagesDto(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("messages") val messages: List<MessageDto>
)