package com.example.polusmessenger.data.api

import com.google.gson.annotations.SerializedName

data class ChatDto(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String
)