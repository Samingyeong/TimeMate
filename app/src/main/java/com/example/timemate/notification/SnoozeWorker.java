package com.example.timemate.notification;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.timemate.data.database.AppDatabase;
import com.example.timemate.ScheduleReminder;
import com.example.timemate.ScheduleReminderDao;

/**
 * 10분 후 다시 알림을 표시하는 Worker
 */
public class SnoozeWorker extends Worker {

    private static final String TAG = "SnoozeWorker";

    public SnoozeWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            int reminderId = getInputData().getInt("reminder_id", -1);
            
            if (reminderId == -1) {
                Log.e(TAG, "❌ 유효하지 않은 reminder_id");
                return Result.failure();
            }
            
            Log.d(TAG, "🔔 10분 후 알림 재표시: " + reminderId);
            
            // 데이터베이스에서 리마인더 조회
            AppDatabase db = AppDatabase.getInstance(getApplicationContext());
            ScheduleReminderDao reminderDao = db.scheduleReminderDao();
            ScheduleReminder reminder = reminderDao.getReminderByScheduleId(reminderId);
            
            if (reminder == null) {
                Log.w(TAG, "⚠️ 리마인더를 찾을 수 없음: " + reminderId);
                return Result.failure();
            }
            
            // 알림 다시 표시
            ReminderNotificationHelper notificationHelper = new ReminderNotificationHelper(getApplicationContext());
            notificationHelper.createScheduleNotification(reminder);
            
            Log.d(TAG, "✅ 10분 후 알림 재표시 완료");
            return Result.success();
            
        } catch (Exception e) {
            Log.e(TAG, "❌ SnoozeWorker 오류", e);
            e.printStackTrace();
            return Result.failure();
        }
    }
}
