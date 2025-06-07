package com.example.timemate;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(User user);

    @Query("SELECT * FROM user WHERE userId = :id")
    User getUserById(String id);

    @Query("SELECT * FROM user")
    List<User> getAllUsers();

    @Query("SELECT * FROM user WHERE nickname = :nickname")
    User getUserByNickname(String nickname);

    @Query("SELECT * FROM user WHERE email = :email")
    User getUserByEmail(String email);

    @Query("SELECT * FROM user WHERE userId = :userId AND password = :password")
    User loginUser(String userId, String password);

    @Query("UPDATE user SET is_active = 0 WHERE userId = :userId")
    void deactivateUser(String userId);
}
