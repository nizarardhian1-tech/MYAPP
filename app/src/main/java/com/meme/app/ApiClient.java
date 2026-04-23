package com.meme.app;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * ApiClient — Singleton Retrofit.
 * TODO: Ganti BASE_URL dengan URL API Anda
 */
public class ApiClient {
    private static final String BASE_URL = "https://api.example.com/";
    private static Retrofit retrofit;

    public static Retrofit getInstance() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder().baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create()).build();
        }
        return retrofit;
    }
}
