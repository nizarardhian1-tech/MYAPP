package com.tes.aop.di;

import dagger.Module;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

/**
 * AppModule — Hilt dependency injection module.
 *
 * TODO: Add @Provides methods here to inject dependencies.
 * Example: provide Retrofit, OkHttpClient, Repository, etc.
 *
 * @Provides
 * @Singleton
 * public Retrofit provideRetrofit() {
 *     return new Retrofit.Builder()
 *         .baseUrl(ApiClient.BASE_URL)
 *         .addConverterFactory(GsonConverterFactory.create())
 *         .build();
 * }
 */
@Module
@InstallIn(SingletonComponent.class)
public class AppModule {
    // Add @Provides methods here
}
