package com.example.timemate.data.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * 공유 일정 모델
 * 친구들과 공유된 일정 정보를 저장
 */
@Entity(tableName = "shared_schedules")
public class SharedSchedule {
    
    @PrimaryKey(autoGenerate = true)
    public int id;
    
    // 원본 일정 ID
    public int originalScheduleId;
    
    // 일정 생성자 (초대한 사람)
    public String creatorUserId;
    public String creatorNickname;
    
    // 초대받은 사람
    public String invitedUserId;
    public String invitedNickname;
    
    // 일정 정보 (캐시)
    public String title;
    public String date;
    public String time;
    public String departure;
    public String destination;
    public String memo;
    
    // 상태 정보
    public String status; // "pending", "accepted", "rejected"
    public boolean isNotificationSent; // 알림 발송 여부
    public boolean isNotificationRead; // 알림 읽음 여부
    
    // 타임스탬프
    public long createdAt;
    public long updatedAt;
    public long respondedAt; // 응답한 시간
    
    public SharedSchedule() {
        this.status = "pending";
        this.isNotificationSent = false;
        this.isNotificationRead = false;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }
    
    /**
     * 상태 확인 메서드들
     */
    public boolean isPending() {
        return "pending".equals(status);
    }
    
    public boolean isAccepted() {
        return "accepted".equals(status);
    }
    
    public boolean isRejected() {
        return "rejected".equals(status);
    }
    
    /**
     * 상태 변경 메서드들
     */
    public void accept() {
        this.status = "accepted";
        this.respondedAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }
    
    public void reject() {
        this.status = "rejected";
        this.respondedAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }
    
    /**
     * 알림 관련 메서드들
     */
    public void markNotificationSent() {
        this.isNotificationSent = true;
        this.updatedAt = System.currentTimeMillis();
    }
    
    public void markNotificationRead() {
        this.isNotificationRead = true;
        this.updatedAt = System.currentTimeMillis();
    }
    
    /**
     * 표시용 메서드들
     */
    public String getStatusText() {
        switch (status) {
            case "pending":
                return "대기 중";
            case "accepted":
                return "수락됨";
            case "rejected":
                return "거절됨";
            default:
                return "알 수 없음";
        }
    }
    
    public String getStatusEmoji() {
        switch (status) {
            case "pending":
                return "⏳";
            case "accepted":
                return "✅";
            case "rejected":
                return "❌";
            default:
                return "❓";
        }
    }
    
    public String getDateTimeDisplay() {
        return date + " " + time;
    }
    
    public String getLocationDisplay() {
        if (departure != null && destination != null) {
            return departure + " → " + destination;
        } else if (destination != null) {
            return "📍 " + destination;
        } else {
            return "위치 정보 없음";
        }
    }
    
    @Override
    public String toString() {
        return String.format("%s: %s (%s) - %s", 
                getStatusEmoji(), title, getDateTimeDisplay(), getStatusText());
    }
}
