package com.example.timemate;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "user")
public class User {
    @PrimaryKey
    @NonNull
    public String userId;

    @ColumnInfo(name = "nickname")
    public String nickname;

    @ColumnInfo(name = "profileImage")
    public String profileImage;

    @ColumnInfo(name = "email")
    public String email;

    @ColumnInfo(name = "gender")
    public String gender;

    @ColumnInfo(name = "phone")
    public String phone;

    @ColumnInfo(name = "password")
    public String password; // 🔐 수동 로그인용 비밀번호 추가

    @ColumnInfo(name = "created_at")
    public long createdAt;

    @ColumnInfo(name = "is_active")
    public boolean isActive;

    public User() {
        this.createdAt = System.currentTimeMillis();
        this.isActive = true;
    }

    @Ignore
    public User(String userId, String nickname, String email) {
        this();
        this.userId = userId;
        this.nickname = nickname;
        this.email = email;
    }

    // 고유 사용자 ID 생성 (닉네임 기반)
    @Ignore
    public static String generateUserId(String nickname) {
        return nickname.toLowerCase().replaceAll("[^a-z0-9]", "") +
               System.currentTimeMillis() % 10000;
    }
}
