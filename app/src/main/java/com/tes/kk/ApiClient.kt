package com.tes.kk

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * ApiClient — Singleton Retrofit.
 * TODO: Replace BASE_URL with your actual API base URL
 */
object ApiClient {
    private const val BASE_URL = "https://api.example.com/"

    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    inline fun <reified T> getService(): T = retrofit.create(T::class.java)
}
