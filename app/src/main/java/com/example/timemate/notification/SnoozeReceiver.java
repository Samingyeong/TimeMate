package com.example.timemate.notification;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

/**
 * 10분 후 다시 알림 기능을 위한 BroadcastReceiver
 */
public class SnoozeReceiver extends BroadcastReceiver {

    private static final String TAG = "SnoozeReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            int reminderId = intent.getIntExtra("reminder_id", -1);
            
            if (reminderId == -1) {
                Log.e(TAG, "❌ 유효하지 않은 reminder_id");
                return;
            }
            
            Log.d(TAG, "⏰ 10분 후 알림 예약: " + reminderId);
            
            // 현재 알림 취소
            ReminderNotificationHelper notificationHelper = new ReminderNotificationHelper(context);
            notificationHelper.cancelNotification(reminderId);
            
            // 10분 후 다시 알림을 위한 WorkRequest 생성
            OneTimeWorkRequest snoozeWork = new OneTimeWorkRequest.Builder(SnoozeWorker.class)
                .setInitialDelay(10, TimeUnit.MINUTES)
                .setInputData(
                    new androidx.work.Data.Builder()
                        .putInt("reminder_id", reminderId)
                        .build()
                )
                .build();
            
            WorkManager.getInstance(context).enqueue(snoozeWork);
            
            Log.d(TAG, "✅ 10분 후 알림 예약 완료");
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Snooze 처리 오류", e);
            e.printStackTrace();
        }
    }
}
