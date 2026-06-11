package com.example.polusmessenger.data.api.responce

import com.google.gson.annotations.SerializedName

data class ChatResponse(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String
)

data class ChatsResponse(
    @SerializedName("chats") val chats: List<ChatResponse>? = null,
    @SerializedName("data") val data: List<ChatResponse>? = null,
    @SerializedName("total") val total: Int? = null,
    @SerializedName("limit") val limit: Int? = null,
    @SerializedName("offset") val offset: Int? = null
)
