package com.example.polusmessenger.domain.usecase

import com.example.polusmessenger.domain.Chat
import com.example.polusmessenger.domain.Message
import com.example.polusmessenger.domain.repository.ChatRepository

private const val PAGE_SIZE = 20

class GetChatsUseCase(private val repo: ChatRepository) {
    suspend operator fun invoke(offset: Int = 0): Pair<List<Chat>, Int> =
        repo.getChatsPage(PAGE_SIZE, offset)
}

class CreateChatUseCase(private val repo: ChatRepository) {
    suspend operator fun invoke(name: String): List<Chat> = repo.createChat(name)
}

class GetMessagesUseCase(private val repo: ChatRepository) {
    suspend operator fun invoke(chatId: Int): Pair<Chat, List<Message>> = repo.getMessages(chatId)
}

class SendMessageUseCase(private val repo: ChatRepository) {
    suspend operator fun invoke(chatId: Int, text: String): List<Message> =
        repo.sendMessage(chatId, text)
}
