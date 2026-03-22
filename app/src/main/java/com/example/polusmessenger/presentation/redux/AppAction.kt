package com.example.polusmessenger.presentation.redux

import kotlinx.coroutines.flow.*

import com.example.polusmessenger.domain.Chat
import com.example.polusmessenger.domain.Message

interface Action

sealed class AppAction : Action {
    object LoadChats : AppAction() //не несет за собой никаких данных просто загружаем
    data class ChatsLoaded(val chats: List<Chat>) : AppAction()
    data class LoadChatsFailed(val error: String) : AppAction()
    data class SelectChat(val chatId: Int) : AppAction()
    data class LoadMessages(val chatId: Int) : AppAction()
    data class MessagesLoaded(val chatId: Int, val messages: List<Message>) : AppAction()
    data class MessagesLoadFailed(val chatId: Int, val error: String) : AppAction()

    data class SendMessage(val chatId: Int, val text: String) : AppAction()
    data class MessageSendFailed(val error: String) : AppAction()

    data class CreateChat(val name: String) : AppAction()
    data class ChatCreated(val chat: Chat) : AppAction()
    data class ChatCreationFailed(val error: String) : AppAction()

    data class SetLoading(val loading: Boolean) : AppAction()
    data class SetError(val error: String?) : AppAction()


}