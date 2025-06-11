package com.example.timemate.data.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

/**
 * 일정 데이터 모델
 * Room 데이터베이스 엔티티
 */
@Entity(tableName = "schedules")
public class Schedule {
    @PrimaryKey(autoGenerate = true)
    public int id;

    @NonNull
    public String userId;        // 사용자 ID
    @NonNull
    public String title;         // 일정 제목
    @NonNull
    public String date;          // 날짜 (yyyy-MM-dd)
    @NonNull
    public String time;          // 시간 (HH:mm)
    @Nullable
    public String departure;     // 출발지
    @Nullable
    public String destination;   // 도착지
    @Nullable
    public String memo;          // 메모
    @Nullable
    public String routeInfo;     // 선택된 경로 정보
    @Nullable
    public String selectedTransportModes; // 선택된 교통수단들 (JSON 형태)
    public boolean isCompleted;  // 완료 여부
    public long createdAt;       // 생성 시간
    public long updatedAt;       // 수정 시간

    // 기본 생성자
    public Schedule() {
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
        this.isCompleted = false;
    }

    // 전체 생성자 (Room에서 무시)
    @Ignore
    public Schedule(@NonNull String userId, @NonNull String title, @NonNull String date, @NonNull String time,
                   @Nullable String departure, @Nullable String destination, @Nullable String memo) {
        this();
        this.userId = userId;
        this.title = title;
        this.date = date;
        this.time = time;
        this.departure = departure;
        this.destination = destination;
        this.memo = memo;
        this.routeInfo = null;
        this.selectedTransportModes = null;
    }

    // 편의 메서드들
    public String getFullDateTime() {
        return date + " " + time;
    }

    public String getRouteInfo() {
        return departure + " → " + destination;
    }

    public void markAsCompleted() {
        this.isCompleted = true;
        this.updatedAt = System.currentTimeMillis();
    }

    public void updateTimestamp() {
        this.updatedAt = System.currentTimeMillis();
    }

    // 호환성을 위한 메서드들
    public java.util.Date getScheduledDate() {
        try {
            java.text.SimpleDateFormat format = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm");
            return format.parse(date + " " + time);
        } catch (Exception e) {
            return new java.util.Date();
        }
    }

    public void setScheduledDate(java.util.Date scheduledDate) {
        if (scheduledDate != null) {
            java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd");
            java.text.SimpleDateFormat timeFormat = new java.text.SimpleDateFormat("HH:mm");
            this.date = dateFormat.format(scheduledDate);
            this.time = timeFormat.format(scheduledDate);
        }
    }

    @Override
    public String toString() {
        return "Schedule{" +
                "id=" + id +
                ", userId='" + userId + '\'' +
                ", title='" + title + '\'' +
                ", date='" + date + '\'' +
                ", time='" + time + '\'' +
                ", departure='" + departure + '\'' +
                ", destination='" + destination + '\'' +
                ", memo='" + memo + '\'' +
                ", isCompleted=" + isCompleted +
                '}';
    }
}
