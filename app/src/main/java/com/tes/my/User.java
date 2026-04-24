package com.tes.my;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * User — Contoh Entity Room.
 * TODO: Replace or add fields as needed
 */
@Entity(tableName = "users")
public class User {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String name;  // TODO: tambah field

    public User(String name) { this.name = name; }
}
