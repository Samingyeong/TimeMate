package com.example.timemate;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import java.util.List;

import androidx.appcompat.app.AppCompatActivity;
import androidx.room.Room;

import java.util.concurrent.Executors;

public class SignupFormActivity extends AppCompatActivity {

    private EditText editNickname, editEmail, editPhone, editUserId, editPassword;
    private RadioGroup radioGenderGroup;
    private com.example.timemate.data.database.AppDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup_form);

        db = com.example.timemate.data.database.AppDatabase.getDatabase(this);

        editNickname = findViewById(R.id.editNickname);
        editEmail = findViewById(R.id.editEmail);
        editPhone = findViewById(R.id.editPhone);
        editUserId = findViewById(R.id.editUserId);
        editPassword = findViewById(R.id.editPassword);
        radioGenderGroup = findViewById(R.id.radioGenderGroup);

        Button btnSignup = findViewById(R.id.btnSignup);
        btnSignup.setOnClickListener(v -> signupUser());
    }

    private void signupUser() {
        String nickname = editNickname.getText().toString().trim();
        String email = editEmail.getText().toString().trim();
        String phone = editPhone.getText().toString().trim();
        String userId = editUserId.getText().toString().trim();
        String password = editPassword.getText().toString().trim();

        // 입력 검증
        if (nickname.isEmpty()) {
            Toast.makeText(this, "닉네임을 입력해주세요", Toast.LENGTH_SHORT).show();
            editNickname.requestFocus();
            return;
        }

        if (email.isEmpty()) {
            Toast.makeText(this, "이메일을 입력해주세요", Toast.LENGTH_SHORT).show();
            editEmail.requestFocus();
            return;
        }

        if (userId.isEmpty()) {
            Toast.makeText(this, "사용자 ID를 입력해주세요", Toast.LENGTH_SHORT).show();
            editUserId.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            Toast.makeText(this, "비밀번호를 입력해주세요", Toast.LENGTH_SHORT).show();
            editPassword.requestFocus();
            return;
        }

        int selectedGenderId = radioGenderGroup.getCheckedRadioButtonId();
        RadioButton selectedGenderButton = findViewById(selectedGenderId);
        String gender = selectedGenderButton != null ? selectedGenderButton.getText().toString() : "";

        Executors.newSingleThreadExecutor().execute(() -> {
            // 중복 확인
            com.example.timemate.data.model.User existingUser = db.userDao().getUserById(userId);
            // 이메일 중복 확인은 별도 메서드로 처리
            boolean emailExists = isEmailExists(email);

            runOnUiThread(() -> {
                if (existingUser != null) {
                    Toast.makeText(this, "이미 사용 중인 사용자 ID입니다", Toast.LENGTH_SHORT).show();
                    editUserId.requestFocus();
                    return;
                }

                if (emailExists) {
                    Toast.makeText(this, "이미 사용 중인 이메일입니다", Toast.LENGTH_SHORT).show();
                    editEmail.requestFocus();
                    return;
                }

                // 새 사용자 생성
                com.example.timemate.data.model.User newUser = new com.example.timemate.data.model.User(userId, nickname);
                newUser.email = email;
                newUser.password = password;

                Executors.newSingleThreadExecutor().execute(() -> {
                    db.userDao().insert(newUser);

                    runOnUiThread(() -> {
                        Toast.makeText(this, "회원가입 성공!\n사용자 ID: " + userId, Toast.LENGTH_LONG).show();

                        // 자동 로그인 처리
                        com.example.timemate.util.UserSession session = com.example.timemate.util.UserSession.getInstance(this);
                        session.login(newUser.userId, newUser.nickname);

                        // 홈 화면으로 이동
                        Intent intent = new Intent(SignupFormActivity.this, com.example.timemate.ui.home.HomeActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    });
                });
            });
        });
    }

    /**
     * 이메일 중복 확인
     */
    private boolean isEmailExists(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }

        try {
            // 모든 활성 사용자 조회해서 이메일 중복 확인
            List<com.example.timemate.data.model.User> allUsers = db.userDao().getAllActiveUsers();
            for (com.example.timemate.data.model.User user : allUsers) {
                if (email.equals(user.email)) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            Log.e("SignupForm", "Email check error", e);
            return false;
        }
    }
}
