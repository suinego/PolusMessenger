/*
package com.example.polusmessenger.data.api


import kotlinx.coroutines.delay

class FakeApiService : ApiService {
    private val chats = mutableListOf(
        ChatDto(id = 1, name = "Общий чат"),
        ChatDto(id = 2, name = "Рабочая группа"),
        ChatDto(id = 3, name = "Друзья")
    )

    private val messages = mutableMapOf(
        1 to mutableListOf(
            MessageDto(1, "Привет!"),
            MessageDto(2, "Как дела?")
        ),
        2 to mutableListOf(
            MessageDto(1, "Работаем над проектом!"),
        ),
        3 to mutableListOf(
            MessageDto(1, "Вечером встречаемся?"),
        )
    )

    override suspend fun getChats(): List<ChatDto> {
        return chats
    }

    override suspend fun getChat(chatId: Int): ChatWithMessagesDto {
        return ChatWithMessagesDto(
            id = chatId,
            name = chats.first { it.id == chatId }.name,
            messages = messages[chatId] ?: emptyList()
        )
    }

    override suspend fun postMessage(chatId: Int, text: String): List<MessageDto> {
        val newMessage = MessageDto(
            id = (1..10000).random(),
            text = text
        )
        messages.getOrPut(chatId) { mutableListOf() }.add(newMessage)
        return messages[chatId]!!
    }

    override suspend fun createChat(name: String): List<ChatDto> {
        val newChat = ChatDto(id = (1..1000).random(), name = name)
        chats.add(newChat)
        return chats
    }
}
*/
