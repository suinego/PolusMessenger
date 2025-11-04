package com.example.polusmessenger.presentation.redux

import android.util.Log

fun appReducer(state: AppState, action: AppAction): AppState {
    return when (action) {

        is AppAction.SetLoading -> state.copy(loading = action.loading)
        is AppAction.SetError -> state.copy(error = action.error)
        is AppAction.ChatsLoaded -> state.copy(chats = action.chats, loading = false, error = null)
        is AppAction.LoadChatsFailed -> state.copy(loading = false, error = action.error)
        is AppAction.SelectChat -> state.copy(selectedChatId = action.chatId)
        is AppAction.MessagesLoaded -> {
            val newMap = state.messages.toMutableMap()
            newMap[action.chatId] = action.messages
            state.copy(messages = newMap, loading = false, error = null)

        }
        is AppAction.MessagesLoadFailed -> state.copy(loading = false, error = action.error)
        else -> state
    }
}