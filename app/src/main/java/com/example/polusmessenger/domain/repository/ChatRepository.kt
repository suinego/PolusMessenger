package com.example.polusmessenger.domain.repository

import android.util.Log
import com.example.polusmessenger.data.api.ApiService
import com.example.polusmessenger.data.api.toDomain
import com.example.polusmessenger.data.api.toDomainList
import com.example.polusmessenger.domain.Chat
import com.example.polusmessenger.domain.Message

class ChatRepository(private val api: ApiService) : ChatRepositoryInterface {

    override suspend fun getChatsPage(limit: Int, offset: Int): Pair<List<Chat>, Int> {
        val response = api.getChats(limit = limit, offset = offset)
        val chatsList = response.chats ?: response.data ?: emptyList()
        val total = response.total ?: chatsList.size
        Log.d("ChatRepository", "загружено чатов: ${chatsList.size}, total=$total, offset=$offset")
        return chatsList.map { it.toDomain() } to total
    }

    override suspend fun getMessages(chatId: Int): Pair<Chat, List<Message>> {
        Log.d("ChatRepository", "вызов getChat($chatId)")
        val response = api.getChat(chatId)
        val messagesCount = response.messages?.size ?: 0
        Log.d("ChatRepository", "загружено сообщений: $messagesCount для чата $chatId")
        return response.toDomain()
    }

    override suspend fun sendMessage(chatId: Int, text: String): List<Message> {
        val resp = api.postMessage(chatId, text)
        return resp.messages.toDomainList()
    }

    override suspend fun createChat(name: String): List<Chat> {
        val response = api.createChat(name)
        val chatsList = response.chats ?: response.data ?: emptyList()
        Log.d("ChatRepository", "создан новый чат '$name', всего чатов: ${chatsList.size}")
        return chatsList.map { it.toDomain() }
    }
}
