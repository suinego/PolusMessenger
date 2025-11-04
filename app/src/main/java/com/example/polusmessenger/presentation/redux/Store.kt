package com.example.polusmessenger.presentation.redux

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

typealias Middleware = suspend (
    dispatch: suspend (AppAction) -> Unit,
    action: AppAction,
    getState: () -> AppState
) -> Unit

class Store(
    initialState: AppState,
    private val reducer: (AppState, AppAction) -> AppState,
    private val middlewareList: List<Middleware> = emptyList()
) {
    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<AppState> = _state
    private val scope = CoroutineScope(Dispatchers.Main)

    fun getState(): AppState = _state.value

    fun dispatch(action: AppAction) {
        scope.launch {
            if (middlewareList.isNotEmpty()) {
                middlewareList.forEach { middleware ->
                    middleware({ newAction ->
                        reduce(newAction)
                    }, action, ::getState)
                }
            } else {
                reduce(action)
            }
        }
    }

    private fun reduce(action: AppAction) {
        _state.value = reducer(getState(), action)
    }
}
