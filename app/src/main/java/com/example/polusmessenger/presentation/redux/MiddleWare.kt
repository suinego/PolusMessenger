package com.example.polusmessenger.presentation.redux

import com.example.polusmessenger.domain.usecase.CreateChatUseCase
import com.example.polusmessenger.domain.usecase.GetChatsUseCase
import com.example.polusmessenger.domain.usecase.GetMessagesUseCase
import com.example.polusmessenger.domain.usecase.SendMessageUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.*
import android.util.Log

fun createAsyncMiddleware(
    getChats: GetChatsUseCase,
    getMessages: GetMessagesUseCase,
    sendMessage: SendMessageUseCase,
    createChat: CreateChatUseCase
): Middleware = { dispatch, action, getState ->
    Log.d("Middleware", "действие: $action")
    val scope = CoroutineScope(Dispatchers.Main.immediate)

    when (action) {
        is AppAction.LoadChats -> {
            dispatch(AppAction.SetLoading(true))
            scope.launch {
                try {
                    val chats = withContext(Dispatchers.IO) { getChats() }
                    Log.d("Middleware", "загрузка чата")
                    dispatch(AppAction.ChatsLoaded(chats))
                } catch (e: Exception) {
                    dispatch(AppAction.LoadChatsFailed(e.localizedMessage ?: "unknown"))
                }
            }
        }

        is AppAction.LoadMessages -> {
            dispatch(AppAction.SetLoading(true))
            scope.launch {
                try {
                    val (chat, messages) = withContext(Dispatchers.IO) { getMessages(action.chatId) }
                    dispatch(AppAction.MessagesLoaded(chat.id, messages))
                } catch (e: Exception) {
                    dispatch(AppAction.MessagesLoadFailed(action.chatId, e.localizedMessage ?: "unknown"))
                }
            }
        }

        is AppAction.SendMessage -> {
            scope.launch {
                try {
                    val messages = withContext(Dispatchers.IO) { sendMessage(action.chatId, action.text) }
                    dispatch(AppAction.MessagesLoaded(action.chatId, messages))
                } catch (e: Exception) {
                    dispatch(AppAction.MessageSendFailed(e.localizedMessage ?: "unknown"))
                }
            }
        }

        is AppAction.CreateChat -> {
            scope.launch {
                try {
                    val chats = withContext(Dispatchers.IO) { createChat(action.name) }
                    dispatch(AppAction.ChatsLoaded(chats))
                } catch (e: Exception) {
                    dispatch(AppAction.ChatCreationFailed(e.localizedMessage ?: "unknown"))
                }
            }
        }

        else -> {}
    }
}
