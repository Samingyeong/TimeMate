package com.example.timemate.worker;

import android.content.Context;
import android.util.Log;

import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

/**
 * 일정 알림 WorkManager 스케줄링 관리자
 */
public class ScheduleReminderManager {

    private static final String TAG = "ScheduleReminderManager";
    private static final String WORK_NAME = "schedule_reminder_work";

    /**
     * 매일 00:05에 실행되는 일정 알림 WorkManager 스케줄링
     */
    public static void scheduleReminderWork(Context context) {
        try {
            Log.d(TAG, "📅 일정 알림 WorkManager 스케줄링 시작");

            // 제약 조건 설정
            Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED) // 네트워크 연결 필요
                .setRequiresBatteryNotLow(true) // 배터리 부족하지 않을 때
                .build();

            // 매일 00:05에 실행되도록 초기 지연 시간 계산
            long initialDelayMinutes = calculateInitialDelay();

            // 24시간마다 반복되는 WorkRequest 생성
            PeriodicWorkRequest reminderWork = new PeriodicWorkRequest.Builder(
                ScheduleWorker.class,
                24, TimeUnit.HOURS, // 24시간마다 반복
                15, TimeUnit.MINUTES // 15분의 flex 시간
            )
            .setInitialDelay(initialDelayMinutes, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build();

            // WorkManager에 등록 (기존 작업이 있으면 교체)
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.REPLACE,
                reminderWork
            );

            Log.d(TAG, "✅ 일정 알림 WorkManager 스케줄링 완료");
            Log.d(TAG, "⏰ 초기 지연: " + initialDelayMinutes + "분 후 첫 실행");
            Log.d(TAG, "🔄 이후 24시간마다 반복 실행");

        } catch (Exception e) {
            Log.e(TAG, "❌ 일정 알림 WorkManager 스케줄링 실패", e);
            e.printStackTrace();
        }
    }

    /**
     * 다음 00:05까지의 초기 지연 시간 계산 (분 단위)
     */
    private static long calculateInitialDelay() {
        Calendar now = Calendar.getInstance();
        Calendar nextRun = Calendar.getInstance();

        // 다음 00:05 시간 설정
        nextRun.set(Calendar.HOUR_OF_DAY, 0);
        nextRun.set(Calendar.MINUTE, 5);
        nextRun.set(Calendar.SECOND, 0);
        nextRun.set(Calendar.MILLISECOND, 0);

        // 현재 시간이 이미 00:05를 지났다면 다음 날 00:05로 설정
        if (now.after(nextRun)) {
            nextRun.add(Calendar.DAY_OF_MONTH, 1);
        }

        long delayMillis = nextRun.getTimeInMillis() - now.getTimeInMillis();
        long delayMinutes = delayMillis / (1000 * 60);

        Log.d(TAG, "현재 시간: " + now.getTime());
        Log.d(TAG, "다음 실행 시간: " + nextRun.getTime());
        Log.d(TAG, "지연 시간: " + delayMinutes + "분");

        return delayMinutes;
    }

    /**
     * 일정 알림 WorkManager 취소
     */
    public static void cancelReminderWork(Context context) {
        try {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME);
            Log.d(TAG, "🔕 일정 알림 WorkManager 취소됨");
        } catch (Exception e) {
            Log.e(TAG, "❌ 일정 알림 WorkManager 취소 실패", e);
            e.printStackTrace();
        }
    }

    /**
     * 즉시 일정 알림 작업 실행 (테스트용)
     */
    public static void runReminderWorkNow(Context context) {
        try {
            Log.d(TAG, "🧪 일정 알림 작업 즉시 실행 (테스트)");

            androidx.work.OneTimeWorkRequest immediateWork = 
                new androidx.work.OneTimeWorkRequest.Builder(ScheduleWorker.class)
                .build();

            WorkManager.getInstance(context).enqueue(immediateWork);

            Log.d(TAG, "✅ 즉시 실행 작업 등록 완료");

        } catch (Exception e) {
            Log.e(TAG, "❌ 즉시 실행 작업 등록 실패", e);
            e.printStackTrace();
        }
    }
}
