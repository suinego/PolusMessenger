package com.example.polusmessenger.data.api.responce

import com.google.gson.annotations.SerializedName

data class ChatWithMessagesResponse(
    @SerializedName("id") val id: Int,
    @SerializedName("messages") val messages: List<MessageResponse>? = null,
    @SerializedName("total") val total: Int? = null,
    @SerializedName("limit") val limit: Int? = null,
    @SerializedName("offset") val offset: Int? = null
)
