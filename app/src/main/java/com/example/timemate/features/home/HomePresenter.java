package com.example.timemate.features.home;

import android.content.Context;
import com.example.timemate.data.database.AppDatabase;
import com.example.timemate.data.model.Schedule;
import com.example.timemate.core.util.UserSession;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 홈 화면 프레젠터
 * 비즈니스 로직과 데이터 처리 담당
 */
public class HomePresenter {

    public interface ScheduleCallback {
        void onSchedulesLoaded(List<Schedule> schedules);
    }

    private Context context;
    private AppDatabase database;
    private UserSession userSession;
    private ExecutorService executor;

    public HomePresenter(Context context) {
        this.context = context;
        this.database = AppDatabase.getDatabase(context);
        this.userSession = UserSession.getInstance(context);
        this.executor = Executors.newSingleThreadExecutor();
    }

    /**
     * 오늘 일정 로드
     */
    public void loadTodaySchedules(ScheduleCallback callback) {
        executor.execute(() -> {
            try {
                String userId = userSession.getCurrentUserId();
                if (userId == null || userId.trim().isEmpty()) {
                    android.util.Log.w("HomePresenter", "사용자 ID가 null - 기본 사용자 사용");
                    userId = "user1"; // 기본 사용자 ID

                    // UserSession에 기본 사용자 정보 설정
                    userSession.login(userId, "사용자1", "user1@test.com", true);
                }

                // 오늘 날짜 범위 계산
                Calendar today = Calendar.getInstance();
                today.set(Calendar.HOUR_OF_DAY, 0);
                today.set(Calendar.MINUTE, 0);
                today.set(Calendar.SECOND, 0);
                today.set(Calendar.MILLISECOND, 0);
                Date startOfDay = today.getTime();

                today.add(Calendar.DAY_OF_MONTH, 1);
                Date startOfNextDay = today.getTime();

                // 오늘 일정 조회 - Date를 String으로 변환
                java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd");
                String startDateStr = dateFormat.format(startOfDay);
                String endDateStr = dateFormat.format(startOfNextDay);

                List<Schedule> schedules = database.scheduleDao()
                    .getSchedulesByUserAndDateRange(userId, startDateStr, endDateStr);

                callback.onSchedulesLoaded(schedules);

            } catch (Exception e) {
                android.util.Log.e("HomePresenter", "Error loading today schedules", e);
                callback.onSchedulesLoaded(List.of());
            }
        });
    }

    /**
     * 내일 일정 로드
     */
    public void loadTomorrowSchedules(ScheduleCallback callback) {
        executor.execute(() -> {
            try {
                String userId = userSession.getCurrentUserId();
                if (userId == null || userId.trim().isEmpty()) {
                    android.util.Log.w("HomePresenter", "사용자 ID가 null - 기본 사용자 사용");
                    userId = "user1"; // 기본 사용자 ID

                    // UserSession에 기본 사용자 정보 설정
                    userSession.login(userId, "사용자1", "user1@test.com", true);
                }

                // 내일 날짜 범위 계산
                Calendar tomorrow = Calendar.getInstance();
                tomorrow.add(Calendar.DAY_OF_MONTH, 1);
                tomorrow.set(Calendar.HOUR_OF_DAY, 0);
                tomorrow.set(Calendar.MINUTE, 0);
                tomorrow.set(Calendar.SECOND, 0);
                tomorrow.set(Calendar.MILLISECOND, 0);
                Date startOfTomorrow = tomorrow.getTime();

                tomorrow.add(Calendar.DAY_OF_MONTH, 1);
                Date startOfDayAfterTomorrow = tomorrow.getTime();

                // 내일 일정 조회 - Date를 String으로 변환
                java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd");
                String startTomorrowStr = dateFormat.format(startOfTomorrow);
                String endTomorrowStr = dateFormat.format(startOfDayAfterTomorrow);

                List<Schedule> schedules = database.scheduleDao()
                    .getSchedulesByUserAndDateRange(userId, startTomorrowStr, endTomorrowStr);

                callback.onSchedulesLoaded(schedules);

            } catch (Exception e) {
                android.util.Log.e("HomePresenter", "Error loading tomorrow schedules", e);
                callback.onSchedulesLoaded(List.of());
            }
        });
    }

    /**
     * 이번 주 일정 통계
     */
    public void loadWeeklyStats(WeeklyStatsCallback callback) {
        executor.execute(() -> {
            try {
                String userId = userSession.getCurrentUserId();
                if (userId == null) {
                    callback.onStatsLoaded(new WeeklyStats());
                    return;
                }

                // 이번 주 시작일과 종료일 계산
                Calendar calendar = Calendar.getInstance();
                calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);
                Date weekStart = calendar.getTime();

                calendar.add(Calendar.WEEK_OF_YEAR, 1);
                Date weekEnd = calendar.getTime();

                // 이번 주 일정 조회 - Date를 String으로 변환
                java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd");
                String weekStartStr = dateFormat.format(weekStart);
                String weekEndStr = dateFormat.format(weekEnd);

                List<Schedule> weeklySchedules = database.scheduleDao()
                    .getSchedulesByUserAndDateRange(userId, weekStartStr, weekEndStr);

                WeeklyStats stats = new WeeklyStats();
                stats.totalSchedules = weeklySchedules.size();
                stats.completedSchedules = (int) weeklySchedules.stream()
                    .filter(schedule -> schedule.isCompleted)
                    .count();

                callback.onStatsLoaded(stats);

            } catch (Exception e) {
                android.util.Log.e("HomePresenter", "Error loading weekly stats", e);
                callback.onStatsLoaded(new WeeklyStats());
            }
        });
    }

    public interface WeeklyStatsCallback {
        void onStatsLoaded(WeeklyStats stats);
    }

    public static class WeeklyStats {
        public int totalSchedules = 0;
        public int completedSchedules = 0;

        public int getCompletionRate() {
            if (totalSchedules == 0) return 0;
            return (completedSchedules * 100) / totalSchedules;
        }
    }

    public void destroy() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
}
