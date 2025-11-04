package com.example.polusmessenger.data.api

import com.google.gson.annotations.SerializedName

data class MessageDto(
    @SerializedName("id") val id: Int,
    @SerializedName("text") val text: String
)