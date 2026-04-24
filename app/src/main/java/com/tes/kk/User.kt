package com.tes.kk

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * User — Contoh Entity Room.
 * TODO: Replace or add fields as needed
 */
@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String  // TODO: tambah field
)
