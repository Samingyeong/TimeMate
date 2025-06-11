package com.example.timemate.features.profile;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.timemate.R;
import com.example.timemate.data.database.AppDatabase;
import com.example.timemate.data.model.User;
import com.example.timemate.util.UserSession;
import com.example.timemate.features.home.HomeActivity;
import com.example.timemate.features.schedule.ScheduleListActivity;
import com.example.timemate.features.friend.FriendListActivity;
import com.example.timemate.AccountSwitchActivity;
import com.example.timemate.ui.recommendation.RecommendationActivity;
import com.example.timemate.MainActivity;
import com.example.timemate.utils.NavigationHelper;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 프로필 화면
 */
public class ProfileActivity extends AppCompatActivity {

    private static final String TAG = "ProfileActivity";
    
    private AppDatabase database;
    private UserSession userSession;
    private ExecutorService executor;
    
    private TextView textUserName;
    private TextView textUserId;
    private TextView textUserEmail;
    private TextView textScheduleCount;
    private TextView textFriendCount;
    private Button btnSwitchAccount;
    private Button btnLogout;
    private Button btnDeleteAccount;
    private BottomNavigationView bottomNavigation;

    // 경로 설정 UI
    private RadioGroup radioGroupPriority;
    private Switch switchRealtimeData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        initViews();
        initServices();
        setupBottomNavigation();
        setupClickListeners();
        setupRouteSettings();

        // 로그인 상태 확인
        if (!userSession.isLoggedIn()) {
            finish();
            return;
        }

