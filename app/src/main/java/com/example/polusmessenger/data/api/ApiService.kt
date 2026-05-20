package com.example.polusmessenger.data.api

import com.example.polusmessenger.data.api.responce.ChatWithMessagesResponse
import com.example.polusmessenger.data.api.responce.ChatsResponse
import com.example.polusmessenger.data.api.responce.SendMessageResponse
import retrofit2.http.*

interface ApiService {

    @GET("mipt_network/chats")
    suspend fun getChats(
        @Query("limit") limit: Int? = null,
        @Query("offset") offset: Int? = null
    ): ChatsResponse

    @GET("mipt_network/chat")
    suspend fun getChat(
        @Query("id") id: Int,
        @Query("limit") limit: Int? = null,
        @Query("offset") offset: Int? = null
    ): ChatWithMessagesResponse

    @POST("mipt_network/msg")
    suspend fun postMessage(
        @Query("id") id: Int,
        @Query("text") text: String
    ): SendMessageResponse

    @POST("mipt_network/create_chat")
    suspend fun createChat(@Query("name") name: String): ChatsResponse
}
