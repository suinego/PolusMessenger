package com.example.polusmessenger.presentation.redux

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import android.util.Log
typealias Dispatch = suspend (Action) -> Unit

fun interface Middleware<S : Any> {
    fun intercept(store: Store<S>, next: Dispatch): Dispatch
}

fun interface Epic {
    fun act(actions: Flow<Action>) : Flow<Action>
}

class Store<State : Any>(
    initialState: State,
    private val reducer: (State, Action) -> State,
    middlewares: List<Middleware<State>> = emptyList(),
    epics: List<Epic> = emptyList(),
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
) {

    private val _states = MutableStateFlow(initialState)
    val states: StateFlow<State> = _states.asStateFlow()

    private val _actions = MutableSharedFlow<Action>(extraBufferCapacity = 64)

    private val currentState: State get() = _states.value
    private val baseDispatch: Dispatch = { action ->
        val newState = reducer(currentState, action)
        Log.e("BaseDispatch", "reducer запущен")
        _states.value = newState
    }

    private val dispatchChain: Dispatch = middlewares.asReversed().fold(baseDispatch) { next, mw ->
        mw.intercept(this, next)
    }

    init {
        scope.launch {
            _actions.collect { action ->
                try {
                    dispatchChain(action)
                } catch (ce: CancellationException) {
                    throw ce
                } catch (e: Exception) {
                    android.util.Log.e("Store", "Store загрузка $action", e)
                }
            }
        }

        epics.forEach { epic ->
            scope.launch {
                epic.act(_actions)
                    .catch { e -> android.util.Log.e("Epic", "Epic ошибка", e) }
                    .collect { produced ->
                        _actions.emit(produced)
                    }
            }
        }
    }

    suspend fun dispatch(action: Action) {
        _actions.emit(action)
    }

    fun dispatchSync(action: Action) {
        CoroutineScope(Dispatchers.Main).launch { dispatch(action) }
    }

    fun getState(): State = currentState
}
