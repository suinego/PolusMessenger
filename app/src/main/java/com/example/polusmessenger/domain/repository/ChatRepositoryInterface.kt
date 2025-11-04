package com.example.polusmessenger.domain.repository
import com.example.polusmessenger.domain.Chat
import com.example.polusmessenger.domain.Message

interface ChatRepositoryInterface {
    suspend fun getChats(): List<Chat>
    suspend fun getMessages(chatId: Int): Pair<Chat, List<Message>>
    suspend fun sendMessage(chatId: Int, text: String): List<Message>
    suspend fun createChat(name: String): List<Chat>
}