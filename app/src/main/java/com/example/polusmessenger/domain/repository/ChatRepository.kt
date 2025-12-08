package com.example.polusmessenger.domain.repository

import android.util.Log
import com.example.polusmessenger.data.api.ApiService
import com.example.polusmessenger.data.api.toDomain
import com.example.polusmessenger.data.api.toDomainList
import com.example.polusmessenger.domain.Chat
import com.example.polusmessenger.domain.Message

class ChatRepository(private val api: ApiService) : ChatRepositoryInterface {

    override suspend fun getChats(): List<Chat> {
        val response = api.getChats()
        Log.d("ChatRepository", "загружено чатов: ${response.chats.size}")
        return response.chats.map { it.toDomain() }
    }

    override suspend fun getMessages(chatId: Int): Pair<Chat, List<Message>> {
        Log.d("ChatRepository", "вызов getChatика($chatId)")
        val response = api.getChat(chatId)
        Log.d("ChatRepository", "загружено сообщений: ${response.messages.size} для чата ${response.name}")
        return response.toDomain()
    }
    override suspend fun sendMessage(chatId: Int, text: String): List<Message> {
        val resp = api.postMessage(chatId, text)
        return resp.toDomainList()
    }


    override suspend fun createChat(name: String): List<Chat> {
        val response = api.createChat(name)
        Log.d("ChatRepository", "создан новый чат '$name'")
        val (_, allChats) = response.toDomain()
        return allChats
    }

    suspend fun createNewChat(name: String): Chat {
        val response = api.createChat(name)
        val (newChat, _) = response.toDomain()
        Log.d("ChatRepository", "создан новый чат '${newChat.name}' с id=${newChat.id}")
        return newChat
    }
}
