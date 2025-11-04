package com.example.polusmessenger.domain

data class Message(
    val id: Int?,
    val text: String,
    val sender: String? = null
)