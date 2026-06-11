package com.example.polusmessenger.di

import com.example.polusmessenger.data.api.RetrofitService
import com.example.polusmessenger.domain.repository.ChatRepository
import com.example.polusmessenger.domain.usecase.*
import com.example.polusmessenger.presentation.redux.*

object AppModule {

    private const val BASE_URL = "http://emil-international.ru/"
    private const val ACCESSTOKEN: String = "0123456789"

    val store: Store<AppState> by lazy {
        val api = RetrofitService.create(BASE_URL, ACCESSTOKEN)
        val chatRepository = ChatRepository(api)
        val getChatsUseCase = GetChatsUseCase(chatRepository)
        val createChatUseCase = CreateChatUseCase(chatRepository)
        val getMessagesUseCase = GetMessagesUseCase(chatRepository)
        val sendMessageUseCase = SendMessageUseCase(chatRepository)

        Store(
            initialState = AppState(),
            reducer = ::appReducer,
            middlewares = emptyList(),
            epics = listOf(
                LoadChatsEpic(getChatsUseCase::invoke),
                LoadMoreChatsEpic(getChatsUseCase::invoke),
                LoadMessagesEpic { chatId ->
                    val (chat, messages) = getMessagesUseCase(chatId)
                    chat to messages
                },
                SendMessageEpic({ store.getState() }, sendMessageUseCase::invoke),
                CreateChatEpic(createChatUseCase::invoke)
            )
        )
    }
}
