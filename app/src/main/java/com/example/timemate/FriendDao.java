package com.example.timemate;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface FriendDao {

    @Insert
    void insert(Friend friend);

    @Query("SELECT * FROM friend")  // ✅ 정확한 테이블 이름
    List<Friend> getAllFriends();

    @Query("SELECT * FROM friend WHERE user_id = :userId AND status = 'ACTIVE' ORDER BY added_at DESC")
    List<Friend> getFriendsByUserId(String userId);

    @Query("SELECT * FROM friend WHERE friend_id = :friendId AND user_id = :userId")
    Friend getFriendById(String friendId, String userId);

    @Query("SELECT * FROM friend WHERE friend_id = :friendId LIMIT 1")
    Friend getFriendById(String friendId);

}

