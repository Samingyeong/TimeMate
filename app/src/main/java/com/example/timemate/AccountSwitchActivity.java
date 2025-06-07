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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class AccountSwitchActivity extends AppCompatActivity {

    private RecyclerView recyclerAccounts;
    private Button btnAddAccount, btnCurrentAccount;
    private TextView textCurrentUser;
    
    private com.example.timemate.data.database.AppDatabase db;
    private com.example.timemate.util.UserSession userSession;
    private AccountAdapter adapter;
    private List<com.example.timemate.data.model.User> allUsers = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_switch);

        initViews();
        setupDatabase();
        setupUserSession();
        setupClickListeners();
        loadAllUsers();
    }

    private void initViews() {
        recyclerAccounts = findViewById(R.id.recyclerAccounts);
        btnAddAccount = findViewById(R.id.btnAddAccount);
        btnCurrentAccount = findViewById(R.id.btnCurrentAccount);
        textCurrentUser = findViewById(R.id.textCurrentUser);
        
        recyclerAccounts.setLayoutManager(new LinearLayoutManager(this));
    }

    private void setupDatabase() {
        db = com.example.timemate.data.database.AppDatabase.getDatabase(this);
    }

    private void setupUserSession() {
        userSession = com.example.timemate.util.UserSession.getInstance(this);
        updateCurrentUserDisplay();
    }

    private void updateCurrentUserDisplay() {
        if (userSession.isLoggedIn()) {
            String currentUser = userSession.getCurrentNickname() + " (" + userSession.getCurrentUserId() + ")";
            textCurrentUser.setText("현재 로그인: " + currentUser);
            btnCurrentAccount.setText("현재 계정으로 계속");
        } else {
            textCurrentUser.setText("로그인되지 않음");
            btnCurrentAccount.setText("로그인 필요");
        }
    }

    private void setupClickListeners() {
        btnAddAccount.setOnClickListener(v -> {
            // 새 계정 생성
            Intent intent = new Intent(this, SignupFormActivity.class);
            startActivity(intent);
        });

        btnCurrentAccount.setOnClickListener(v -> {
            if (userSession.isLoggedIn()) {
                // 현재 계정으로 홈화면 이동
                Intent intent = new Intent(this, com.example.timemate.ui.home.HomeActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            } else {
                // 로그인 화면으로 이동
                Intent intent = new Intent(this, ManualLoginActivity.class);
                startActivity(intent);
            }
        });
    }

    private void loadAllUsers() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<com.example.timemate.data.model.User> users = db.userDao().getAllActiveUsers();

            runOnUiThread(() -> {
                allUsers.clear();
                allUsers.addAll(users);

                adapter = new AccountAdapter(allUsers, this::onAccountSelected);
                recyclerAccounts.setAdapter(adapter);

                Log.d("AccountSwitch", "Loaded " + users.size() + " users");
            });
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
        userSession.login(user.userId, user.nickname);

        // 디버그 정보 출력
        userSession.logSessionInfo();

        Toast.makeText(this, user.nickname + " 계정으로 전환되었습니다", Toast.LENGTH_SHORT).show();

        // 홈화면으로 이동
        Intent intent = new Intent(this, com.example.timemate.ui.home.HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 화면이 다시 보일 때마다 사용자 목록과 현재 사용자 정보 새로고침
        updateCurrentUserDisplay();
        loadAllUsers();
    }
}
