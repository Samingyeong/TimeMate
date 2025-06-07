package com.example.timemate;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
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
    
    private AppDatabase db;
    private UserSession userSession;
    private AccountAdapter adapter;
    private List<User> allUsers = new ArrayList<>();

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
        db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "timeMate-db")
                .fallbackToDestructiveMigration()
                .build();
    }

    private void setupUserSession() {
        userSession = new UserSession(this);
        updateCurrentUserDisplay();
    }

    private void updateCurrentUserDisplay() {
        if (userSession.isLoggedIn()) {
            String currentUser = userSession.getCurrentUserNickname() + " (" + userSession.getCurrentUserId() + ")";
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
                Intent intent = new Intent(this, HomeActivity.class);
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
            List<User> users = db.userDao().getAllUsers();
            
            runOnUiThread(() -> {
                allUsers.clear();
                allUsers.addAll(users);
                
                adapter = new AccountAdapter(allUsers, this::onAccountSelected);
                recyclerAccounts.setAdapter(adapter);
                
                Log.d("AccountSwitch", "Loaded " + users.size() + " users");
            });
        });
    }

    private void onAccountSelected(User user) {
        String currentUserId = userSession.getCurrentUserId();
        
        if (user.userId.equals(currentUserId)) {
            Toast.makeText(this, "이미 현재 계정입니다", Toast.LENGTH_SHORT).show();
            return;
        }

        // 계정 전환 확인 다이얼로그
        new AlertDialog.Builder(this)
                .setTitle("계정 전환")
                .setMessage(user.nickname + " (" + user.userId + ") 계정으로 전환하시겠습니까?")
                .setPositiveButton("전환", (dialog, which) -> {
                    switchToAccount(user);
                })
                .setNegativeButton("취소", null)
                .show();
    }

    private void switchToAccount(User user) {
        Log.d("AccountSwitch", "Switching from " + userSession.getCurrentUserId() + " to " + user.userId);
        
        // 현재 세션 완전 로그아웃
        userSession.logoutUser();
        
        // 새 계정으로 로그인
        userSession.createLoginSession(user);
        
        // 디버그 정보 출력
        userSession.debugCurrentUser();
        
        Toast.makeText(this, user.nickname + " 계정으로 전환되었습니다", Toast.LENGTH_SHORT).show();
        
        // 홈화면으로 이동
        Intent intent = new Intent(this, HomeActivity.class);
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
