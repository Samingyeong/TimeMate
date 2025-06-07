package com.example.timemate.ui.profile;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.timemate.R;
import com.example.timemate.data.database.AppDatabase;
import com.example.timemate.ui.home.HomeActivity;
import com.example.timemate.ui.friend.FriendListActivity;
import com.example.timemate.ui.recommendation.RecommendationActivity;
import com.example.timemate.util.UserSession;
import com.example.timemate.AccountSwitchActivity;
import com.example.timemate.MainActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.concurrent.Executors;

/**
 * 프로필 화면
 * - 사용자 정보 표시
 * - 계정 전환 기능
 * - 로그아웃 및 계정 삭제
 */
public class ProfileActivity extends AppCompatActivity {

    private TextView textUserName;
    private TextView textUserId;
    private TextView textUserEmail;
    private Button btnSwitchAccount, btnLogout, btnDeleteAccount;
    private AppDatabase db;
    private UserSession userSession;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        userSession = UserSession.getInstance(this);

        // 로그인 상태 확인
        if (!userSession.isLoggedIn()) {
            finish();
            return;
        }

        initViews();
        setupDatabase();
        setupClickListeners();
        setupBottomNavigation();
        loadUserInfo();
    }

    private void initViews() {
        textUserName = findViewById(R.id.textUserName);
        textUserId = findViewById(R.id.textUserId);
        textUserEmail = findViewById(R.id.textUserEmail);
        btnSwitchAccount = findViewById(R.id.btnSwitchAccount);
        btnLogout = findViewById(R.id.btnLogout);
        btnDeleteAccount = findViewById(R.id.btnDeleteAccount);
    }

    private void setupDatabase() {
        db = AppDatabase.getDatabase(this);
    }

    private void setupClickListeners() {
        // 사용자 ID 클릭 시 복사 기능
        textUserId.setOnClickListener(v -> copyUserIdToClipboard());
        textUserId.setOnLongClickListener(v -> {
            copyUserIdToClipboard();
            return true;
        });

        // 계정 전환 버튼 클릭
        btnSwitchAccount.setOnClickListener(v -> {
            Log.d("ProfileActivity", "계정 전환 버튼 클릭됨");
            try {
                Intent intent = new Intent(this, AccountSwitchActivity.class);
                startActivity(intent);
                Log.d("ProfileActivity", "AccountSwitchActivity 시작됨");
            } catch (Exception e) {
                Log.e("ProfileActivity", "계정 전환 화면 시작 실패", e);
                Toast.makeText(this, "계정 전환 화면을 열 수 없습니다: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });

        // 로그아웃 버튼 클릭
        btnLogout.setOnClickListener(v -> showLogoutDialog());

        // 계정 삭제 버튼 클릭
        btnDeleteAccount.setOnClickListener(v -> showDeleteAccountDialog());
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigationView);
        bottomNav.setSelectedItemId(R.id.menu_profile);

        bottomNav.setOnItemSelectedListener(new BottomNavigationView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();
                if (id == R.id.menu_home) {
                    startActivity(new Intent(ProfileActivity.this, HomeActivity.class));
                    finish();
                    return true;
                } else if (id == R.id.menu_friends) {
                    startActivity(new Intent(ProfileActivity.this, FriendListActivity.class));
                    finish();
                    return true;
                } else if (id == R.id.menu_recommendation) {
                    startActivity(new Intent(ProfileActivity.this, RecommendationActivity.class));
                    finish();
                    return true;
                } else if (id == R.id.menu_profile) {
                    return true; // 현재 화면
                }
                return false;
            }
        });
    }

    private void loadUserInfo() {
        String userName = userSession.getCurrentNickname();
        String userId = userSession.getCurrentUserId();

        textUserName.setText(userName != null ? userName : "사용자");
        textUserId.setText(userId != null ? userId : "ID 없음");

        // 데이터베이스에서 사용자 정보 조회하여 이메일 가져오기
        if (userId != null) {
            Executors.newSingleThreadExecutor().execute(() -> {
                try {
                    com.example.timemate.data.model.User user = db.userDao().getUserById(userId);
                    runOnUiThread(() -> {
                        if (user != null && user.email != null && !user.email.isEmpty()) {
                            textUserEmail.setText(user.email);
                        } else {
                            textUserEmail.setText("이메일 정보 없음");
                        }
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> {
                        textUserEmail.setText("이메일 정보 없음");
                    });
                }
            });
        } else {
            textUserEmail.setText("이메일 정보 없음");
        }
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
        userSession.logout();

        // 로그인 화면으로 이동
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();

        Toast.makeText(this, "로그아웃되었습니다.", Toast.LENGTH_SHORT).show();
    }

    private void deleteAccount() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                // 현재 사용자의 모든 데이터 삭제
                String currentUserId = userSession.getCurrentUserId();
                if (currentUserId != null) {
                    db.scheduleDao().deleteAllSchedulesByUserId(currentUserId);
                    db.friendDao().deleteAllFriendRelations(currentUserId);
                    db.userDao().deactivateUser(currentUserId);
                }
                
                runOnUiThread(() -> {
                    // 사용자 세션 클리어
                    userSession.logout();

                    // 로그인 화면으로 이동
                    Intent intent = new Intent(ProfileActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();

                    Toast.makeText(ProfileActivity.this, "계정이 삭제되었습니다.", Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(ProfileActivity.this, "계정 삭제 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
}
