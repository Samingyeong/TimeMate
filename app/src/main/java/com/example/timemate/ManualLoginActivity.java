package com.example.timemate;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.room.Room;

import java.util.concurrent.Executors;

public class ManualLoginActivity extends AppCompatActivity {

    private EditText editUserId, editPassword;
    private AppDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manual_login);

        db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "timeMate-db").build();

        editUserId = findViewById(R.id.editUserId);
        editPassword = findViewById(R.id.editPassword);
        Button btnLogin = findViewById(R.id.btnLogin);
        Button btnGoSignup = findViewById(R.id.btnGoSignup);

        btnLogin.setOnClickListener(v -> {
            String userId = editUserId.getText().toString();
            String password = editPassword.getText().toString();

            Executors.newSingleThreadExecutor().execute(() -> {
                User user = db.userDao().getUserById(userId);
                if (user != null && user.password.equals(password)) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "로그인 성공", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(ManualLoginActivity.this, ScheduleListActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    });
                } else {
                    runOnUiThread(() -> Toast.makeText(this, "아이디 또는 비밀번호가 틀렸습니다", Toast.LENGTH_SHORT).show());
                }
            });
        });

        btnGoSignup.setOnClickListener(v -> {
            Intent intent = new Intent(ManualLoginActivity.this, SignupFormActivity.class);
            startActivity(intent);
        });
    }
}
