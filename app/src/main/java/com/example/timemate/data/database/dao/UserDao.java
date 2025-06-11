package com.example.timemate.data.database.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.timemate.data.model.User;

import java.util.List;

/**
 * 사용자 데이터 접근 객체
 */
@Dao
public interface UserDao {
    
    @Insert
    long insert(User user);
    
    @Update
    int update(User user);
    
    @Delete
    int delete(User user);
    
    @Query("SELECT * FROM users WHERE userId = :userId")
    User getUserById(String userId);

    @Query("SELECT * FROM users WHERE userId = :userId AND password = :password")
    User getUserByIdAndPassword(String userId, String password);

    @Query("SELECT * FROM users WHERE email = :email")
    User getUserByEmail(String email);

    @Query("SELECT * FROM users WHERE nickname LIKE :name")
    List<User> getUsersByName(String name);

    @Query("SELECT * FROM users")
    List<User> getAllUsers();

    @Query("DELETE FROM users WHERE userId = :userId")
    int deleteById(String userId);

    @Query("SELECT COUNT(*) FROM users WHERE userId = :userId")
    int checkUserExists(String userId);

    @Query("SELECT * FROM users WHERE isActive = 1")
    List<User> getAllActiveUsers();
}
