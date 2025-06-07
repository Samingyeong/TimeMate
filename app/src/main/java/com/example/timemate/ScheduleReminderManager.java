package com.example.timemate;

import android.content.Context;
import android.util.Log;

import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public class ScheduleReminderManager {
    
    private static final String TAG = "ScheduleReminderManager";
    private static final String WORK_NAME = "schedule_reminder_work";
    
    /**
     * 매일 00:05에 실행되는 일정 알림 작업을 스케줄링합니다.
     */
    public static void scheduleReminderWork(Context context) {
        Log.d(TAG, "Scheduling reminder work");
        
        // 네트워크 연결이 필요한 제약 조건
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        
        // 24시간마다 반복되는 작업 요청
        PeriodicWorkRequest reminderWork = new PeriodicWorkRequest.Builder(
                ScheduleReminderWorker.class, 
                24, TimeUnit.HOURS)
                .setConstraints(constraints)
                .setInitialDelay(calculateInitialDelay(), TimeUnit.MILLISECONDS)
                .build();
        
        // 기존 작업이 있으면 교체
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.REPLACE,
                reminderWork
        );
        
        Log.d(TAG, "Reminder work scheduled successfully");
    }
    
    /**
     * 다음 00:05까지의 지연 시간을 계산합니다.
     */
    private static long calculateInitialDelay() {
        Calendar now = Calendar.getInstance();
        Calendar nextRun = Calendar.getInstance();
        
        // 다음 실행 시간을 00:05로 설정
        nextRun.set(Calendar.HOUR_OF_DAY, 0);
        nextRun.set(Calendar.MINUTE, 5);
        nextRun.set(Calendar.SECOND, 0);
        nextRun.set(Calendar.MILLISECOND, 0);
        
        // 현재 시간이 이미 00:05를 지났다면 다음 날로 설정
        if (nextRun.before(now)) {
            nextRun.add(Calendar.DAY_OF_MONTH, 1);
        }
        
        long delay = nextRun.getTimeInMillis() - now.getTimeInMillis();
        
        Log.d(TAG, "Initial delay calculated: " + delay + " ms (" + 
            (delay / (1000 * 60 * 60)) + " hours)");
        
        return delay;
    }
    
    /**
     * 일정 알림 작업을 취소합니다.
     */
    public static void cancelReminderWork(Context context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME);
        Log.d(TAG, "Reminder work cancelled");
    }
    
    /**
     * 즉시 일정 알림 작업을 실행합니다 (테스트용).
     */
    public static void runReminderWorkNow(Context context) {
        Log.d(TAG, "Running reminder work immediately");
        
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        
        androidx.work.OneTimeWorkRequest immediateWork = 
            new androidx.work.OneTimeWorkRequest.Builder(ScheduleReminderWorker.class)
                .setConstraints(constraints)
                .build();
        
        WorkManager.getInstance(context).enqueue(immediateWork);
    }
}
