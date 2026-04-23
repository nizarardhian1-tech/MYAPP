package com.moby.app;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * User — Contoh Entity Room.
 * TODO: Ganti atau tambah field sesuai kebutuhan.
 * Bisa duplikat file ini untuk Entity lain.
 */
@Entity(tableName = "users")
public class User {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String name;  // TODO: tambah field sesuai kebutuhan

    public User(String name) {
        this.name = name;
    }
}
