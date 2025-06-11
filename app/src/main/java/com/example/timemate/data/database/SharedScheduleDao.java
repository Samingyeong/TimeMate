package com.example.timemate.data.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.timemate.data.model.SharedSchedule;

import java.util.List;

/**
 * 공유 일정 데이터 접근 객체 (DAO)
 * 친구 초대 및 일정 공유 관련 쿼리
 */
@Dao
public interface SharedScheduleDao {

    // 공유 일정 추가
    @Insert
    long insert(SharedSchedule sharedSchedule);

    // 공유 일정 수정
    @Update
    void update(SharedSchedule sharedSchedule);

    // 공유 일정 삭제
    @Delete
    void delete(SharedSchedule sharedSchedule);

    // ID로 공유 일정 조회
    @Query("SELECT * FROM shared_schedules WHERE id = :id")
    SharedSchedule getSharedScheduleById(int id);

    // 사용자가 받은 일정 초대 조회 (대기 중)
    @Query("SELECT * FROM shared_schedules WHERE invitedUserId = :userId AND status = 'pending' ORDER BY createdAt DESC")
    List<SharedSchedule> getPendingInvitations(String userId);

    // 사용자가 받은 모든 일정 초대 조회
    @Query("SELECT * FROM shared_schedules WHERE invitedUserId = :userId ORDER BY createdAt DESC")
    List<SharedSchedule> getAllInvitations(String userId);

    // 사용자가 보낸 일정 초대 조회
    @Query("SELECT * FROM shared_schedules WHERE creatorUserId = :userId ORDER BY createdAt DESC")
    List<SharedSchedule> getSentInvitations(String userId);

    // 특정 일정의 모든 공유 정보 조회
    @Query("SELECT * FROM shared_schedules WHERE originalScheduleId = :scheduleId ORDER BY createdAt ASC")
    List<SharedSchedule> getSharedSchedulesByScheduleId(int scheduleId);

    // 사용자의 수락된 공유 일정 조회
    @Query("SELECT * FROM shared_schedules WHERE invitedUserId = :userId AND status = 'accepted' ORDER BY date ASC, time ASC")
    List<SharedSchedule> getAcceptedSharedSchedules(String userId);

    // 읽지 않은 알림 수 조회
    @Query("SELECT COUNT(*) FROM shared_schedules WHERE invitedUserId = :userId AND isNotificationRead = 0")
    int getUnreadNotificationCount(String userId);

    // 대기 중인 초대 수 조회
    @Query("SELECT COUNT(*) FROM shared_schedules WHERE invitedUserId = :userId AND status = 'pending'")
    int getPendingInvitationCount(String userId);

    // 일정 초대 상태 업데이트
    @Query("UPDATE shared_schedules SET status = :status, respondedAt = :respondedAt, updatedAt = :updatedAt WHERE id = :id")
    void updateInvitationStatus(int id, String status, long respondedAt, long updatedAt);

    // 알림 읽음 처리
    @Query("UPDATE shared_schedules SET isNotificationRead = 1, updatedAt = :updatedAt WHERE id = :id")
    void markNotificationAsRead(int id, long updatedAt);

    // 알림 발송 처리
    @Query("UPDATE shared_schedules SET isNotificationSent = 1, updatedAt = :updatedAt WHERE id = :id")
    void markNotificationAsSent(int id, long updatedAt);

    // 특정 사용자와의 특정 일정 공유 관계 조회
    @Query("SELECT * FROM shared_schedules WHERE originalScheduleId = :scheduleId AND creatorUserId = :creatorId AND invitedUserId = :invitedId")
    SharedSchedule getSharedScheduleRelation(int scheduleId, String creatorId, String invitedId);

    // 일정 삭제 시 관련 공유 일정 모두 삭제
    @Query("DELETE FROM shared_schedules WHERE originalScheduleId = :scheduleId")
    void deleteSharedSchedulesByScheduleId(int scheduleId);

    // 사용자의 모든 공유 일정 삭제
    @Query("DELETE FROM shared_schedules WHERE creatorUserId = :userId OR invitedUserId = :userId")
    void deleteAllSharedSchedulesByUserId(String userId);

    // 오래된 거절된/만료된 초대 정리 (30일 이상)
    @Query("DELETE FROM shared_schedules WHERE status = 'rejected' AND updatedAt < :cutoffTime")
    void cleanupOldRejectedInvitations(long cutoffTime);

    // 날짜별 공유 일정 조회 (수락된 것만)
    @Query("SELECT * FROM shared_schedules WHERE invitedUserId = :userId AND status = 'accepted' AND date = :date ORDER BY time ASC")
    List<SharedSchedule> getAcceptedSharedSchedulesByDate(String userId, String date);

    // 날짜 범위별 공유 일정 조회 (수락된 것만)
    @Query("SELECT * FROM shared_schedules WHERE invitedUserId = :userId AND status = 'accepted' AND date BETWEEN :startDate AND :endDate ORDER BY date ASC, time ASC")
    List<SharedSchedule> getAcceptedSharedSchedulesByDateRange(String userId, String startDate, String endDate);

    // 친구별 공유 일정 통계
    @Query("SELECT creatorUserId, creatorNickname, COUNT(*) as count FROM shared_schedules WHERE invitedUserId = :userId AND status = 'accepted' GROUP BY creatorUserId ORDER BY count DESC")
    List<FriendScheduleStats> getFriendScheduleStats(String userId);

    // 최근 활동 조회 (최근 7일)
    @Query("SELECT * FROM shared_schedules WHERE (creatorUserId = :userId OR invitedUserId = :userId) AND updatedAt > :sevenDaysAgo ORDER BY updatedAt DESC LIMIT 20")
    List<SharedSchedule> getRecentActivity(String userId, long sevenDaysAgo);

    /**
     * 친구별 일정 통계를 위한 데이터 클래스
     */
    class FriendScheduleStats {
        public String creatorUserId;
        public String creatorNickname;
        public int count;
    }
}
