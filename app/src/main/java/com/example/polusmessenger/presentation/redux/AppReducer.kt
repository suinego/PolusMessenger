package com.example.polusmessenger.presentation.redux

import android.util.Log
import com.example.polusmessenger.domain.Chat
import com.example.polusmessenger.domain.Message

//как меняется состояние нашего приложения при действиях пользователя
fun appReducer(state: AppState, action: Action): AppState {
    val a = action as? AppAction ?: return state
    return state.copy(
        chats = reduceChats(state.chats, a),
        messages = reduceMessages(state.messages, a),
        selectedChatId = reduceSelectedChat(state.selectedChatId, a),
        loading = reduceLoading(state.loading, a),
        error = reduceError(state.error, a)
    )
}
private fun reduceChats(chats: List<Chat>, action: AppAction): List<Chat> = when (action) {
    is AppAction.ChatsLoaded -> action.chats
    is AppAction.ChatCreated -> chats + action.chat // хотим иммутабельно вернуть новый state
    else -> chats // иначе возвращаем старый state
}

private fun reduceMessages(
    messages: Map<Int, List<Message>>,
    action: AppAction
): Map<Int, List<Message>> = when (action) {
    is AppAction.MessagesLoaded -> {
        messages + (action.chatId to action.messages)
    }
    is AppAction.ChatCreated -> {
        messages + (action.chat.id to emptyList())
    }
    else -> messages
}

private fun reduceSelectedChat(selectedChatId: Int?, action: AppAction): Int? = when (action) {
    is AppAction.SelectChat -> action.chatId
    is AppAction.ChatCreated -> action.chat.id
    else -> selectedChatId
}

private fun reduceLoading(loading: Boolean, action: AppAction): Boolean = when (action) {
    is AppAction.SetLoading -> action.loading
    is AppAction.LoadChats,
    is AppAction.LoadMessages,
    is AppAction.SendMessage,
    is AppAction.CreateChat -> true
    is AppAction.ChatsLoaded,
    is AppAction.LoadChatsFailed,
    is AppAction.MessagesLoaded,
    is AppAction.MessagesLoadFailed,
    is AppAction.MessageSendFailed,
    is AppAction.ChatCreationFailed,
    is AppAction.ChatCreated -> false
    else -> loading
}

private fun reduceError(error: String?, action: AppAction): String? = when (action) {
    is AppAction.SetError -> action.error
    is AppAction.LoadChatsFailed -> action.error
    is AppAction.MessagesLoadFailed -> action.error
    is AppAction.MessageSendFailed -> action.error
    is AppAction.ChatCreationFailed -> action.error
    is AppAction.ChatsLoaded,
    is AppAction.MessagesLoaded,
    is AppAction.ChatCreated -> null
    is AppAction.LoadChats,
    is AppAction.LoadMessages,
    is AppAction.SendMessage,
    is AppAction.CreateChat -> null
    else -> error
}