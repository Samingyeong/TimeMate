package com.example.timemate;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;

import com.example.timemate.data.database.AppDatabase;
import com.example.timemate.data.model.User;
import com.example.timemate.core.util.UserSession;
import com.example.timemate.features.home.HomeActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class AccountSwitchActivity extends AppCompatActivity {

    private RecyclerView recyclerAccounts;
    private Button btnAddAccount, btnCurrentAccount;
    private TextView textCurrentUser;
    
    private com.example.timemate.data.database.AppDatabase db;
    private UserSession userSession;
    private AccountAdapter adapter;
    private List<com.example.timemate.data.model.User> allUsers = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            Log.d("AccountSwitch", "AccountSwitchActivity 시작");

            // Kakao SDK 초기화
            try {
                com.kakao.sdk.common.KakaoSdk.init(getApplicationContext(), "353da63944cad7ee636e97e681b42185");
                Log.d("AccountSwitch", "Kakao SDK 초기화 완료");
            } catch (Exception e) {
                Log.e("AccountSwitch", "Kakao SDK 초기화 실패", e);
            }

            setContentView(R.layout.activity_account_switch);

            initViews();
            setupDatabase();
            setupUserSession();
            setupClickListeners();

            // UI 렌더링 완료 후 데이터 로드
            recyclerAccounts.post(() -> loadAllUsers());

        } catch (Exception e) {
            Log.e("AccountSwitch", "AccountSwitchActivity 초기화 오류", e);
            e.printStackTrace();

            Toast.makeText(this, "계정 화면 초기화 중 오류가 발생했습니다", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void initViews() {
        recyclerAccounts = findViewById(R.id.recyclerAccounts);
        btnAddAccount = findViewById(R.id.btnAddAccount);
        btnCurrentAccount = findViewById(R.id.btnCurrentAccount);
        textCurrentUser = findViewById(R.id.textCurrentUser);

        // RecyclerView 성능 최적화
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerAccounts.setLayoutManager(layoutManager);
        recyclerAccounts.setHasFixedSize(true); // 크기 고정으로 성능 향상
        recyclerAccounts.setItemViewCacheSize(10); // 뷰 캐시 크기 증가
    }

    private void setupDatabase() {
        try {
            // 앱 데이터 디렉토리 확인 및 생성
            java.io.File dataDir = new java.io.File(getFilesDir(), "database");
            if (!dataDir.exists()) {
                boolean created = dataDir.mkdirs();
                Log.d("AccountSwitch", "데이터베이스 디렉토리 생성: " + created);
            }

            db = com.example.timemate.data.database.AppDatabase.getDatabase(this);
            Log.d("AccountSwitch", "데이터베이스 초기화 완료");
        } catch (Exception e) {
            Log.e("AccountSwitch", "데이터베이스 초기화 실패", e);

            // 데이터베이스 초기화 실패 시 기본 동작 유지
            Toast.makeText(this, "일부 기능이 제한될 수 있습니다", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupUserSession() {
        try {
            userSession = UserSession.getInstance(this);
            updateCurrentUserDisplay();
            Log.d("AccountSwitch", "UserSession 초기화 완료");
        } catch (Exception e) {
            Log.e("AccountSwitch", "UserSession 초기화 실패", e);
        }
    }

    private void updateCurrentUserDisplay() {
        try {
            Log.d("AccountSwitch", "사용자 정보 업데이트 시작");

            if (userSession != null && userSession.isLoggedIn()) {
                String userId = userSession.getCurrentUserId();
                String userName = userSession.getCurrentUserName();

                Log.d("AccountSwitch", "로그인 상태 - userId: " + userId + ", userName: " + userName);

                if (userId != null && !userId.isEmpty()) {
                    String displayName = (userName != null && !userName.isEmpty()) ? userName : "사용자";
                    String currentUser = displayName + " (" + userId + ")";
                    textCurrentUser.setText("현재 로그인: " + currentUser);
                    btnCurrentAccount.setText("현재 계정으로 계속");
                    btnCurrentAccount.setEnabled(true);
                    Log.d("AccountSwitch", "현재 로그인된 사용자 표시: " + currentUser);
                } else {
                    textCurrentUser.setText("로그인 정보 오류");
                    btnCurrentAccount.setText("다시 로그인");
                    btnCurrentAccount.setEnabled(true);
                    Log.w("AccountSwitch", "사용자 ID가 비어있음");
                }
            } else {
                textCurrentUser.setText("로그인되지 않음");
                btnCurrentAccount.setText("로그인 필요");
                btnCurrentAccount.setEnabled(true);
                Log.d("AccountSwitch", "로그인되지 않은 상태");
            }
        } catch (Exception e) {
            Log.e("AccountSwitch", "사용자 정보 표시 오류", e);
            e.printStackTrace();
            textCurrentUser.setText("정보 로드 오류");
            btnCurrentAccount.setText("다시 시도");
            btnCurrentAccount.setEnabled(true);
        }
    }

    private void setupClickListeners() {
        btnAddAccount.setOnClickListener(v -> {
            // 새 계정 생성
            Intent intent = new Intent(this, SignupFormActivity.class);
            startActivity(intent);
        });

        btnCurrentAccount.setOnClickListener(v -> {
            try {
                Log.d("AccountSwitch", "현재 계정 버튼 클릭");

                if (userSession != null && userSession.isLoggedIn()) {
                    String userId = userSession.getCurrentUserId();
                    Log.d("AccountSwitch", "로그인된 사용자: " + userId);

                    // 안전한 홈화면 이동
                    safeStartHomeActivity();

                } else {
                    Log.d("AccountSwitch", "로그인되지 않음, 로그인 화면으로 이동");
                    Intent intent = new Intent(AccountSwitchActivity.this, ManualLoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                }

            } catch (Exception e) {
                Log.e("AccountSwitch", "현재 계정 버튼 오류", e);
                e.printStackTrace();

                // 오류 발생 시 토스트 메시지 표시
                Toast.makeText(AccountSwitchActivity.this,
                    "화면 이동 중 오류가 발생했습니다: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
            }
        });
    }

    private void loadAllUsers() {
        // 로딩 상태 표시 (선택사항)
        // progressBar.setVisibility(View.VISIBLE);

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                List<com.example.timemate.data.model.User> users = db.userDao().getAllActiveUsers();

                runOnUiThread(() -> {
                    // 어댑터가 이미 있으면 데이터만 업데이트
                    if (adapter == null) {
                        allUsers.clear();
                        allUsers.addAll(users);
                        adapter = new AccountAdapter(allUsers, this::onAccountSelected);
                        recyclerAccounts.setAdapter(adapter);
                    } else {
                        // 기존 어댑터 데이터 업데이트
                        allUsers.clear();
                        allUsers.addAll(users);
                        adapter.notifyDataSetChanged();
                    }

                    Log.d("AccountSwitch", "Loaded " + users.size() + " users");
                    // progressBar.setVisibility(View.GONE);
                });
            } catch (Exception e) {
                Log.e("AccountSwitch", "Error loading users", e);
                runOnUiThread(() -> {
                    // progressBar.setVisibility(View.GONE);
                    // 오류 처리
                });
            }
        });
    }

    private void onAccountSelected(com.example.timemate.data.model.User user) {
        String currentUserId = userSession.getCurrentUserId();

        if (user.userId.equals(currentUserId)) {
            Toast.makeText(this, "이미 현재 계정입니다", Toast.LENGTH_SHORT).show();
            return;
        }

        // 비밀번호 입력 다이얼로그 표시
        showPasswordDialog(user);
    }

    private void showPasswordDialog(com.example.timemate.data.model.User user) {
        // 비밀번호 입력 EditText 생성
        EditText editPassword = new EditText(this);
        editPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        editPassword.setHint("비밀번호를 입력하세요");
        editPassword.setPadding(50, 30, 50, 30);

        // 다이얼로그 생성
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("계정 전환")
                .setMessage(user.nickname + " (" + user.userId + ") 계정으로 전환하려면 비밀번호를 입력하세요")
                .setView(editPassword)
                .setPositiveButton("로그인", (dialog, which) -> {
                    String inputPassword = editPassword.getText().toString().trim();

                    if (inputPassword.isEmpty()) {
                        Toast.makeText(this, "비밀번호를 입력해주세요", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // 비밀번호 확인 및 계정 전환
                    verifyPasswordAndSwitch(user, inputPassword);
                })
                .setNegativeButton("취소", null)
                .show();

        // EditText에 포커스 주고 키보드 표시
        editPassword.requestFocus();
    }

    private void verifyPasswordAndSwitch(com.example.timemate.data.model.User user, String inputPassword) {
        // 백그라운드에서 비밀번호 확인
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                // 데이터베이스에서 사용자 정보 다시 조회 (최신 비밀번호 확인)
                com.example.timemate.data.model.User dbUser = db.userDao().getUserById(user.userId);

                if (dbUser == null) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "사용자를 찾을 수 없습니다", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                // 비밀번호 확인
                if (dbUser.password != null && dbUser.password.equals(inputPassword)) {
                    // 비밀번호 일치 - 계정 전환
                    runOnUiThread(() -> {
                        switchToAccount(dbUser);
                    });
                } else {
                    // 비밀번호 불일치
                    runOnUiThread(() -> {
                        Toast.makeText(this, "비밀번호가 올바르지 않습니다", Toast.LENGTH_SHORT).show();
                    });
                }

            } catch (Exception e) {
                Log.e("AccountSwitch", "Password verification error", e);
                runOnUiThread(() -> {
                    Toast.makeText(this, "로그인 중 오류가 발생했습니다", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void switchToAccount(com.example.timemate.data.model.User user) {
        Log.d("AccountSwitch", "Switching from " + userSession.getCurrentUserId() + " to " + user.userId);

        // 현재 세션 완전 로그아웃
        userSession.logout();

        // 새 계정으로 로그인
        userSession.login(user.userId, user.nickname, user.email, true);

        // 디버그 정보 출력
        Log.d("AccountSwitch", "Login successful: " + user.userId);

        Toast.makeText(this, user.nickname + " 계정으로 전환되었습니다", Toast.LENGTH_SHORT).show();

        // 홈화면으로 이동
        safeStartHomeActivity();
    }

    /**
     * 안전한 홈화면 이동
     */
    private void safeStartHomeActivity() {
        try {
            Log.d("AccountSwitch", "홈화면으로 안전한 이동 시작");

            Intent intent = new Intent(this, com.example.timemate.ui.home.HomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

            // Activity 존재 여부 확인
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
                finish();
                Log.d("AccountSwitch", "홈화면 이동 성공");
            } else {
                Log.e("AccountSwitch", "HomeActivity를 찾을 수 없음");
                Toast.makeText(this, "홈화면을 찾을 수 없습니다", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            Log.e("AccountSwitch", "홈화면 이동 중 오류", e);
            e.printStackTrace();

            Toast.makeText(this, "홈화면 이동 중 오류가 발생했습니다", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 현재 사용자 정보만 업데이트 (빠른 UI 업데이트)
        updateCurrentUserDisplay();

        // 사용자 목록은 필요한 경우에만 새로고침
        // onCreate에서 이미 로드했으므로 여기서는 생략
        // 새 계정이 추가된 경우에만 다시 로드하도록 개선 가능
    }
}
