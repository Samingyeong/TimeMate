package com.example.timemate;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.room.Room;

import com.example.timemate.data.database.AppDatabase;
import com.example.timemate.data.model.User;
import com.example.timemate.util.UserSession;

import java.util.concurrent.Executors;

public class ManualLoginActivity extends AppCompatActivity {

    private EditText editUserId, editPassword;
    private AppDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manual_login);

        db = AppDatabase.getDatabase(this);

        editUserId = findViewById(R.id.editUserId);
        editPassword = findViewById(R.id.editPassword);
        Button btnLogin = findViewById(R.id.btnLogin);
        Button btnGoSignup = findViewById(R.id.btnGoSignup);
        Button btnCancel = findViewById(R.id.btnCancel);
        TextView textForgotPassword = findViewById(R.id.textForgotPassword);

        // 회원가입 완료 후 이동한 경우 처리
        handleSignupSuccess();

        // 비밀번호 재설정 완료 후 이동한 경우 처리
        handlePasswordResetSuccess();

        btnLogin.setOnClickListener(v -> {
            String userId = editUserId.getText().toString().trim();
            String password = editPassword.getText().toString().trim();

            // 입력 검증
            if (userId.isEmpty()) {
                Toast.makeText(this, "아이디를 입력해주세요", Toast.LENGTH_SHORT).show();
                editUserId.requestFocus();
                return;
            }

            if (password.isEmpty()) {
                Toast.makeText(this, "비밀번호를 입력해주세요", Toast.LENGTH_SHORT).show();
                editPassword.requestFocus();
                return;
            }

            Executors.newSingleThreadExecutor().execute(() -> {
                try {
                    com.example.timemate.data.model.User user = db.userDao().getUserById(userId);
                    if (user != null && user.password != null && user.password.equals(password)) {
                        runOnUiThread(() -> {
                            try {
                                // 로그인 세션 설정
                                com.example.timemate.util.UserSession session = com.example.timemate.util.UserSession.getInstance(this);
                                session.login(user.userId, user.nickname);

                                Toast.makeText(this, "로그인 성공! " + user.nickname + "님 환영합니다.", Toast.LENGTH_SHORT).show();

                                // 홈화면으로 이동
                                Intent intent = new Intent(ManualLoginActivity.this, com.example.timemate.ui.home.HomeActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                finish();
                            } catch (Exception e) {
                                android.util.Log.e("ManualLogin", "로그인 후 화면 전환 오류", e);
                                Toast.makeText(this, "로그인 처리 중 오류가 발생했습니다", Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        runOnUiThread(() -> Toast.makeText(this, "아이디 또는 비밀번호가 틀렸습니다", Toast.LENGTH_SHORT).show());
                    }
                } catch (Exception e) {
                    android.util.Log.e("ManualLogin", "로그인 처리 오류", e);
                    runOnUiThread(() -> Toast.makeText(this, "로그인 중 오류가 발생했습니다", Toast.LENGTH_SHORT).show());
                }
            });
        });

        btnGoSignup.setOnClickListener(v -> {
            Intent intent = new Intent(ManualLoginActivity.this, SignupFormActivity.class);
            startActivity(intent);
        });

        btnCancel.setOnClickListener(v -> {
            // 취소 확인 다이얼로그
            new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("로그인 취소")
                .setMessage("로그인을 취소하고 메인 화면으로 돌아가시겠습니까?")
                .setPositiveButton("취소", (dialog, which) -> {
                    // 메인 화면으로 돌아가기
                    Intent intent = new Intent(ManualLoginActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("계속 로그인", null)
                .show();
        });

        textForgotPassword.setOnClickListener(v -> {
            // 비밀번호 찾기 화면으로 이동
            Intent intent = new Intent(ManualLoginActivity.this, PasswordResetActivity.class);
            startActivity(intent);
        });
    }

    /**
     * 회원가입 완료 후 이동한 경우 처리
     */
    private void handleSignupSuccess() {
        Intent intent = getIntent();
        if (intent != null) {
            boolean signupSuccess = intent.getBooleanExtra("signup_success", false);
            String signupUserId = intent.getStringExtra("signup_user_id");
            String signupPassword = intent.getStringExtra("signup_password");

            if (signupSuccess) {
                // 회원가입 완료 메시지 표시
                Toast.makeText(this, "🎉 회원가입이 완료되었습니다!\n자동으로 로그인합니다.", Toast.LENGTH_LONG).show();

                // 회원가입한 사용자 정보를 로그인 필드에 자동 입력
                if (signupUserId != null && !signupUserId.isEmpty()) {
                    editUserId.setText(signupUserId);

                    if (signupPassword != null && !signupPassword.isEmpty()) {
                        editPassword.setText(signupPassword);

                        // 2초 후 자동 로그인 실행
                        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                            performLogin(signupUserId, signupPassword);
                        }, 2000);
                    } else {
                        editPassword.requestFocus(); // 비밀번호 필드에 포커스
                    }
                }
            }
        }
    }

    /**
     * 비밀번호 재설정 완료 후 처리
     */
    private void handlePasswordResetSuccess() {
        Intent intent = getIntent();
        if (intent != null) {
            boolean resetSuccess = intent.getBooleanExtra("password_reset_success", false);
            String resetUserId = intent.getStringExtra("reset_user_id");
            String resetPassword = intent.getStringExtra("reset_password");

            if (resetSuccess) {
                // 비밀번호 재설정 완료 메시지 표시
                Toast.makeText(this, "🎉 비밀번호가 변경되었습니다!\n새 비밀번호로 자동 로그인합니다.", Toast.LENGTH_LONG).show();

                // 변경된 정보를 로그인 필드에 자동 입력
                if (resetUserId != null && !resetUserId.isEmpty()) {
                    editUserId.setText(resetUserId);

                    if (resetPassword != null && !resetPassword.isEmpty()) {
                        editPassword.setText(resetPassword);

                        // 2초 후 자동 로그인 실행
                        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                            performLogin(resetUserId, resetPassword);
                        }, 2000);
                    } else {
                        editPassword.requestFocus(); // 비밀번호 필드에 포커스
                    }
                }
            }
        }
    }

    /**
     * 로그인 실행 (자동 로그인용)
     */
    private void performLogin(String userId, String password) {
        if (userId.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "아이디와 비밀번호를 입력해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        // 로그인 처리 (기존 로그인 로직과 동일)
        Executors.newSingleThreadExecutor().execute(() -> {
            User user = db.userDao().getUserByIdAndPassword(userId, password);

            runOnUiThread(() -> {
                if (user != null) {
                    // 로그인 성공
                    UserSession.getInstance(ManualLoginActivity.this).login(user.userId, user.nickname);
                    Toast.makeText(this, "✅ 자동 로그인 성공!", Toast.LENGTH_SHORT).show();

                    // 홈 화면으로 이동
                    Intent intent = new Intent(ManualLoginActivity.this, com.example.timemate.ui.home.HomeActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    // 로그인 실패
                    Toast.makeText(this, "❌ 자동 로그인 실패. 수동으로 로그인해주세요.", Toast.LENGTH_SHORT).show();
                    editPassword.requestFocus();
                }
            });
        });
    }

    /**
     * 뒤로가기 버튼 처리
     */
    @Override
    public void onBackPressed() {
        // 취소 확인 다이얼로그
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("로그인 취소")
            .setMessage("로그인을 취소하고 메인 화면으로 돌아가시겠습니까?")
            .setPositiveButton("취소", (dialog, which) -> {
                // 메인 화면으로 돌아가기
                Intent intent = new Intent(ManualLoginActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            })
            .setNegativeButton("계속 로그인", null)
            .show();
    }
}
