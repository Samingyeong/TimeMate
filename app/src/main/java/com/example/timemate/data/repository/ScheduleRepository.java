package com.example.timemate.data.repository;

import android.content.Context;
import com.example.timemate.data.database.AppDatabase;
import com.example.timemate.data.database.dao.ScheduleDao;
import com.example.timemate.data.database.dao.SharedScheduleDao;
import com.example.timemate.data.model.Schedule;
import com.example.timemate.data.model.SharedSchedule;
import com.example.timemate.core.util.UserSession;

import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 일정 데이터 리포지토리
 * 일정 관련 모든 데이터 작업을 중앙 관리
 */
public class ScheduleRepository {

    private static ScheduleRepository instance;
    
    private AppDatabase database;
    private ScheduleDao scheduleDao;
    private SharedScheduleDao sharedScheduleDao;
    private UserSession userSession;
    private ExecutorService executor;

    private ScheduleRepository(Context context) {
        database = AppDatabase.getDatabase(context);
        scheduleDao = database.scheduleDao();
        sharedScheduleDao = database.sharedScheduleDao();
        userSession = UserSession.getInstance(context);
        executor = Executors.newSingleThreadExecutor();
    }

    public static synchronized ScheduleRepository getInstance(Context context) {
        if (instance == null) {
            instance = new ScheduleRepository(context.getApplicationContext());
        }
        return instance;
    }

    /**
     * 일정 생성
     */
    public void createSchedule(Schedule schedule, CreateScheduleCallback callback) {
        executor.execute(() -> {
            try {
                String currentUserId = userSession.getCurrentUserId();
                if (currentUserId == null) {
                    callback.onError("로그인이 필요합니다.");
                    return;
                }

                schedule.userId = currentUserId;
                schedule.createdAt = System.currentTimeMillis();
                schedule.updatedAt = System.currentTimeMillis();

                long scheduleId = scheduleDao.insert(schedule);
                schedule.id = (int) scheduleId;

                callback.onSuccess(schedule);

            } catch (Exception e) {
                android.util.Log.e("ScheduleRepository", "Error creating schedule", e);
                callback.onError("일정 생성 중 오류가 발생했습니다.");
            }
        });
    }

    /**
     * 일정 수정
     */
    public void updateSchedule(Schedule schedule, UpdateScheduleCallback callback) {
        executor.execute(() -> {
            try {
                schedule.updatedAt = System.currentTimeMillis();
                int updatedRows = scheduleDao.update(schedule);
                
                if (updatedRows > 0) {
                    callback.onSuccess(schedule);
                } else {
                    callback.onError("일정을 찾을 수 없습니다.");
                }

            } catch (Exception e) {
                android.util.Log.e("ScheduleRepository", "Error updating schedule", e);
                callback.onError("일정 수정 중 오류가 발생했습니다.");
            }
        });
    }

    /**
     * 일정 삭제
     */
    public void deleteSchedule(long scheduleId, DeleteScheduleCallback callback) {
        executor.execute(() -> {
            try {
                // 공유 일정도 함께 삭제
                sharedScheduleDao.deleteByScheduleId(scheduleId);
                
                int deletedRows = scheduleDao.deleteById(scheduleId);
                
                if (deletedRows > 0) {
                    callback.onSuccess();
                } else {
                    callback.onError("일정을 찾을 수 없습니다.");
                }

            } catch (Exception e) {
                android.util.Log.e("ScheduleRepository", "Error deleting schedule", e);
                callback.onError("일정 삭제 중 오류가 발생했습니다.");
            }
        });
    }

    /**
     * 사용자의 모든 일정 조회
     */
    public void getUserSchedules(GetSchedulesCallback callback) {
        executor.execute(() -> {
            try {
                String currentUserId = userSession.getCurrentUserId();
                if (currentUserId == null) {
                    callback.onSuccess(List.of());
                    return;
                }

                List<Schedule> schedules = scheduleDao.getSchedulesByUserId(currentUserId);
                callback.onSuccess(schedules);

            } catch (Exception e) {
                android.util.Log.e("ScheduleRepository", "Error getting user schedules", e);
                callback.onError("일정 조회 중 오류가 발생했습니다.");
            }
        });
    }

