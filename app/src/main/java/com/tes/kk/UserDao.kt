package com.tes.kk

import androidx.room.*

/**
 * UserDao — Data Access Object untuk User.
 * TODO: Add queries as needed
 */
@Dao
interface UserDao {

    @Insert
    suspend fun insert(user: User)

    @Query("SELECT * FROM users ORDER BY id ASC")
    fun getAll(): List<User>

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    fun findById(id: Int): User?

    @Update
    suspend fun update(user: User)

    @Delete
    suspend fun delete(user: User)

    @Query("DELETE FROM users")
    suspend fun deleteAll()
}
