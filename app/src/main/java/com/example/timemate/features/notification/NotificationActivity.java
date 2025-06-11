package com.example.timemate.features.notification;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.timemate.R;
import com.example.timemate.adapters.NotificationAdapter;
import com.example.timemate.data.database.AppDatabase;
import com.example.timemate.data.model.SharedSchedule;
import com.example.timemate.util.UserSession;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

/**
 * 알림 목록 화면
 * - 친구 초대 알림
 * - 일정 공유 요청
 * - 시스템 알림
 */
public class NotificationActivity extends AppCompatActivity {
    
    private static final String TAG = "NotificationActivity";
    
    private RecyclerView recyclerNotifications;
    private TextView textEmptyNotifications;
    private NotificationAdapter adapter;
    private UserSession userSession;
    private List<SharedSchedule> notificationList;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "🔔 NotificationActivity 시작");

        try {
            setContentView(R.layout.activity_notification);
            Log.d(TAG, "✅ 레이아웃 설정 완료");

            userSession = UserSession.getInstance(this);
            Log.d(TAG, "✅ UserSession 초기화 완료");

            initBasicViews();
            Log.d(TAG, "✅ 기본 뷰 초기화 완료");

            setupBasicToolbar();
            Log.d(TAG, "✅ 기본 툴바 설정 완료");

            // 알림 로드
            loadNotifications();
            Log.d(TAG, "✅ 알림 로드 시작");

        } catch (Exception e) {
            Log.e(TAG, "❌ NotificationActivity 초기화 오류", e);
            Toast.makeText(this, "알림 화면을 준비 중입니다", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    /**
     * 기본 뷰 초기화
     */
    private void initBasicViews() {
        try {
            recyclerNotifications = findViewById(R.id.recyclerNotifications);
            textEmptyNotifications = findViewById(R.id.textEmptyNotifications);

            if (recyclerNotifications == null) {
                Log.e(TAG, "recyclerNotifications를 찾을 수 없습니다");
                throw new RuntimeException("recyclerNotifications를 찾을 수 없습니다");
            }

            if (textEmptyNotifications == null) {
                Log.e(TAG, "textEmptyNotifications를 찾을 수 없습니다");
                throw new RuntimeException("textEmptyNotifications를 찾을 수 없습니다");
            }

            // RecyclerView 기본 설정
            recyclerNotifications.setLayoutManager(new LinearLayoutManager(this));

            // 어댑터 초기화
            notificationList = new ArrayList<>();
            adapter = new NotificationAdapter(notificationList, this::handleNotificationAction);
            recyclerNotifications.setAdapter(adapter);

            Log.d(TAG, "기본 뷰 초기화 완료");

        } catch (Exception e) {
            Log.e(TAG, "기본 뷰 초기화 오류", e);
            throw e;
        }
    }

    /**
     * 기본 툴바 설정
     */
    private void setupBasicToolbar() {
        try {
            ImageButton btnBack = findViewById(R.id.btnBack);
            if (btnBack != null) {
                btnBack.setOnClickListener(v -> {
                    try {
                        Intent homeIntent = new Intent(this, com.example.timemate.ui.home.HomeActivity.class);
                        startActivity(homeIntent);
                        finish();
                    } catch (Exception e) {
                        Log.e(TAG, "홈으로 이동 오류", e);
                        finish();
                    }
                });
            }

            Log.d(TAG, "기본 툴바 설정 완료");

        } catch (Exception e) {
            Log.e(TAG, "기본 툴바 설정 오류", e);
        }
    }

    /**
     * 빈 상태 표시
     */
    private void showEmptyState() {
        try {
            if (recyclerNotifications != null) {
                recyclerNotifications.setVisibility(View.GONE);
            }
            if (textEmptyNotifications != null) {
                textEmptyNotifications.setVisibility(View.VISIBLE);
            }

            Log.d(TAG, "빈 상태 표시 완료");

        } catch (Exception e) {
            Log.e(TAG, "빈 상태 표시 오류", e);
        }
    }
    
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    /**
     * 알림 목록 로드
     */
    private void loadNotifications() {
        String currentUserId = userSession.getCurrentUserId();
        if (currentUserId == null) {
            showEmptyState();
            return;
        }
        
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                AppDatabase database = AppDatabase.getInstance(this);
                
                // 공유 일정 초대 알림 로드 (pending 상태)
                List<SharedSchedule> pendingInvites = database.sharedScheduleDao()
                    .getSharedSchedulesByUserId(currentUserId);
                
                // pending 상태인 것들만 필터링
                List<SharedSchedule> notifications = new ArrayList<>();
                for (SharedSchedule invite : pendingInvites) {
                    if ("pending".equals(invite.status)) {
                        notifications.add(invite);
                    }
                }
                
                Log.d(TAG, "로드된 알림 수: " + notifications.size());
                
                runOnUiThread(() -> {
                    if (notifications.isEmpty()) {
                        showEmptyState();
                    } else {
                        showNotifications(notifications);
                    }
                });
                
            } catch (Exception e) {
                Log.e(TAG, "알림 로드 오류", e);
                runOnUiThread(() -> {
                    Toast.makeText(this, "알림을 불러오는 중 오류가 발생했습니다", Toast.LENGTH_SHORT).show();
                    showEmptyState();
                });
            }
        });
    }
    
    /**
     * 알림 목록 표시
     */
    private void showNotifications(List<SharedSchedule> notifications) {
        recyclerNotifications.setVisibility(View.VISIBLE);
        textEmptyNotifications.setVisibility(View.GONE);

        if (adapter != null) {
            adapter.updateNotifications(notifications);
        }
    }
    
    /**
     * 알림 액션 처리 (수락/거절)
     */
    private void handleNotificationAction(SharedSchedule notification, String action) {
        Log.d(TAG, "알림 액션: " + action + " for notification ID: " + notification.id);
        
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                AppDatabase database = AppDatabase.getInstance(this);
                
                if ("accept".equals(action)) {
                    // 수락
                    database.sharedScheduleDao().updateStatus(notification.id, "accepted");
                    
                    runOnUiThread(() -> {
                        Toast.makeText(this, "일정 초대를 수락했습니다", Toast.LENGTH_SHORT).show();
                        loadNotifications(); // 목록 새로고침
                    });
                    
                } else if ("reject".equals(action)) {
                    // 거절
                    database.sharedScheduleDao().updateStatus(notification.id, "rejected");
                    
                    runOnUiThread(() -> {
                        Toast.makeText(this, "일정 초대를 거절했습니다", Toast.LENGTH_SHORT).show();
                        loadNotifications(); // 목록 새로고침
                    });
                }
                
            } catch (Exception e) {
                Log.e(TAG, "알림 액션 처리 오류", e);
                runOnUiThread(() -> {
                    Toast.makeText(this, "처리 중 오류가 발생했습니다", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // 화면이 다시 보일 때 알림 목록 새로고침
        loadNotifications();
    }
}
