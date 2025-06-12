package com.example.timemate.data.database.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.timemate.data.model.Friend;

import java.util.List;

/**
 * 친구 데이터 접근 객체
 */
@Dao
public interface FriendDao {
    
    @Insert
    long insert(Friend friend);
    
    @Update
    int update(Friend friend);
    
    @Delete
    int delete(Friend friend);
    
    @Query("SELECT * FROM friends WHERE id = :id")
    Friend getFriendById(long id);
    
    @Query("SELECT * FROM friends WHERE userId = :userId")
    List<Friend> getFriendsByUserId(String userId);
    
    @Query("SELECT * FROM friends WHERE userId = :userId AND friendUserId = :friendUserId")
    Friend getFriendship(String userId, String friendUserId);
    
    @Query("SELECT * FROM friends WHERE userId = :userId AND isAccepted = :isAccepted")
    List<Friend> getFriendsByStatus(String userId, boolean isAccepted);

    @Query("UPDATE friends SET isAccepted = :isAccepted WHERE id = :id")
    int updateFriendStatus(long id, boolean isAccepted);

    @Query("DELETE FROM friends WHERE userId = :userId AND friendUserId = :friendUserId")
    int deleteFriendship(String userId, String friendUserId);

    @Query("SELECT COUNT(*) FROM friends WHERE userId = :userId AND isAccepted = 1")
    int getFriendCount(String userId);

    @Query("SELECT * FROM friends")
    List<Friend> getAllFriends();
}
