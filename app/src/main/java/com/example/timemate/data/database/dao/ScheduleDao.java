package com.example.timemate.data.database.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import androidx.lifecycle.LiveData;

import com.example.timemate.data.model.Schedule;

import java.util.Date;
import java.util.List;

/**
 * 일정 데이터 접근 객체
 */
@Dao
public interface ScheduleDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Schedule schedule);
    
    @Update
    int update(Schedule schedule);
    
    @Delete
    int delete(Schedule schedule);
    
    @Query("SELECT * FROM schedules WHERE id = :id")
    Schedule getScheduleById(long id);
    
    @Query("SELECT * FROM schedules WHERE userId = :userId ORDER BY date ASC, time ASC")
    List<Schedule> getSchedulesByUserId(String userId);

    // 오늘 이후 일정만 조회 (과거 일정 숨김)
    @Query("SELECT * FROM schedules WHERE userId = :userId AND date >= :todayDate ORDER BY date ASC, time ASC")
    List<Schedule> getSchedulesByUserIdFromToday(String userId, String todayDate);

    @Query("SELECT * FROM schedules WHERE userId = :userId AND date BETWEEN :startDate AND :endDate ORDER BY date ASC, time ASC")
    List<Schedule> getSchedulesByUserAndDateRange(String userId, String startDate, String endDate);

    @Query("SELECT * FROM schedules WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC, time ASC")
    List<Schedule> getSchedulesByDateRange(String startDate, String endDate);

    @Query("SELECT * FROM schedules WHERE date = :targetDate ORDER BY date ASC, time ASC")
    List<Schedule> getSchedulesByDate(String targetDate);

    @Query("SELECT * FROM schedules WHERE userId = :userId AND date = :targetDate ORDER BY date ASC, time ASC")
    List<Schedule> getSchedulesByDate(String userId, String targetDate);

    @Query("SELECT * FROM schedules ORDER BY date ASC, time ASC")
    List<Schedule> getAllSchedules();

    @Query("SELECT * FROM schedules WHERE userId = :userId ORDER BY date ASC, time ASC")
    LiveData<List<Schedule>> getSchedulesByUserIdLive(String userId);
    
    @Query("UPDATE schedules SET isCompleted = :completed WHERE id = :id")
    int updateScheduleCompletion(long id, boolean completed);
    
    @Query("DELETE FROM schedules WHERE id = :id")
    int deleteById(long id);
    
    @Query("DELETE FROM schedules WHERE userId = :userId")
    int deleteByUserId(String userId);
    
    @Query("SELECT COUNT(*) FROM schedules WHERE userId = :userId")
    int getScheduleCountByUserId(String userId);
    
    @Query("SELECT COUNT(*) FROM schedules WHERE userId = :userId AND isCompleted = 1")
    int getCompletedScheduleCountByUserId(String userId);
}
