package com.example.polusmessenger.presentation.chat

import com.example.polusmessenger.presentation.redux.AppState
import com.example.polusmessenger.presentation.redux.Store
import kotlinx.coroutines.flow.*

class ChatViewStateMapper(
    private val store: Store<AppState>
) {

    fun viewStates(): Flow<ChatViewState> {
        return store.states
            .map { state ->
                val chatId = state.selectedChatId
                val chat = state.chats.find { it.id == chatId }
                val messages = if (chatId != null)
                    state.messages[chatId].orEmpty()
                else emptyList()

                ChatViewState(
                    chatName = chat?.name,
                    messages = messages.toList(),
                    isLoading = state.loading,
                    error = state.error
                )
            }
            .distinctUntilChanged()
    }
}
