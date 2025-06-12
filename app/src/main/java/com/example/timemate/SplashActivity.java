package com.example.timemate;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;

import com.example.timemate.features.home.HomeActivity;
import com.example.timemate.util.UserSession;

/**
 * 스플래시 화면 Activity
 * 앱 시작 시 3초간 표시되는 로딩 화면
 */
public class SplashActivity extends AppCompatActivity {

    private static final String TAG = "SplashActivity";
    private static final int SPLASH_DELAY = 3000; // 3초

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "🚀 SplashActivity 시작");

        try {
            setContentView(R.layout.activity_splash);
            Log.d(TAG, "✅ 스플래시 레이아웃 설정 완료");

            // 3초 후 메인 화면으로 이동
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                Log.d(TAG, "⏰ 3초 경과, 메인 화면으로 이동 시작");
                navigateToMainActivity();
            }, SPLASH_DELAY);

        } catch (Exception e) {
            Log.e(TAG, "❌ SplashActivity onCreate 오류", e);
            // 즉시 메인 화면으로 이동
            navigateToMainActivity();
        }
    }

    private void navigateToMainActivity() {
        try {
            Log.d(TAG, "🔄 메인 화면 전환 시작");

            // 일단 테스트를 위해 무조건 HomeActivity로 이동
            Intent intent = new Intent(this, HomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

            Log.d(TAG, "🚀 HomeActivity로 이동 시작");
            startActivity(intent);
            finish(); // 스플래시 화면 종료

            Log.d(TAG, "🎉 화면 전환 완료");

        } catch (Exception e) {
            Log.e(TAG, "❌ HomeActivity 전환 오류, MainActivity로 폴백", e);
            // 오류 발생 시 기본적으로 MainActivity로 이동
            try {
                Intent intent = new Intent(this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
                Log.d(TAG, "✅ MainActivity로 폴백 완료");
            } catch (Exception fallbackError) {
                Log.e(TAG, "❌ 폴백 화면 전환도 실패", fallbackError);
            }
        }
    }

    @Override
    public void onBackPressed() {
        // 스플래시 화면에서는 뒤로가기 버튼 비활성화
        Log.d(TAG, "뒤로가기 버튼 무시됨");
    }
}
