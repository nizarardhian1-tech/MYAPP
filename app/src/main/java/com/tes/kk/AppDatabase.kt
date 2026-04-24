package com.tes.kk

import android.content.Context
import androidx.room.*

/**
 * AppDatabase — Room Database singleton.
 * TODO: Add new Entities here: entities = [User::class, ...]
 * WARNING: fallbackToDestructiveMigration() will DELETE all data
 * when the schema changes. Replace with proper migrations in production.
 */
@Database(entities = [User::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java, "app_db"
                ).fallbackToDestructiveMigration().build().also { instance = it }
            }
    }
}
