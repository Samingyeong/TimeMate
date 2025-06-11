package com.example.timemate;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import androidx.room.Room;

import com.example.timemate.data.database.AppDatabase;

import java.util.concurrent.Executors;

public class NotificationActionReceiver extends BroadcastReceiver {
    
    @Override
    public void onReceive(Context context, Intent intent) {
        int notificationId = intent.getIntExtra("notification_id", -1);
        String action = intent.getStringExtra("action");
        
        if (notificationId == -1 || action == null) {
            return;
        }
        
        AppDatabase db = Room.databaseBuilder(context.getApplicationContext(), 
                AppDatabase.class, "timeMate-db")
                .fallbackToDestructiveMigration()
                .build();
        
        Executors.newSingleThreadExecutor().execute(() -> {
            // 알림 ID로 데이터베이스에서 알림 찾기
            // 실제로는 notificationId와 데이터베이스 ID를 매핑하는 로직이 필요
            
            if ("ACCEPT".equals(action)) {
                // 수락 처리
                handleAcceptInvite(context, db, notificationId);
            } else if ("REJECT".equals(action)) {
                // 거절 처리
                handleRejectInvite(context, db, notificationId);
            }
        });
        
        // 알림 취소
        NotificationService notificationService = new NotificationService(context);
        notificationService.cancelNotification(notificationId);
    }
    
    private void handleAcceptInvite(Context context, AppDatabase db, int notificationId) {
        // 실제 구현에서는 알림 ID로 해당 알림을 찾아서 상태 업데이트
        // 여기서는 간단히 토스트만 표시
        
        // UI 스레드에서 토스트 표시
        android.os.Handler mainHandler = new android.os.Handler(context.getMainLooper());
        mainHandler.post(() -> {
            Toast.makeText(context, "일정 초대를 수락했습니다", Toast.LENGTH_SHORT).show();
        });
    }
    
    private void handleRejectInvite(Context context, AppDatabase db, int notificationId) {
        // 실제 구현에서는 알림 ID로 해당 알림을 찾아서 상태 업데이트
        // 여기서는 간단히 토스트만 표시
        
        // UI 스레드에서 토스트 표시
        android.os.Handler mainHandler = new android.os.Handler(context.getMainLooper());
        mainHandler.post(() -> {
            Toast.makeText(context, "일정 초대를 거절했습니다", Toast.LENGTH_SHORT).show();
        });
    }
}
