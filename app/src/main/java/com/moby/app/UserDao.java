package com.moby.app;

import androidx.room.*;
import java.util.List;

/**
 * UserDao — Data Access Object untuk Entity User.
 * TODO: Tambah query sesuai kebutuhan.
 */
@Dao
public interface UserDao {

    @Insert
    void insert(User user);

    @Query("SELECT * FROM users")
    List<User> getAll();

    @Delete
    void delete(User user);
}
