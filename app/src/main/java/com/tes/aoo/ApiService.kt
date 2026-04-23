package com.tes.aoo

import retrofit2.Call
import retrofit2.http.*

/**
 * ApiService — Definisikan endpoint API di sini.
 * TODO: Ganti atau tambah method sesuai API Anda
 */
interface ApiService {

    @GET("items")
    fun getItems(): Call<List<String>>

    // Contoh POST:
    // @POST("items")
    // fun createItem(@Body item: String): Call<String>
}
