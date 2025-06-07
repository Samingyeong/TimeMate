package com.example.timemate;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "friend",
        primaryKeys = {"user_id", "friend_id"})
public class Friend {

    @NonNull
    @ColumnInfo(name = "user_id")
    public String userId;  // 현재 로그인한 사용자 ID

    @NonNull
    @ColumnInfo(name = "friend_id")
    public String friendId;  // 사용자 고유 ID로 친구를 추가할 때 사용

    @ColumnInfo(name = "friend_nickname")
    public String friendNickname;  // 친구 별명

    @ColumnInfo(name = "added_at")
    public long addedAt; // 친구 추가 시간

    @ColumnInfo(name = "status")
    public String status; // "ACTIVE", "BLOCKED"

    public Friend() {
        this.addedAt = System.currentTimeMillis();
        this.status = "ACTIVE";
    }
}
