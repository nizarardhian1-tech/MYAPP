package com.tes.my;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * ApiClient — Singleton Retrofit.
 * TODO: Replace BASE_URL with your actual API base URL
 */
public class ApiClient {
    private static final String BASE_URL = "https://api.example.com/";
    private static Retrofit retrofit;

    public static Retrofit getInstance() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        }
        return retrofit;
    }

    /** Shortcut: ApiClient.getService(ApiService.class) */
    public static <T> T getService(Class<T> serviceClass) {
        return getInstance().create(serviceClass);
    }
}
