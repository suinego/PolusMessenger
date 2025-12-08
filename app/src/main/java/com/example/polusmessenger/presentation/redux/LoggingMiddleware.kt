package com.example.polusmessenger.presentation.redux

class LoggingMiddleware<S: Any> : Middleware<S> {
    override fun intercept(store: Store<S>, next: Dispatch): Dispatch = { action ->
        android.util.Log.d("MW", "Before action: $action, state=${store.getState()}")
        next(action)
        android.util.Log.d("MW", "After state=${store.getState()}")
    }
}
