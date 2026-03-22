package com.example.polusmessenger.data.api

import com.example.polusmessenger.data.api.responce.ChatResponse
import com.example.polusmessenger.data.api.responce.ChatWithMessagesResponse
import com.example.polusmessenger.data.api.responce.MessageResponse
import com.example.polusmessenger.domain.Chat
import com.example.polusmessenger.domain.Message

fun ChatResponse.toDomain(): Chat = Chat(
    id = this.id,
    name = this.name
)

fun MessageResponse.toDomain(): Message {
    return Message(
        id = this.id,
        text = this.text,
    )
}
fun List<MessageResponse>.toDomainList(): List<Message> =
    map { it.toDomain() }

fun ChatWithMessagesResponse.toDomain(): Pair<Chat, List<Message>> {
    val chat = Chat( id = this.id, name = "${this.id}")
    val messages = messages.map { it.toDomain() }
    return chat to messages
}

