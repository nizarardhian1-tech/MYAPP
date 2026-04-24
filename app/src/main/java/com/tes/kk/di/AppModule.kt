package com.tes.kk.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * AppModule — Hilt dependency injection module.
 *
 * TODO: Add @Provides or @Binds functions here.
 * Example:
 *
 * @Provides @Singleton
 * fun provideRetrofit(): Retrofit = Retrofit.Builder()
 *     .baseUrl(ApiClient.BASE_URL)
 *     .addConverterFactory(GsonConverterFactory.create())
 *     .build()
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    // Add @Provides functions here
}
