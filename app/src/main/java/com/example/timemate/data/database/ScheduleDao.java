package com.example.timemate.data.database;

import androidx.annotation.NonNull;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.timemate.data.model.Schedule;

import java.util.List;

/**
 * 일정 데이터 접근 객체 (DAO)
 * Room 데이터베이스 쿼리 인터페이스
 */
@Dao
public interface ScheduleDao {

    // 일정 추가
    @Insert
    long insert(@NonNull Schedule schedule);

    // 일정 수정
    @Update
    void update(@NonNull Schedule schedule);

    // 일정 삭제
    @Delete
    void delete(@NonNull Schedule schedule);

    // ID로 일정 조회
    @Query("SELECT * FROM schedules WHERE id = :id")
    Schedule getScheduleById(int id);

    // 사용자별 모든 일정 조회 (최신순)
    @Query("SELECT * FROM schedules WHERE userId = :userId ORDER BY date DESC, time DESC")
    List<Schedule> getSchedulesByUserId(@NonNull String userId);

    // 사용자별 특정 날짜 일정 조회
    @Query("SELECT * FROM schedules WHERE userId = :userId AND date = :date ORDER BY time ASC")
    List<Schedule> getSchedulesByDate(String userId, String date);

    // 사용자별 날짜 범위 일정 조회
    @Query("SELECT * FROM schedules WHERE userId = :userId AND date BETWEEN :startDate AND :endDate ORDER BY date ASC, time ASC")
    List<Schedule> getSchedulesByDateRange(String userId, String startDate, String endDate);

    // 사용자별 완료되지 않은 일정 조회
    @Query("SELECT * FROM schedules WHERE userId = :userId AND isCompleted = 0 ORDER BY date ASC, time ASC")
    List<Schedule> getIncompleteSchedules(String userId);

    // 사용자별 완료된 일정 조회
    @Query("SELECT * FROM schedules WHERE userId = :userId AND isCompleted = 1 ORDER BY date DESC, time DESC")
    List<Schedule> getCompletedSchedules(String userId);

    // 제목으로 일정 검색
    @Query("SELECT * FROM schedules WHERE userId = :userId AND title LIKE '%' || :keyword || '%' ORDER BY date DESC, time DESC")
    List<Schedule> searchSchedulesByTitle(String userId, String keyword);

    // 목적지로 일정 검색
    @Query("SELECT * FROM schedules WHERE userId = :userId AND destination LIKE '%' || :keyword || '%' ORDER BY date DESC, time DESC")
    List<Schedule> searchSchedulesByDestination(String userId, String keyword);

    // 사용자별 일정 개수 조회
    @Query("SELECT COUNT(*) FROM schedules WHERE userId = :userId")
    int getScheduleCount(String userId);

    // 사용자별 완료되지 않은 일정 개수 조회
    @Query("SELECT COUNT(*) FROM schedules WHERE userId = :userId AND isCompleted = 0")
    int getIncompleteScheduleCount(String userId);

    // 모든 일정 삭제 (사용자별)
    @Query("DELETE FROM schedules WHERE userId = :userId")
    void deleteAllSchedulesByUserId(String userId);

    // 특정 날짜 이전의 완료된 일정 삭제 (정리용)
    @Query("DELETE FROM schedules WHERE userId = :userId AND isCompleted = 1 AND date < :beforeDate")
    void deleteOldCompletedSchedules(String userId, String beforeDate);

    // 최근 N개 일정 조회
    @Query("SELECT * FROM schedules WHERE userId = :userId ORDER BY createdAt DESC LIMIT :limit")
    List<Schedule> getRecentSchedules(String userId, int limit);

    // 오늘 일정 조회
    @Query("SELECT * FROM schedules WHERE userId = :userId AND date = date('now', 'localtime') ORDER BY time ASC")
    List<Schedule> getTodaySchedules(String userId);

    // 내일 일정 조회
    @Query("SELECT * FROM schedules WHERE userId = :userId AND date = date('now', '+1 day', 'localtime') ORDER BY time ASC")
    List<Schedule> getTomorrowSchedules(String userId);
}
