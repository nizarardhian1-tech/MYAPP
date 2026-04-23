package com.tes.aoo

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * User — Contoh Entity Room.
 * TODO: Ganti atau tambah field sesuai kebutuhan
 */
@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String  // TODO: tambah field
)
