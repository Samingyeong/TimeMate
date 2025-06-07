package com.example.timemate;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class NotificationActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private NotificationAdapter adapter;
    private List<Notification> notifications = new ArrayList<>();
    private AppDatabase db;
    private UserSession userSession;
    private ImageButton btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        userSession = new UserSession(this);
        
        // 로그인 상태 확인
        if (!userSession.isLoggedIn()) {
            finish();
            return;
        }

        initViews();
        setupDatabase();
        setupClickListeners();
        loadNotifications();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerNotifications);
        btnBack = findViewById(R.id.btnBack);
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigationView);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NotificationAdapter(notifications, this::onNotificationAction);
        recyclerView.setAdapter(adapter);

        // 현재 메뉴 선택 표시 (알림함은 별도 메뉴가 없으므로 홈으로 설정)
        bottomNav.setSelectedItemId(R.id.menu_home);

        // 바텀 네비게이션 처리
        bottomNav.setOnItemSelectedListener(new BottomNavigationView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();
                if (id == R.id.menu_recommendation) {
                    startActivity(new Intent(NotificationActivity.this, ScheduleListActivity.class));
                    finish();
                    return true;
                } else if (id == R.id.menu_home) {
                    startActivity(new Intent(NotificationActivity.this, HomeActivity.class));
                    finish();
                    return true;
                } else if (id == R.id.menu_friends) {
                    startActivity(new Intent(NotificationActivity.this, FriendListActivity.class));
                    finish();
                    return true;
                } else if (id == R.id.menu_profile) {
                    startActivity(new Intent(NotificationActivity.this, ProfileActivity.class));
                    finish();
                    return true;
                }
                return false;
            }
        });
    }

    private void setupDatabase() {
        db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "timeMate-db")
                .fallbackToDestructiveMigration()
                .build();
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadNotifications(); // 화면이 다시 보일 때마다 알림 새로고침
    }

    private void loadNotifications() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<Notification> allNotifications = db.notificationDao().getAllNotifications();
            
            runOnUiThread(() -> {
                notifications.clear();
                notifications.addAll(allNotifications);
                adapter.notifyDataSetChanged();
            });
        });
    }

    private void onNotificationAction(Notification notification, String action) {
        switch (action) {
            case "ACCEPT":
                handleInviteResponse(notification, "ACCEPTED");
                break;
            case "REJECT":
                handleInviteResponse(notification, "REJECTED");
                break;
            case "MARK_READ":
                markAsRead(notification);
                break;
            case "DELETE":
                deleteNotification(notification);
                break;
        }
    }

    private void handleInviteResponse(Notification notification, String status) {
        Executors.newSingleThreadExecutor().execute(() -> {
            // 알림 상태 업데이트
            db.notificationDao().updateStatus(notification.id, status);
            db.notificationDao().markAsRead(notification.id);
            
            runOnUiThread(() -> {
                String message = status.equals("ACCEPTED") ? 
                    "일정 초대를 수락했습니다" : "일정 초대를 거절했습니다";
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                loadNotifications(); // 목록 새로고침
            });
        });
    }

    private void markAsRead(Notification notification) {
        Executors.newSingleThreadExecutor().execute(() -> {
            db.notificationDao().markAsRead(notification.id);
            
            runOnUiThread(() -> {
                loadNotifications(); // 목록 새로고침
            });
        });
    }

    private void deleteNotification(Notification notification) {
        Executors.newSingleThreadExecutor().execute(() -> {
            db.notificationDao().delete(notification.id);
            
            runOnUiThread(() -> {
                Toast.makeText(this, "알림이 삭제되었습니다", Toast.LENGTH_SHORT).show();
                loadNotifications(); // 목록 새로고침
            });
        });
    }
}
