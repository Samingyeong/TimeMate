package com.example.timemate;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.timemate.data.database.AppDatabase;
import com.example.timemate.data.model.User;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.concurrent.Executors;

/**
 * 비밀번호 찾기/재설정 액티비티
 * 1단계: 아이디 입력 및 사용자 확인
 * 2단계: 새 비밀번호 입력 및 변경
 */
public class PasswordResetActivity extends AppCompatActivity {

    private TextInputEditText editUserId, editNewPassword, editConfirmPassword;
    private TextInputLayout layoutNewPassword, layoutConfirmPassword;
    private LinearLayout layoutUserInfo;
    private TextView textUserInfo;
    private Button btnConfirm, btnCancel;
    
    private AppDatabase db;
    private User foundUser;
    private boolean isUserVerified = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_password_reset);

        db = AppDatabase.getDatabase(this);
        
        initViews();
        setupClickListeners();
    }

    private void initViews() {
        editUserId = findViewById(R.id.editUserId);
        editNewPassword = findViewById(R.id.editNewPassword);
        editConfirmPassword = findViewById(R.id.editConfirmPassword);
        layoutNewPassword = findViewById(R.id.layoutNewPassword);
        layoutConfirmPassword = findViewById(R.id.layoutConfirmPassword);
        layoutUserInfo = findViewById(R.id.layoutUserInfo);
        textUserInfo = findViewById(R.id.textUserInfo);
        btnConfirm = findViewById(R.id.btnConfirm);
        btnCancel = findViewById(R.id.btnCancel);
    }

    private void setupClickListeners() {
        btnConfirm.setOnClickListener(v -> {
            if (!isUserVerified) {
                verifyUser();
            } else {
                changePassword();
            }
        });

        btnCancel.setOnClickListener(v -> {
            showCancelDialog();
        });
    }

    /**
     * 1단계: 사용자 확인
     */
    private void verifyUser() {
        String userId = editUserId.getText().toString().trim();
        
        if (userId.isEmpty()) {
            Toast.makeText(this, "아이디를 입력해주세요", Toast.LENGTH_SHORT).show();
            editUserId.requestFocus();
            return;
        }

        // 사용자 검색
        Executors.newSingleThreadExecutor().execute(() -> {
            User user = db.userDao().getUserById(userId);
            
            runOnUiThread(() -> {
                if (user != null) {
                    // 사용자 찾음
                    foundUser = user;
                    showUserInfo(user);
                    showPasswordChangeStep();
                } else {
                    // 사용자 없음
                    Toast.makeText(this, "❌ 존재하지 않는 아이디입니다", Toast.LENGTH_SHORT).show();
                    editUserId.requestFocus();
                }
            });
        });
    }

    /**
     * 사용자 정보 표시
     */
    private void showUserInfo(User user) {
        String userInfo = "👤 닉네임: " + (user.nickname != null ? user.nickname : "없음") + "\n" +
                         "📧 이메일: " + (user.email != null ? user.email : "없음") + "\n" +
                         "🆔 사용자 ID: " + user.userId;

        textUserInfo.setText(userInfo);
        layoutUserInfo.setVisibility(View.VISIBLE);
    }

    /**
     * 2단계: 비밀번호 변경 단계로 전환
     */
    private void showPasswordChangeStep() {
        isUserVerified = true;
        
        // UI 변경
        editUserId.setEnabled(false); // 아이디 입력 비활성화
        layoutNewPassword.setVisibility(View.VISIBLE);
        layoutConfirmPassword.setVisibility(View.VISIBLE);
        btnConfirm.setText("비밀번호 변경");
        
        // 새 비밀번호 입력에 포커스
        editNewPassword.requestFocus();
        
        Toast.makeText(this, "✅ 사용자 확인 완료! 새 비밀번호를 입력해주세요", Toast.LENGTH_LONG).show();
    }

    /**
     * 2단계: 비밀번호 변경
     */
    private void changePassword() {
        String newPassword = editNewPassword.getText().toString().trim();
        String confirmPassword = editConfirmPassword.getText().toString().trim();
        
        // 입력 검증
        if (newPassword.isEmpty()) {
            Toast.makeText(this, "새 비밀번호를 입력해주세요", Toast.LENGTH_SHORT).show();
            editNewPassword.requestFocus();
            return;
        }
        
        if (newPassword.length() < 4) {
            Toast.makeText(this, "비밀번호는 4자 이상 입력해주세요", Toast.LENGTH_SHORT).show();
            editNewPassword.requestFocus();
            return;
        }
        
        if (confirmPassword.isEmpty()) {
            Toast.makeText(this, "비밀번호 확인을 입력해주세요", Toast.LENGTH_SHORT).show();
            editConfirmPassword.requestFocus();
            return;
        }
        
        if (!newPassword.equals(confirmPassword)) {
            Toast.makeText(this, "비밀번호가 일치하지 않습니다", Toast.LENGTH_SHORT).show();
            editConfirmPassword.requestFocus();
            return;
        }

        // 비밀번호 변경 실행
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                foundUser.password = newPassword;
                db.userDao().update(foundUser);
                
                runOnUiThread(() -> {
                    Toast.makeText(this, "🎉 비밀번호가 성공적으로 변경되었습니다!", Toast.LENGTH_LONG).show();
                    
                    // 로그인 화면으로 이동 (변경된 정보와 함께)
                    Intent intent = new Intent(PasswordResetActivity.this, ManualLoginActivity.class);
                    intent.putExtra("reset_user_id", foundUser.userId);
                    intent.putExtra("reset_password", newPassword);
                    intent.putExtra("password_reset_success", true);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                });
                
            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "❌ 비밀번호 변경 중 오류가 발생했습니다", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    /**
     * 취소 확인 다이얼로그
     */
    private void showCancelDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("비밀번호 찾기 취소")
            .setMessage("비밀번호 찾기를 취소하고 로그인 화면으로 돌아가시겠습니까?")
            .setPositiveButton("취소", (dialog, which) -> {
                Intent intent = new Intent(PasswordResetActivity.this, ManualLoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            })
            .setNegativeButton("계속 진행", null)
            .show();
    }

    /**
     * 뒤로가기 버튼 처리
     */
    @Override
    public void onBackPressed() {
        showCancelDialog();
    }
}
