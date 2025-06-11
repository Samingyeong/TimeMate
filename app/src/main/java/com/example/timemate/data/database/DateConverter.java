package com.example.timemate.data.database;

import androidx.room.TypeConverter;

import java.util.Date;

/**
 * Room 데이터베이스용 Date 타입 컨버터
 */
public class DateConverter {
    
    @TypeConverter
    public static Date fromTimestamp(Long value) {
        return value == null ? null : new Date(value);
    }
    
    @TypeConverter
    public static Long dateToTimestamp(Date date) {
        return date == null ? null : date.getTime();
    }
}
