package com.example.polusmessenger.presentation.redux

class ChatInteractor(private val store: Store<AppState>) {
    fun loadChats() = store.dispatchSync(AppAction.LoadChats)
    fun selectChat(chatId: Int) {
        store.dispatchSync(AppAction.SelectChat(chatId))
        store.dispatchSync(AppAction.LoadMessages(chatId))
    }
    fun createChat(name: String) = store.dispatchSync(AppAction.CreateChat(name))
    fun sendMessage(chatId: Int, text: String) = store.dispatchSync(AppAction.SendMessage(chatId, text))
}
