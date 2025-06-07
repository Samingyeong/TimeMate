package com.example.timemate;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "notification")
public class Notification {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String type; // "FRIEND_INVITE", "DEPARTURE_REMINDER"
    public String title;
    public String message;
    public String fromUserId; // 초대를 보낸 사용자 ID
    public String scheduleId; // 관련 일정 ID
    public String status; // "PENDING", "ACCEPTED", "REJECTED", "READ"
    public long timestamp;
    public boolean isRead;

    public Notification() {
        this.timestamp = System.currentTimeMillis();
        this.isRead = false;
        this.status = "PENDING";
    }

    @Ignore
    public Notification(String type, String title, String message) {
        this();
        this.type = type;
        this.title = title;
        this.message = message;
    }

    // 친구 초대 알림 생성
    @Ignore
    public static Notification createFriendInvite(String fromUserId, String scheduleTitle, String scheduleId) {
        Notification notification = new Notification();
        notification.type = "FRIEND_INVITE";
        notification.title = "일정 초대";
        notification.message = fromUserId + "님이 '" + scheduleTitle + "' 일정에 초대했습니다";
        notification.fromUserId = fromUserId;
        notification.scheduleId = scheduleId;
        return notification;
    }

    // 출발 알림 생성
    @Ignore
    public static Notification createDepartureReminder(String scheduleTitle, String scheduleId) {
        Notification notification = new Notification();
        notification.type = "DEPARTURE_REMINDER";
        notification.title = "출발 알림";
        notification.message = "'" + scheduleTitle + "' 일정 출발 시간입니다";
        notification.scheduleId = scheduleId;
        notification.status = "READ"; // 출발 알림은 바로 읽음 처리
        return notification;
    }

    // 알림 타입별 아이콘 리소스 ID 반환
    @Ignore
    public int getIconResource() {
        switch (type) {
            case "FRIEND_INVITE":
                return R.drawable.ic_friends;
            case "DEPARTURE_REMINDER":
                return R.drawable.ic_notifications; // ic_schedule이 없으므로 ic_notifications 사용
            default:
                return R.drawable.ic_notifications;
        }
    }

    // 상태별 색상 반환
    @Ignore
    public int getStatusColor() {
        switch (status) {
            case "ACCEPTED":
                return R.color.green;
            case "REJECTED":
                return R.color.red;
            case "PENDING":
                return R.color.orange;
            default:
                return R.color.purple_500;
        }
    }
}
