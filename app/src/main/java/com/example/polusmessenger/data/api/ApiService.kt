package com.example.polusmessenger.data.api

import retrofit2.http.*

interface ApiService {

    @GET("mipt_network/chats")
    suspend fun getChats(): ChatsResponse

    @GET("mipt_network/chat")
    suspend fun getChat(@Query("id") id: Int): ChatWithMessagesDto

    @POST("mipt_network/msg")
    suspend fun postMessage(
        @Query("id") id: Int,
        @Query("text") text: String
    ): List<MessageDto>

    @POST("mipt_network/create_chat")
    suspend fun createChat(@Query("name") name: String): List<ChatDto>
}
