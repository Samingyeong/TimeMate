package com.example.timemate;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "schedule")
public class Schedule {
    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "title")
    public String title;

    @ColumnInfo(name = "date_time")
    public String dateTime;

    @ColumnInfo(name = "departure")
    public String departure;

    @ColumnInfo(name = "destination")
    public String destination;

    @ColumnInfo(name = "memo")
    public String memo;

    @ColumnInfo(name = "user_id")
    public String userId; // 일정 생성자 ID

    @ColumnInfo(name = "is_shared")
    public boolean isShared; // 공유 일정 여부

    @ColumnInfo(name = "created_at")
    public long createdAt;

    public Schedule() {
        this.createdAt = System.currentTimeMillis();
        this.isShared = false;
    }
}
