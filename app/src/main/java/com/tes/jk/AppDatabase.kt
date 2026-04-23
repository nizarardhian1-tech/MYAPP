package com.tes.jk

import android.content.Context
import androidx.room.*

/**
 * AppDatabase — Room Database singleton.
 * TODO: Tambah Entity baru: entities = [User::class, ...]
 * PERHATIAN: fallbackToDestructiveMigration() menghapus data
 * saat schema berubah. Ganti dengan migrasi di production.
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
