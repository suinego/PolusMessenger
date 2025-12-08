package com.example.polusmessenger.data.api

import com.example.polusmessenger.data.api.responce.ChatResponse
import com.example.polusmessenger.data.api.responce.ChatWithMessagesResponse
import com.example.polusmessenger.data.api.responce.MessageResponse
import com.example.polusmessenger.data.api.responce.ChatsResponseDto
import com.example.polusmessenger.data.api.responce.CreateChatResponse
import com.example.polusmessenger.data.api.responce.MessagesResponse
import retrofit2.http.*


interface ApiService {

    @GET("mipt_network/chats")
    suspend fun getChats(): ChatsResponseDto

    @GET("mipt_network/chat")
    suspend fun getChat(@Query("id") id: Int): ChatWithMessagesResponse

    @POST("mipt_network/msg")
    suspend fun postMessage(
        @Query("id") id: Int,
        @Query("text") text: String
    ): MessagesResponse

    @POST("mipt_network/create_chat")
    suspend fun createChat(@Query("name") name: String): CreateChatResponse
}
