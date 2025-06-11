package com.example.timemate.notification;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.example.timemate.R;
import com.example.timemate.ScheduleReminder;
import com.example.timemate.ScheduleReminderDetailActivity;

/**
 * 일정 알림 생성 및 관리 헬퍼
 */
public class ReminderNotificationHelper {

    private static final String TAG = "ReminderNotification";
    private static final String CHANNEL_ID = "schedule_reminder";
    private static final String CHANNEL_NAME = "일정 알림";
    private static final String CHANNEL_DESCRIPTION = "내일 일정의 출발 시간 알림";

    private Context context;
    private NotificationManager notificationManager;

    public ReminderNotificationHelper(Context context) {
        this.context = context;
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        createNotificationChannel();
    }

    /**
     * 알림 채널 생성 (Android 8.0 이상)
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription(CHANNEL_DESCRIPTION);
            channel.enableLights(true);
            channel.enableVibration(true);
            channel.setLightColor(android.graphics.Color.BLUE);
            channel.setVibrationPattern(new long[]{0, 1000, 500, 1000});

            notificationManager.createNotificationChannel(channel);
            Log.d(TAG, "✅ 알림 채널 생성됨: " + CHANNEL_ID);
        }
    }

    /**
     * 일정 알림 생성
     */
    public void createScheduleNotification(ScheduleReminder reminder) {
        try {
            Log.d(TAG, "🔔 알림 생성 시작: " + reminder.title);

            // 알림 제목: "내일 '{제목}' 약속, {출발시각} 출발하세요!"
            String title = String.format("내일 '%s' 약속, %s 출발하세요!", 
                                       reminder.title, reminder.recommendedDepartureTime);

            // 알림 내용
            String content = String.format("%s → %s\n예상 %d분 소요 (%s)",
                                         reminder.departure, reminder.destination,
                                         reminder.durationMinutes, reminder.getTransportDisplayName());

            // 상세보기 Intent
            Intent detailIntent = new Intent(context, ScheduleReminderDetailActivity.class);
            detailIntent.putExtra("reminder_id", reminder.id);
            detailIntent.putExtra("title", reminder.title);
            detailIntent.putExtra("departure", reminder.departure);
            detailIntent.putExtra("destination", reminder.destination);
            detailIntent.putExtra("appointment_time", reminder.appointmentTime);
            detailIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

            PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                reminder.id,
                detailIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            // 알림 빌드
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_schedule_notification) // 알림 아이콘
                .setContentTitle(title)
                .setContentText(content)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(content))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .addAction(R.drawable.ic_directions, "길찾기", pendingIntent)
                .addAction(R.drawable.ic_snooze, "10분 후", createSnoozeIntent(reminder))
                .setColor(context.getResources().getColor(R.color.primary_color, null));

            // 알림 표시
            notificationManager.notify(reminder.id, builder.build());

            Log.d(TAG, "✅ 알림 생성 완료: " + reminder.title);

        } catch (Exception e) {
            Log.e(TAG, "❌ 알림 생성 오류", e);
            e.printStackTrace();
        }
    }

    /**
     * 10분 후 다시 알림 Intent 생성
     */
    private PendingIntent createSnoozeIntent(ScheduleReminder reminder) {
        Intent snoozeIntent = new Intent(context, SnoozeReceiver.class);
        snoozeIntent.putExtra("reminder_id", reminder.id);
        
        return PendingIntent.getBroadcast(
            context,
            reminder.id + 1000, // 다른 ID 사용
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    /**
     * 알림 취소
     */
    public void cancelNotification(int reminderId) {
        try {
            notificationManager.cancel(reminderId);
            Log.d(TAG, "🔕 알림 취소됨: " + reminderId);
        } catch (Exception e) {
            Log.e(TAG, "❌ 알림 취소 오류", e);
        }
    }

    /**
     * 모든 알림 취소
     */
    public void cancelAllNotifications() {
        try {
            notificationManager.cancelAll();
            Log.d(TAG, "🔕 모든 알림 취소됨");
        } catch (Exception e) {
            Log.e(TAG, "❌ 모든 알림 취소 오류", e);
        }
    }
}
