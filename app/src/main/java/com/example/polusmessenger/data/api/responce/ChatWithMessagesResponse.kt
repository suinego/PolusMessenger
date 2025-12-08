    package com.example.polusmessenger.data.api.responce

    import com.google.gson.annotations.SerializedName

    data class ChatWithMessagesResponse(
        @SerializedName("id") val id: Int,
        @SerializedName("name") val name: String,
        @SerializedName("messages") val messages: List<MessageResponse>
    )