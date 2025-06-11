package com.example.timemate;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            Log.d("MainActivity", "MainActivity 시작");

            // 간단한 화면 전환 - 계정 전환 화면으로 이동
            Intent intent = new Intent(this, AccountSwitchActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();

        } catch (Exception e) {
            Log.e("MainActivity", "MainActivity 오류", e);
            e.printStackTrace();

            // 오류 발생 시 토스트 표시 후 종료
            Toast.makeText(this, "앱 시작 중 오류가 발생했습니다", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

}
