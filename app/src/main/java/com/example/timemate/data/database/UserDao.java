package com.example.timemate.data.database;

import androidx.annotation.NonNull;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.timemate.data.model.User;

import java.util.List;

/**
 * 사용자 데이터 접근 객체 (DAO)
 * Room 데이터베이스 쿼리 인터페이스
 */
@Dao
public interface UserDao {

    // 사용자 추가 (중복 시 교체)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(@NonNull User user);

    // 사용자 정보 수정
    @Update
    void update(@NonNull User user);

    // 사용자 삭제
    @Delete
    void delete(@NonNull User user);

    // 사용자 ID로 조회
    @Query("SELECT * FROM users WHERE userId = :userId")
    User getUserById(@NonNull String userId);

    // 닉네임으로 사용자 검색
    @Query("SELECT * FROM users WHERE nickname LIKE '%' || :nickname || '%' AND isActive = 1")
    List<User> searchUsersByNickname(String nickname);

    // 모든 활성 사용자 조회
    @Query("SELECT * FROM users WHERE isActive = 1 ORDER BY lastLoginAt DESC")
    List<User> getAllActiveUsers();

    // 모든 사용자 조회 (비활성 포함)
    @Query("SELECT * FROM users ORDER BY createdAt DESC")
    List<User> getAllUsers();

    // 사용자 존재 여부 확인
    @Query("SELECT COUNT(*) FROM users WHERE userId = :userId")
    int isUserExists(String userId);

    // 닉네임 중복 확인
    @Query("SELECT COUNT(*) FROM users WHERE nickname = :nickname AND userId != :excludeUserId")
    int isNicknameExists(String nickname, String excludeUserId);

    // 사용자 비활성화
    @Query("UPDATE users SET isActive = 0 WHERE userId = :userId")
    void deactivateUser(String userId);

    // 사용자 활성화
    @Query("UPDATE users SET isActive = 1, lastLoginAt = :loginTime WHERE userId = :userId")
    void activateUser(String userId, long loginTime);

    // 마지막 로그인 시간 업데이트
    @Query("UPDATE users SET lastLoginAt = :loginTime WHERE userId = :userId")
    void updateLastLogin(String userId, long loginTime);

    // 프로필 이미지 업데이트
    @Query("UPDATE users SET profileImage = :imagePath WHERE userId = :userId")
    void updateProfileImage(String userId, String imagePath);

    // 닉네임 업데이트
    @Query("UPDATE users SET nickname = :nickname WHERE userId = :userId")
    void updateNickname(String userId, String nickname);

    // 이메일 업데이트
    @Query("UPDATE users SET email = :email WHERE userId = :userId")
    void updateEmail(String userId, String email);

    // 최근 로그인한 사용자들 조회 (N명)
    @Query("SELECT * FROM users WHERE isActive = 1 ORDER BY lastLoginAt DESC LIMIT :limit")
    List<User> getRecentActiveUsers(int limit);

    // 특정 기간 이후 로그인한 사용자들 조회
    @Query("SELECT * FROM users WHERE isActive = 1 AND lastLoginAt > :afterTime ORDER BY lastLoginAt DESC")
    List<User> getUsersLoggedInAfter(long afterTime);

    // 사용자 통계 - 총 사용자 수
    @Query("SELECT COUNT(*) FROM users WHERE isActive = 1")
    int getActiveUserCount();

    // 사용자 통계 - 전체 사용자 수
    @Query("SELECT COUNT(*) FROM users")
    int getTotalUserCount();
}
