package com.example.timemate.data.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.TypeConverters;

import com.example.timemate.data.database.DateConverter;

import java.util.Date;

/**
 * 알림 엔티티
 */
@Entity(tableName = "notifications",
        foreignKeys = {
            @ForeignKey(entity = User.class,
                       parentColumns = "id",
                       childColumns = "userId",
                       onDelete = ForeignKey.CASCADE)
        },
        indices = {@Index("userId")})
@TypeConverters(DateConverter.class)
public class Notification {
    
    @PrimaryKey(autoGenerate = true)
    public long id;
    
    public String userId;
    public String type; // "friend_request", "schedule_invite", "schedule_reminder"
    public String title;
    public String message;
    public String data; // JSON 형태의 추가 데이터
    public boolean isRead;
    public Date createdAt;
    public Date readAt;
    
    public Notification() {}
    
    public Notification(String userId, String type, String title, String message) {
        this.userId = userId;
        this.type = type;
        this.title = title;
        this.message = message;
        this.isRead = false;
        this.createdAt = new Date();
    }
}
