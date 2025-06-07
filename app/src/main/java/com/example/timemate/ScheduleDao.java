package com.example.timemate;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface ScheduleDao {
    @Insert
    void insert(Schedule schedule);

    @Insert
    long insertAndGetId(Schedule schedule);

    @Update
    void update(Schedule schedule);

    @Delete
    void delete(Schedule schedule);

    @Query("SELECT * FROM schedule ORDER BY date_time")
    List<Schedule> getAllSchedules();

    @Query("SELECT * FROM schedule WHERE id = :id")
    Schedule getScheduleById(int id);

    @Query("SELECT * FROM schedule WHERE date_time LIKE :date || '%'")
    List<Schedule> getTodaySchedules(String date);

    @Query("SELECT * FROM schedule WHERE user_id = :userId ORDER BY created_at DESC")
    List<Schedule> getSchedulesByUserId(String userId);

    @Query("SELECT * FROM schedule WHERE user_id = :userId AND is_shared = 1 ORDER BY created_at DESC")
    List<Schedule> getSharedSchedulesByUserId(String userId);

    @Query("UPDATE schedule SET is_shared = 1 WHERE id = :scheduleId")
    void markAsShared(int scheduleId);

    @Query("SELECT * FROM schedule WHERE date_time LIKE :date || '%' ORDER BY date_time ASC")
    List<Schedule> getSchedulesByDate(String date); // "yyyy-MM-dd" 형식

}