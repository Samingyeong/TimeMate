package com.example.timemate;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface ScheduleReminderDao {
    
    @Insert
    void insert(ScheduleReminder reminder);

    @Insert
    void insertReminder(ScheduleReminder reminder);
    
    @Update
    void update(ScheduleReminder reminder);
    
    @Delete
    void delete(ScheduleReminder reminder);
    
    @Query("SELECT * FROM schedule_reminder WHERE user_id = :userId AND is_active = 1 ORDER BY appointment_time ASC")
    List<ScheduleReminder> getActiveRemindersByUserId(String userId);
    
    @Query("SELECT * FROM schedule_reminder WHERE schedule_id = :scheduleId")
    ScheduleReminder getReminderByScheduleId(int scheduleId);
    
    @Query("SELECT * FROM schedule_reminder WHERE appointment_time LIKE :date || '%' AND is_active = 1")
    List<ScheduleReminder> getRemindersByDate(String date); // "yyyy-MM-dd" 형식
    
    @Query("SELECT * FROM schedule_reminder WHERE notification_sent = 0 AND is_active = 1")
    List<ScheduleReminder> getPendingNotifications();
    
    @Query("UPDATE schedule_reminder SET notification_sent = 1 WHERE id = :reminderId")
    void markNotificationSent(int reminderId);
    
    @Query("UPDATE schedule_reminder SET is_active = 0 WHERE schedule_id = :scheduleId")
    void deactivateByScheduleId(int scheduleId);
    
    @Query("DELETE FROM schedule_reminder WHERE schedule_id = :scheduleId")
    void deleteByScheduleId(int scheduleId);
    
    @Query("SELECT COUNT(*) FROM schedule_reminder WHERE user_id = :userId AND appointment_time LIKE :tomorrow || '%' AND is_active = 1")
    int getTomorrowRemindersCount(String userId, String tomorrow);
}
