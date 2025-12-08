package com.example.polusmessenger.data.api

import com.example.polusmessenger.data.api.responce.ChatResponse
import com.example.polusmessenger.data.api.responce.ChatWithMessagesResponse
import com.example.polusmessenger.data.api.responce.MessageResponse
import com.example.polusmessenger.data.api.responce.CreateChatResponse
import com.example.polusmessenger.data.api.responce.MessagesResponse
import com.example.polusmessenger.domain.Chat
import com.example.polusmessenger.domain.Message

fun ChatResponse.toDomain(): Chat = Chat(
    id = this.id ?: 0,
    name = this.name ?: ""
)

fun MessageResponse.toDomain(): Message {
    return Message(
        id = this.id ?: 0,
        text = this.text ?: "",
    )
}
fun MessagesResponse.toDomainList(): List<Message> = messages.map { it.toDomain() }

fun CreateChatResponse.toDomain(): Pair<Chat, List<Chat>> {
    val allChats = chats.orEmpty().map { it.toDomain() }
    val newChat = chat?.toDomain()
        ?: allChats.lastOrNull()
        ?: Chat(
            id = 0,
            name = ""
        )
    return newChat to allChats
}
fun ChatWithMessagesResponse.toDomain(): Pair<Chat, List<Message>> {
    val chat = Chat( id = this.id ?: 0,
        name = this.name ?: "")
    val messages = messages.map { it.toDomain() }
    return chat to messages
}