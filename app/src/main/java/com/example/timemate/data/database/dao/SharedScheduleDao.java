package com.example.timemate.data.database.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.timemate.data.model.SharedSchedule;

import java.util.List;

/**
 * 공유 일정 데이터 접근 객체
 */
@Dao
public interface SharedScheduleDao {
    
    @Insert
    long insert(SharedSchedule sharedSchedule);
    
    @Update
    int update(SharedSchedule sharedSchedule);
    
    @Delete
    int delete(SharedSchedule sharedSchedule);
    
    @Query("SELECT * FROM shared_schedules WHERE id = :id")
    SharedSchedule getSharedScheduleById(long id);
    
    @Query("SELECT * FROM shared_schedules WHERE originalScheduleId = :scheduleId")
    List<SharedSchedule> getSharedSchedulesByScheduleId(long scheduleId);

    @Query("SELECT * FROM shared_schedules WHERE invitedUserId = :userId")
    List<SharedSchedule> getSharedSchedulesByUserId(String userId);

    @Query("SELECT * FROM shared_schedules WHERE originalScheduleId = :scheduleId AND invitedUserId = :userId")
    SharedSchedule getSharedSchedule(long scheduleId, String userId);

    @Query("UPDATE shared_schedules SET status = :status WHERE id = :id")
    int updateStatus(long id, String status);

    @Query("DELETE FROM shared_schedules WHERE originalScheduleId = :scheduleId")
    int deleteByScheduleId(long scheduleId);

    @Query("DELETE FROM shared_schedules WHERE invitedUserId = :userId")
    int deleteByUserId(String userId);
}
