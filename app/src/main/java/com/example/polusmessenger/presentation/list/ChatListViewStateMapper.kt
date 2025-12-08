package com.example.polusmessenger.presentation.list

import com.example.polusmessenger.presentation.redux.AppState
import com.example.polusmessenger.presentation.redux.Store
import kotlinx.coroutines.flow.*

class ChatListViewStateMapper(private val store: Store<AppState>) {
    fun viewStates(): Flow<ChatListViewState> {
        return store.states
            .map { state ->
                ChatListViewState(
                    isLoading = state.loading,
                    chats = state.chats.toList(),
                    error = state.error
                )
            }
            .distinctUntilChanged()
    }
}
