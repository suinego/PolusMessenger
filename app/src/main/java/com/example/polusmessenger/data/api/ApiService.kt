package com.example.polusmessenger.data.api

import com.example.polusmessenger.data.api.responce.ChatWithMessagesResponse
import com.example.polusmessenger.data.api.responce.ChatsResponse
import com.example.polusmessenger.data.api.responce.MessageResponse
import com.example.polusmessenger.data.api.responce.SendMessageResponse
import retrofit2.http.*

interface ApiService {

    @GET("chats")
    suspend fun getChats(): ChatsResponse

    @GET("chat")
    suspend fun getChat(@Query("id") id: Int): ChatWithMessagesResponse

    @POST("msg")
    suspend fun postMessage(
        @Query("id") id: Int,
        @Query("text") text: String
    ): SendMessageResponse

    @POST("create_chat")
    suspend fun createChat(@Query("name") name: String): ChatsResponse
}
