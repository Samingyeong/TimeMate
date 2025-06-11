package com.example.timemate.data.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.TypeConverters;

import com.example.timemate.data.database.DateConverter;

import java.util.Date;

/**
 * 일정 리마인더 엔티티
 */
@Entity(tableName = "schedule_reminders",
        foreignKeys = {
            @ForeignKey(entity = Schedule.class,
                       parentColumns = "id",
                       childColumns = "scheduleId",
                       onDelete = ForeignKey.CASCADE)
        },
        indices = {@Index("scheduleId")})
@TypeConverters(DateConverter.class)
public class ScheduleReminder {
    
    @PrimaryKey(autoGenerate = true)
    public long id;
    
    public long scheduleId;
    public Date reminderTime;
    public String type; // "notification", "alarm", "email"
    public boolean isTriggered;
    public Date createdAt;
    public Date triggeredAt;
    
    public ScheduleReminder() {}
    
    public ScheduleReminder(long scheduleId, Date reminderTime, String type) {
        this.scheduleId = scheduleId;
        this.reminderTime = reminderTime;
        this.type = type;
        this.isTriggered = false;
        this.createdAt = new Date();
    }
}
