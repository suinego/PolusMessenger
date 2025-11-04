package com.example.polusmessenger.di

import com.example.polusmessenger.data.api.RetrofitService
import com.example.polusmessenger.domain.repository.ChatRepository
import com.example.polusmessenger.domain.usecase.*
import com.example.polusmessenger.presentation.redux.createAsyncMiddleware
import com.example.polusmessenger.presentation.redux.appReducer
import com.example.polusmessenger.presentation.redux.AppState
import com.example.polusmessenger.presentation.redux.Store

object AppModule {
    private const val BASE_URL = "http://emil-international.ru/"

    private val oauthProvider = { "0123456789" }

    val api by lazy { RetrofitService.create(BASE_URL, oauthProvider) }
    val chatRepository by lazy { ChatRepository(api) }

    val getChatsUseCase by lazy { GetChatsUseCase(chatRepository) }
    val createChatUseCase by lazy { CreateChatUseCase(chatRepository) }
    val getMessagesUseCase by lazy { GetMessagesUseCase(chatRepository) }
    val sendMessageUseCase by lazy { SendMessageUseCase(chatRepository) }

    val middlewares by lazy {
        listOf(
            createAsyncMiddleware(
                getChatsUseCase,
                getMessagesUseCase,
                sendMessageUseCase,
                createChatUseCase
            )
        )
    }

    val store by lazy {
        Store(AppState(), ::appReducer, middlewares)
    }
}