package com.example.polusmessenger.data.api

import com.example.polusmessenger.data.api.ChatDto
import com.example.polusmessenger.data.api.MessageDto
import com.example.polusmessenger.data.api.ChatWithMessagesDto
import com.example.polusmessenger.domain.Chat
import com.example.polusmessenger.domain.Message

fun ChatDto.toDomain(): Chat = Chat(
    id = id,
    name = name
)

fun MessageDto.toDomain(): Message = Message(
    id = id,
    text = text
)

fun ChatWithMessagesDto.toDomain(): Pair<Chat, List<Message>> {
    val chat = Chat(id = id, name = name)
    val messages = messages.map { it.toDomain() }
    return chat to messages
}