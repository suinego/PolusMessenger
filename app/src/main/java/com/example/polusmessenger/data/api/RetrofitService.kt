package com.example.polusmessenger.data.api

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitService {
    fun create(baseUrl: String, accessToken: String) : ApiService {
        val client = OkHttpClient.Builder().addInterceptor {
            bond -> val request = bond.request().newBuilder().addHeader("oauth", accessToken).build()
            bond.proceed(request)
        }.build()
        return Retrofit.Builder().baseUrl(baseUrl).client(client).addConverterFactory(
            GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}