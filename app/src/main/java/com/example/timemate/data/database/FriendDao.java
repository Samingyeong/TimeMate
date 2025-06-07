package com.example.timemate.data.database;

import androidx.annotation.NonNull;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.timemate.data.model.Friend;

import java.util.List;

/**
 * 친구 관계 데이터 접근 객체 (DAO)
 * Room 데이터베이스 쿼리 인터페이스
 */
@Dao
public interface FriendDao {

    // 친구 관계 추가
    @Insert
    long insert(@NonNull Friend friend);

    // 친구 관계 수정
    @Update
    void update(@NonNull Friend friend);

    // 친구 관계 삭제
    @Delete
    void delete(@NonNull Friend friend);

    // ID로 친구 관계 조회
    @Query("SELECT * FROM friends WHERE id = :id")
    Friend getFriendById(int id);

    // 사용자의 모든 친구 조회 (수락된 친구만)
    @Query("SELECT * FROM friends WHERE userId = :userId AND isAccepted = 1 AND isBlocked = 0 ORDER BY friendNickname ASC")
    List<Friend> getFriendsByUserId(@NonNull String userId);

    // 사용자의 친구 요청 조회 (받은 요청)
    @Query("SELECT * FROM friends WHERE friendUserId = :userId AND isAccepted = 0 AND isBlocked = 0 ORDER BY createdAt DESC")
    List<Friend> getFriendRequestsReceived(String userId);

    // 사용자의 친구 요청 조회 (보낸 요청)
    @Query("SELECT * FROM friends WHERE userId = :userId AND isAccepted = 0 AND isBlocked = 0 ORDER BY createdAt DESC")
    List<Friend> getFriendRequestsSent(String userId);

    // 특정 친구 관계 조회
    @Query("SELECT * FROM friends WHERE userId = :userId AND friendUserId = :friendUserId")
    Friend getFriendRelation(String userId, String friendUserId);

    // 친구 관계 존재 여부 확인 (양방향)
    @Query("SELECT COUNT(*) FROM friends WHERE (userId = :userId AND friendUserId = :friendUserId) OR (userId = :friendUserId AND friendUserId = :userId)")
    int isFriendRelationExists(String userId, String friendUserId);

    // 친구 여부 확인 (수락된 관계만)
    @Query("SELECT COUNT(*) FROM friends WHERE userId = :userId AND friendUserId = :friendUserId AND isAccepted = 1 AND isBlocked = 0")
    int isFriend(String userId, String friendUserId);

    // 차단된 친구 조회
    @Query("SELECT * FROM friends WHERE userId = :userId AND isBlocked = 1 ORDER BY friendNickname ASC")
    List<Friend> getBlockedFriends(String userId);

    // 친구 요청 수락
    @Query("UPDATE friends SET isAccepted = 1 WHERE userId = :fromUserId AND friendUserId = :toUserId")
    void acceptFriendRequest(String fromUserId, String toUserId);

    // 친구 차단
    @Query("UPDATE friends SET isBlocked = 1 WHERE userId = :userId AND friendUserId = :friendUserId")
    void blockFriend(String userId, String friendUserId);

    // 친구 차단 해제
    @Query("UPDATE friends SET isBlocked = 0 WHERE userId = :userId AND friendUserId = :friendUserId")
    void unblockFriend(String userId, String friendUserId);

    // 친구 닉네임 업데이트 (캐시 갱신)
    @Query("UPDATE friends SET friendNickname = :newNickname WHERE friendUserId = :friendUserId")
    void updateFriendNickname(String friendUserId, String newNickname);

    // 사용자의 친구 수 조회
    @Query("SELECT COUNT(*) FROM friends WHERE userId = :userId AND isAccepted = 1 AND isBlocked = 0")
    int getFriendCount(String userId);

    // 사용자의 대기 중인 친구 요청 수 조회 (받은 요청)
    @Query("SELECT COUNT(*) FROM friends WHERE friendUserId = :userId AND isAccepted = 0 AND isBlocked = 0")
    int getPendingFriendRequestCount(String userId);

    // 친구 관계 완전 삭제 (양방향)
    @Query("DELETE FROM friends WHERE (userId = :userId AND friendUserId = :friendUserId) OR (userId = :friendUserId AND friendUserId = :userId)")
    void deleteFriendRelation(String userId, String friendUserId);

    // 사용자의 모든 친구 관계 삭제
    @Query("DELETE FROM friends WHERE userId = :userId OR friendUserId = :userId")
    void deleteAllFriendRelations(String userId);

    // 닉네임으로 친구 검색
    @Query("SELECT * FROM friends WHERE userId = :userId AND friendNickname LIKE '%' || :nickname || '%' AND isAccepted = 1 AND isBlocked = 0 ORDER BY friendNickname ASC")
    List<Friend> searchFriendsByNickname(String userId, String nickname);

    // 최근 추가된 친구들 조회
    @Query("SELECT * FROM friends WHERE userId = :userId AND isAccepted = 1 AND isBlocked = 0 ORDER BY createdAt DESC LIMIT :limit")
    List<Friend> getRecentFriends(String userId, int limit);
}
