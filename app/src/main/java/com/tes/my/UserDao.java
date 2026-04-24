package com.tes.my;

import androidx.room.*;
import java.util.List;

/**
 * UserDao — Data Access Object untuk User.
 * TODO: Add queries as needed
 */
@Dao
public interface UserDao {

    @Insert
    void insert(User user);

    @Query("SELECT * FROM users ORDER BY id ASC")
    List<User> getAll();

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    User findById(int id);

    @Update
    void update(User user);

    @Delete
    void delete(User user);

    @Query("DELETE FROM users")
    void deleteAll();
}
