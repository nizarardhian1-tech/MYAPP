package com.tes.aop;

import android.content.Context;
import androidx.room.*;

/**
 * AppDatabase — Room Database singleton.
 * TODO: Add new Entities to the array, e.g. entities = {User.class, ...}
 * WARNING: fallbackToDestructiveMigration() will DELETE all data
 * when the schema changes. Replace with proper migrations in production.
 */
@Database(entities = {User.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase instance;

    public abstract UserDao userDao();

    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                        context.getApplicationContext(),
                        AppDatabase.class, "app_db")
                        .fallbackToDestructiveMigration()
                        .build();
                }
            }
        }
        return instance;
    }
}
