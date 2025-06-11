package com.example.timemate.data.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

/**
 * 친구 관계 데이터 모델
 * Room 데이터베이스 엔티티
 */
@Entity(tableName = "friends")
public class Friend {
    @PrimaryKey(autoGenerate = true)
    public int id;

    @NonNull
    public String userId;        // 현재 사용자 ID
    @NonNull
    public String friendUserId;  // 친구 사용자 ID
    @NonNull
    public String friendNickname; // 친구 닉네임 (캐시)
    public long createdAt;       // 친구 추가 시간
    public boolean isAccepted;   // 친구 요청 수락 여부
    public boolean isBlocked;    // 차단 여부

    // 기본 생성자
    public Friend() {
        this.createdAt = System.currentTimeMillis();
        this.isAccepted = false;
        this.isBlocked = false;
    }

    // 생성자 (Room에서 무시)
    @Ignore
    public Friend(@NonNull String userId, @NonNull String friendUserId, @NonNull String friendNickname) {
        this();
        this.userId = userId;
        this.friendUserId = friendUserId;
        this.friendNickname = friendNickname;
    }

    // 편의 메서드들
    public void acceptFriendRequest() {
        this.isAccepted = true;
    }

    public void blockFriend() {
        this.isBlocked = true;
    }

    public void unblockFriend() {
        this.isBlocked = false;
    }

    public boolean isActiveFriend() {
        return isAccepted && !isBlocked;
    }

    // 호환성을 위한 메서드들
    public String getStatus() {
        if (isBlocked) return "blocked";
        if (isAccepted) return "accepted";
        return "pending";
    }

    public void setStatus(String status) {
        switch (status) {
            case "accepted":
                this.isAccepted = true;
                this.isBlocked = false;
                break;
            case "blocked":
                this.isAccepted = false;
                this.isBlocked = true;
                break;
            case "pending":
            default:
                this.isAccepted = false;
                this.isBlocked = false;
                break;
        }
    }

    public String getFriendName() {
        return friendNickname;
    }

    public void setFriendName(String friendName) {
        this.friendNickname = friendName;
    }

    @Override
    public String toString() {
        return "Friend{" +
                "id=" + id +
                ", userId='" + userId + '\'' +
                ", friendUserId='" + friendUserId + '\'' +
                ", friendNickname='" + friendNickname + '\'' +
                ", isAccepted=" + isAccepted +
                ", isBlocked=" + isBlocked +
                '}';
    }
}
