package com.example.polusmessenger.domain.usecase
import com.example.polusmessenger.domain.Chat
import com.example.polusmessenger.domain.Message
import com.example.polusmessenger.domain.repository.ChatRepository

class GetChatsUseCase(private val repo: ChatRepository) {
    suspend operator fun invoke(): List<Chat> = repo.getChats()
}

class CreateChatUseCase(private val repo: ChatRepository) {
    suspend operator fun invoke(name: String): Chat = repo.createNewChat(name)
    suspend fun createNewChat(name: String): Chat = repo.createNewChat(name)

}
class GetMessagesUseCase(private val repo: ChatRepository) {
    suspend operator fun invoke(chatId: Int): Pair<Chat, List<Message>> = repo.getMessages(chatId)
}

class SendMessageUseCase(private val repo: ChatRepository) {
    suspend operator fun invoke(chatId: Int, text: String): List<Message> = repo.sendMessage(chatId, text)
}