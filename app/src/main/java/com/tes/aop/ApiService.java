package com.tes.aop;

import retrofit2.Call;
import retrofit2.http.*;
import java.util.List;

/**
 * ApiService — Define your API endpoints here.
 * TODO: Replace or add methods to match your API
 */
public interface ApiService {

    @GET("items")
    Call<List<String>> getItems();

    // Contoh POST:
    // @POST("items")
    // Call<String> createItem(@Body String item);
}