        loadUserInfo();
        loadUserStats();
    }

    private void initViews() {
        textUserName = findViewById(R.id.textUserName);
        textUserId = findViewById(R.id.textUserId);
        textUserEmail = findViewById(R.id.textUserEmail);
        // 통계 정보 뷰들 (레이아웃에 없으면 null이 됨)
        // textScheduleCount = findViewById(R.id.textScheduleCount);
        // textFriendCount = findViewById(R.id.textFriendCount);
        btnSwitchAccount = findViewById(R.id.btnSwitchAccount);
        btnLogout = findViewById(R.id.btnLogout);
        btnDeleteAccount = findViewById(R.id.btnDeleteAccount);
        bottomNavigation = findViewById(R.id.bottomNavigationView);

        // 경로 설정 UI
        radioGroupPriority = findViewById(R.id.radioGroupPriority);
        switchRealtimeData = findViewById(R.id.switchRealtimeData);

        // 뒤로가기 버튼
        ImageButton btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                try {
                    Log.d("ProfileActivity", "뒤로가기 버튼 클릭");
                    onBackPressed();
                } catch (Exception e) {
                    Log.e("ProfileActivity", "뒤로가기 오류", e);
                    finish();
                }
            });
        }
    }

    private void initServices() {
        database = AppDatabase.getDatabase(this);
        userSession = UserSession.getInstance(this);
        executor = Executors.newSingleThreadExecutor();
    }

    private void setupBottomNavigation() {
        try {
            Log.d(TAG, "공통 네비게이션 헬퍼 사용");
            NavigationHelper.setupBottomNavigation(this, R.id.nav_profile);
        } catch (Exception e) {
            Log.e(TAG, "바텀 네비게이션 설정 오류", e);
        }
    }

    private void setupClickListeners() {
        // 사용자 ID 클릭 시 복사 기능
        textUserId.setOnClickListener(v -> copyUserIdToClipboard());
        textUserId.setOnLongClickListener(v -> {
            copyUserIdToClipboard();
            return true;
        });

        // 계정 전환 버튼
        btnSwitchAccount.setOnClickListener(v -> {
            Log.d(TAG, "계정 전환 버튼 클릭됨");
            try {
                Intent intent = new Intent(this, AccountSwitchActivity.class);
                startActivity(intent);
            } catch (Exception e) {
                Log.e(TAG, "계정 전환 화면 시작 실패", e);
                Toast.makeText(this, "계정 전환 화면을 열 수 없습니다", Toast.LENGTH_SHORT).show();
            }
        });

        // 로그아웃 버튼
        btnLogout.setOnClickListener(v -> showLogoutDialog());

        // 계정 삭제 버튼
        btnDeleteAccount.setOnClickListener(v -> showDeleteAccountDialog());
    }

    private void setupRouteSettings() {
        // 현재 설정 불러오기
        String currentPriority = userSession.getRoutePriority();
        boolean realtimeEnabled = userSession.isRealtimeDataEnabled();

        // UI에 현재 설정 반영
        if ("cost".equals(currentPriority)) {
            radioGroupPriority.check(R.id.radioCostPriority);
        } else {
            radioGroupPriority.check(R.id.radioTimePriority);
        }
        switchRealtimeData.setChecked(realtimeEnabled);

        // 우선순위 변경 리스너
        radioGroupPriority.setOnCheckedChangeListener((group, checkedId) -> {
            String newPriority = (checkedId == R.id.radioCostPriority) ? "cost" : "time";
            userSession.setRoutePriority(newPriority);

            String priorityText = "cost".equals(newPriority) ? "비용 우선" : "시간 우선";
            Toast.makeText(this, "경로 우선순위가 " + priorityText + "로 변경되었습니다", Toast.LENGTH_SHORT).show();

            Log.d(TAG, "경로 우선순위 변경: " + newPriority);
        });

        // 실시간 데이터 설정 리스너
        switchRealtimeData.setOnCheckedChangeListener((buttonView, isChecked) -> {
            userSession.setRealtimeDataEnabled(isChecked);

            String statusText = isChecked ? "활성화" : "비활성화";
            Toast.makeText(this, "실시간 교통정보가 " + statusText + "되었습니다", Toast.LENGTH_SHORT).show();

            Log.d(TAG, "실시간 데이터 설정 변경: " + isChecked);
        });
    }

    private void loadUserInfo() {
        String userName = userSession.getCurrentUserName();
        String userId = userSession.getCurrentUserId();
        String userEmail = userSession.getCurrentUserEmail();

        textUserName.setText(userName != null ? userName : "사용자");
        textUserId.setText(userId != null ? userId : "ID 없음");
        textUserEmail.setText(userEmail != null && !userEmail.isEmpty() ? userEmail : "이메일 없음");
    }

    private void loadUserStats() {
        executor.execute(() -> {
            try {
                String currentUserId = userSession.getCurrentUserId();
                if (currentUserId == null) return;

                // 일정 개수 조회
                int scheduleCount = database.scheduleDao().getScheduleCountByUserId(currentUserId);
                int completedScheduleCount = database.scheduleDao().getCompletedScheduleCountByUserId(currentUserId);
                
                // 친구 개수 조회
                int friendCount = database.friendDao().getFriendCount(currentUserId);

                runOnUiThread(() -> {
                    if (textScheduleCount != null) {
                        textScheduleCount.setText(String.format("📅 총 %d개 (완료: %d개)", scheduleCount, completedScheduleCount));
                    }
                    if (textFriendCount != null) {
                        textFriendCount.setText(String.format("👥 %d명", friendCount));
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "Error loading user stats", e);
                runOnUiThread(() -> {
                    if (textScheduleCount != null) {
                        textScheduleCount.setText("📅 정보 없음");
                    }
                    if (textFriendCount != null) {
                        textFriendCount.setText("👥 정보 없음");
                    }
                });
            }
        });
    }

    private void copyUserIdToClipboard() {
        String userId = userSession.getCurrentUserId();
        if (userId != null && !userId.isEmpty()) {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("사용자 ID", userId);
            clipboard.setPrimaryClip(clip);

            Toast.makeText(this, "📋 사용자 ID가 복사되었습니다: " + userId, Toast.LENGTH_SHORT).show();
        }
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("로그아웃")
                .setMessage("정말 로그아웃 하시겠습니까?")
                .setPositiveButton("로그아웃", (dialog, which) -> logout())
                .setNegativeButton("취소", null)
                .show();
    }

    private void showDeleteAccountDialog() {
        new AlertDialog.Builder(this)
                .setTitle("계정 삭제")
                .setMessage("계정을 삭제하면 모든 데이터가 삭제됩니다. 정말 삭제하시겠습니까?")
                .setPositiveButton("삭제", (dialog, which) -> deleteAccount())
                .setNegativeButton("취소", null)
                .show();
    }

    private void logout() {
        // 사용자 세션 클리어
        userSession.logoutUser();

        // 로그인 화면으로 이동
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();

        Toast.makeText(this, "로그아웃되었습니다", Toast.LENGTH_SHORT).show();
    }

    private void deleteAccount() {
        executor.execute(() -> {
            try {
                String currentUserId = userSession.getCurrentUserId();
                if (currentUserId != null) {
                    // 사용자 관련 데이터 삭제
                    database.scheduleDao().deleteByUserId(currentUserId);
                    database.friendDao().deleteFriendship(currentUserId, currentUserId);
                    database.userDao().deleteById(currentUserId);
                }
                
                runOnUiThread(() -> {
                    // 사용자 세션 클리어
                    userSession.logoutUser();

                    // 로그인 화면으로 이동
                    Intent intent = new Intent(ProfileActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();

                    Toast.makeText(ProfileActivity.this, "계정이 삭제되었습니다", Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                Log.e(TAG, "Error deleting account", e);
                runOnUiThread(() -> 
                    Toast.makeText(ProfileActivity.this, "계정 삭제 중 오류가 발생했습니다", Toast.LENGTH_SHORT).show());
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUserInfo();
        loadUserStats();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
}
