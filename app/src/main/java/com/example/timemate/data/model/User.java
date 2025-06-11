package com.example.timemate.data.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

/**
 * 사용자 데이터 모델
 * Room 데이터베이스 엔티티
 */
@Entity(tableName = "users")
public class User {
    @PrimaryKey
    @NonNull
    public String userId;        // 사용자 고유 ID

    @NonNull
    public String nickname;      // 닉네임
    @Nullable
    public String email;         // 이메일 (선택사항)
    @Nullable
    public String password;      // 비밀번호 (계정 전환용)
    @Nullable
    public String profileImage;  // 프로필 이미지 경로
    public long createdAt;       // 계정 생성 시간
    public long lastLoginAt;     // 마지막 로그인 시간
    public boolean isActive;     // 활성 상태

    // 기본 생성자
    public User() {
        this.createdAt = System.currentTimeMillis();
        this.lastLoginAt = System.currentTimeMillis();
        this.isActive = true;
    }

    // 생성자 (Room에서 무시)
    @Ignore
    public User(@NonNull String userId, @NonNull String nickname) {
        this();
        this.userId = userId;
        this.nickname = nickname;
    }

    // 편의 메서드들
    public void updateLastLogin() {
        this.lastLoginAt = System.currentTimeMillis();
    }

    public void deactivate() {
        this.isActive = false;
    }

    public void activate() {
        this.isActive = true;
        updateLastLogin();
    }

    // 호환성을 위한 메서드들
    public String getName() {
        return nickname;
    }

    public void setName(String name) {
        this.nickname = name;
    }

    @Override
    public String toString() {
        return "User{" +
                "userId='" + userId + '\'' +
                ", nickname='" + nickname + '\'' +
                ", email='" + email + '\'' +
                ", isActive=" + isActive +
                '}';
    }
}
