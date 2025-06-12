package com.example.timemate;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int SPLASH_DELAY = 3000; // 3초

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            Log.d(TAG, "🚀 MainActivity 시작");

            // 스플래시 화면 설정
            setContentView(R.layout.activity_splash);

            // 3초 후 계정 전환 화면으로 이동
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                navigateToAccountSwitch();
            }, SPLASH_DELAY);

        } catch (Exception e) {
            Log.e(TAG, "❌ MainActivity 오류", e);
            // 오류 발생 시 즉시 계정 전환 화면으로 이동
            navigateToAccountSwitch();
        }
    }

    private void navigateToAccountSwitch() {
        try {
            Log.d(TAG, "🔄 계정 전환 화면으로 이동");

            Intent intent = new Intent(this, AccountSwitchActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();

            Log.d(TAG, "✅ 화면 전환 완료");

        } catch (Exception e) {
            Log.e(TAG, "❌ 화면 전환 오류", e);
            Toast.makeText(this, "앱 시작 중 오류가 발생했습니다", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        // 스플래시 중에는 뒤로가기 버튼 비활성화
        Log.d(TAG, "뒤로가기 버튼 무시됨");
    }
}
