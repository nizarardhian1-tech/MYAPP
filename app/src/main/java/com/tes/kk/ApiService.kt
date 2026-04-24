package com.tes.kk

import retrofit2.Call
import retrofit2.http.*

/**
 * ApiService — Define your API endpoints here.
 * TODO: Replace or add methods to match your API
 */
interface ApiService {

    @GET("items")
    fun getItems(): Call<List<String>>

    // Contoh POST:
    // @POST("items")
    // fun createItem(@Body item: String): Call<String>
}
