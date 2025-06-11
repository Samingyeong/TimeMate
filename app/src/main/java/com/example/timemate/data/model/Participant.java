package com.example.timemate.data.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.TypeConverters;

import com.example.timemate.data.database.DateConverter;

import java.util.Date;

/**
 * 일정 참가자 엔티티
 */
@Entity(tableName = "participants",
        foreignKeys = {
            @ForeignKey(entity = Schedule.class,
                       parentColumns = "id",
                       childColumns = "scheduleId",
                       onDelete = ForeignKey.CASCADE),
            @ForeignKey(entity = User.class,
                       parentColumns = "id", 
                       childColumns = "userId",
                       onDelete = ForeignKey.CASCADE)
        },
        indices = {@Index("scheduleId"), @Index("userId")})
@TypeConverters(DateConverter.class)
public class Participant {
    
    @PrimaryKey(autoGenerate = true)
    public long id;
    
    public long scheduleId;
    public String userId;
    public String status; // "pending", "accepted", "declined"
    public Date invitedAt;
    public Date respondedAt;
    public String invitedBy;
    
    public Participant() {}
    
    public Participant(long scheduleId, String userId, String status) {
        this.scheduleId = scheduleId;
        this.userId = userId;
        this.status = status;
        this.invitedAt = new Date();
    }
}
