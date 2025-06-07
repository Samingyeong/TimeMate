package com.example.timemate;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import androidx.room.Room;

import com.kakao.sdk.common.KakaoSdk;

import java.util.List;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private AppDatabase db;
    private UserSession userSession;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Kakao 초기화
        KakaoSdk.init(getApplicationContext(), "353da63944cad7ee636e97e681b42185");

        // 사용자 세션 초기화
        userSession = new UserSession(this);

        // 개발 단계: 데이터베이스 스키마 문제 해결을 위해 데이터베이스 삭제
        // 실제 배포 시에는 이 부분을 제거해야 합니다
        DatabaseHelper.deleteDatabase(this);

        db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "timeMate-db")
                .fallbackToDestructiveMigration()
                .build();

        // 일정 알림 WorkManager 스케줄링
        ScheduleReminderManager.scheduleReminderWork(this);

        // 계정 전환 화면으로 이동 (모든 계정 표시)
        Intent intent = new Intent(MainActivity.this, AccountSwitchActivity.class);
        startActivity(intent);
        finish();
    }
}