    /**
     * 날짜 범위별 일정 조회
     */
    public void getSchedulesByDateRange(Date startDate, Date endDate, GetSchedulesCallback callback) {
        executor.execute(() -> {
            try {
                String currentUserId = userSession.getCurrentUserId();
                if (currentUserId == null) {
                    callback.onSuccess(List.of());
                    return;
                }

                // Date를 String으로 변환
                java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd");
                String startDateStr = dateFormat.format(startDate);
                String endDateStr = dateFormat.format(endDate);

                List<Schedule> schedules = scheduleDao.getSchedulesByUserAndDateRange(
                    currentUserId, startDateStr, endDateStr);
                callback.onSuccess(schedules);

            } catch (Exception e) {
                android.util.Log.e("ScheduleRepository", "Error getting schedules by date range", e);
                callback.onError("일정 조회 중 오류가 발생했습니다.");
            }
        });
    }

    /**
     * 오늘 일정 조회
     */
    public void getTodaySchedules(GetSchedulesCallback callback) {
        java.util.Calendar today = java.util.Calendar.getInstance();
        today.set(java.util.Calendar.HOUR_OF_DAY, 0);
        today.set(java.util.Calendar.MINUTE, 0);
        today.set(java.util.Calendar.SECOND, 0);
        today.set(java.util.Calendar.MILLISECOND, 0);
        Date startOfDay = today.getTime();

        today.add(java.util.Calendar.DAY_OF_MONTH, 1);
        Date startOfNextDay = today.getTime();

        getSchedulesByDateRange(startOfDay, startOfNextDay, callback);
    }

    /**
     * 내일 일정 조회
     */
    public void getTomorrowSchedules(GetSchedulesCallback callback) {
        java.util.Calendar tomorrow = java.util.Calendar.getInstance();
        tomorrow.add(java.util.Calendar.DAY_OF_MONTH, 1);
        tomorrow.set(java.util.Calendar.HOUR_OF_DAY, 0);
        tomorrow.set(java.util.Calendar.MINUTE, 0);
        tomorrow.set(java.util.Calendar.SECOND, 0);
        tomorrow.set(java.util.Calendar.MILLISECOND, 0);
        Date startOfTomorrow = tomorrow.getTime();

        tomorrow.add(java.util.Calendar.DAY_OF_MONTH, 1);
        Date startOfDayAfterTomorrow = tomorrow.getTime();

        getSchedulesByDateRange(startOfTomorrow, startOfDayAfterTomorrow, callback);
    }

    /**
     * 일정 완료 상태 변경
     */
    public void markScheduleCompleted(long scheduleId, boolean completed, UpdateScheduleCallback callback) {
        executor.execute(() -> {
            try {
                Schedule schedule = scheduleDao.getScheduleById(scheduleId);
                if (schedule == null) {
                    callback.onError("일정을 찾을 수 없습니다.");
                    return;
                }

                schedule.isCompleted = completed;
                schedule.updatedAt = System.currentTimeMillis();
                
                scheduleDao.update(schedule);
                callback.onSuccess(schedule);

            } catch (Exception e) {
                android.util.Log.e("ScheduleRepository", "Error updating schedule completion", e);
                callback.onError("일정 상태 변경 중 오류가 발생했습니다.");
            }
        });
    }

    /**
     * 공유 일정 생성
     */
    public void shareSchedule(long scheduleId, List<String> friendIds, ShareScheduleCallback callback) {
        executor.execute(() -> {
            try {
                for (String friendId : friendIds) {
                    SharedSchedule sharedSchedule = new SharedSchedule();
                    sharedSchedule.originalScheduleId = (int) scheduleId;
                    sharedSchedule.invitedUserId = friendId;
                    sharedSchedule.status = "pending";
                    sharedSchedule.createdAt = System.currentTimeMillis();

                    sharedScheduleDao.insert(sharedSchedule);
                }

                callback.onSuccess();

            } catch (Exception e) {
                android.util.Log.e("ScheduleRepository", "Error sharing schedule", e);
                callback.onError("일정 공유 중 오류가 발생했습니다.");
            }
        });
    }

    // 콜백 인터페이스들
    public interface CreateScheduleCallback {
        void onSuccess(Schedule schedule);
        void onError(String error);
    }

    public interface UpdateScheduleCallback {
        void onSuccess(Schedule schedule);
        void onError(String error);
    }

    public interface DeleteScheduleCallback {
        void onSuccess();
        void onError(String error);
    }

    public interface GetSchedulesCallback {
        void onSuccess(List<Schedule> schedules);
        void onError(String error);
    }

    public interface ShareScheduleCallback {
        void onSuccess();
        void onError(String error);
    }

    public void shutdown() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
}
