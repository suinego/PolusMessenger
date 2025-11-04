package com.example.polusmessenger.domain.repository

import android.util.Log
import com.example.polusmessenger.data.api.ApiService
import com.example.polusmessenger.data.api.toDomain
import com.example.polusmessenger.domain.Chat
import com.example.polusmessenger.domain.Message

class ChatRepository(private val api: ApiService) : ChatRepositoryInterface {

    override suspend fun getChats(): List<Chat> {
        val response = api.getChats()
        val chats = response.chats.map { it.toDomain() }
        Log.d("ChatRepository", "Загружено чатов: ${chats.size}")
        return chats
    }

    override suspend fun getMessages(chatId: Int): Pair<Chat, List<Message>> {
        Log.d("ChatRepository", "Вызов getChat($chatId)")
        val dto = api.getChat(chatId)
        val (chat, messages) = dto.toDomain()
        Log.d("ChatRepository", "Загружено сообщений: ${messages.size} для чата ${chat.name}")
        return chat to messages
    }

    override suspend fun sendMessage(chatId: Int, text: String): List<Message> {
        val messages = api.postMessage(chatId, text).map { it.toDomain() }
        Log.d("ChatRepository", "Отправлено сообщение '$text' в чат $chatId, теперь ${messages.size} сообщений")
        return messages
    }

    override suspend fun createChat(name: String): List<Chat> {
        val chats = api.createChat(name).map { it.toDomain() }
        Log.d("ChatRepository", "Создан новый чат '$name'")
        return chats
    }
}
