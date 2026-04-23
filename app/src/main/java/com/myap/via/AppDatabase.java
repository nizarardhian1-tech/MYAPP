package com.myap.via;

import android.content.Context;
import androidx.room.*;

/**
 * AppDatabase — Room Database.
 * TODO: Tambahkan Entity class dan DAO interface
 */
@Database(entities = {/* TODO: Entity.class */}, version = 1)
public abstract class AppDatabase extends RoomDatabase {
    private static AppDatabase instance;

    // TODO: public abstract YourDao yourDao();

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(
                context.getApplicationContext(), AppDatabase.class, "app_db")
                .fallbackToDestructiveMigration().build();
        }
        return instance;
    }
}
