package com.example.timemate;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.room.Room;

import java.util.concurrent.Executors;

public class ScheduleReminderDetailActivity extends AppCompatActivity {

    private TextView textTitle, textDateTime, textRoute, textDuration, textDepartureTime;
    private TextView textTransport, textDistance, textTollFare, textFuelPrice;
    private Button btnStartNavigation, btnSnooze, btnDismiss;
    private ImageButton btnBack;
    
    private AppDatabase db;
    private ScheduleReminder reminder;
    private int reminderId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule_reminder_detail);

        reminderId = getIntent().getIntExtra("reminder_id", -1);
        if (reminderId == -1) {
            finish();
            return;
        }

        initViews();
        setupDatabase();
        loadReminderData();
        setupClickListeners();
    }

    private void initViews() {
        textTitle = findViewById(R.id.textTitle);
        textDateTime = findViewById(R.id.textDateTime);
        textRoute = findViewById(R.id.textRoute);
        textDuration = findViewById(R.id.textDuration);
        textDepartureTime = findViewById(R.id.textDepartureTime);
        textTransport = findViewById(R.id.textTransport);
        textDistance = findViewById(R.id.textDistance);
        textTollFare = findViewById(R.id.textTollFare);
        textFuelPrice = findViewById(R.id.textFuelPrice);
        
        btnStartNavigation = findViewById(R.id.btnStartNavigation);
        btnSnooze = findViewById(R.id.btnSnooze);
        btnDismiss = findViewById(R.id.btnDismiss);
        btnBack = findViewById(R.id.btnBack);
    }

    private void setupDatabase() {
        db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "timeMate-db")
                .fallbackToDestructiveMigration()
                .build();
    }

    private void loadReminderData() {
        Executors.newSingleThreadExecutor().execute(() -> {
            reminder = db.scheduleReminderDao().getReminderByScheduleId(reminderId);
            
            runOnUiThread(() -> {
                if (reminder != null) {
                    displayReminderData();
                } else {
                    Toast.makeText(this, "알림 정보를 찾을 수 없습니다", Toast.LENGTH_SHORT).show();
                    finish();
                }
            });
        });
    }

    private void displayReminderData() {
        textTitle.setText(reminder.title);
        textDateTime.setText(reminder.appointmentTime);
        textRoute.setText(reminder.departure + " → " + reminder.destination);
        textDuration.setText("예상 " + reminder.durationMinutes + "분 소요");
        textDepartureTime.setText("추천 출발시간: " + reminder.recommendedDepartureTime);
        textTransport.setText("교통수단: " + reminder.getTransportDisplayName());
        textDistance.setText("거리: " + reminder.distance);
        
        // 자동차 경로인 경우만 통행료, 연료비 표시
        if ("driving".equals(reminder.optimalTransport)) {
            textTollFare.setText("통행료: " + reminder.tollFare);
            textFuelPrice.setText("연료비: " + reminder.fuelPrice);
            textTollFare.setVisibility(View.VISIBLE);
            textFuelPrice.setVisibility(View.VISIBLE);
        } else {
            textTollFare.setVisibility(View.GONE);
            textFuelPrice.setVisibility(View.GONE);
        }
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());
        
        btnStartNavigation.setOnClickListener(v -> startNavigation());
        
        btnSnooze.setOnClickListener(v -> {
            // 10분 후 다시 알림
            Toast.makeText(this, "10분 후 다시 알려드리겠습니다", Toast.LENGTH_SHORT).show();
            // TODO: 10분 후 알림 스케줄링
            finish();
        });
        
        btnDismiss.setOnClickListener(v -> {
            // 알림 해제
            markReminderAsRead();
            finish();
        });
    }

    private void startNavigation() {
        if (reminder == null) return;
        
        try {
            // 네이버 지도 앱으로 길찾기
            String departure = reminder.departure;
            String destination = reminder.destination;
            
            // 교통수단에 따른 네이버 지도 URL
            String transportMode = "car"; // 기본값
            switch (reminder.optimalTransport) {
                case "transit":
                    transportMode = "transit";
                    break;
                case "walking":
                    transportMode = "walk";
                    break;
                default:
                    transportMode = "car";
                    break;
            }
            
            String naverMapUrl = "nmap://route/" + transportMode + "?slat=&slng=&sname=" + 
                Uri.encode(departure) + "&dlat=&dlng=&dname=" + Uri.encode(destination);
            
            Intent naverIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(naverMapUrl));
            naverIntent.setPackage("com.nhn.android.nmap");
            
            // 네이버 지도 앱이 설치되어 있는지 확인
            if (naverIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(naverIntent);
            } else {
                // 네이버 지도 앱이 없으면 웹 버전으로
                String webUrl = "https://map.naver.com/v5/directions/" +
                    Uri.encode(departure) + "/" + Uri.encode(destination) + "/-/-/" + transportMode;
                Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(webUrl));
                startActivity(webIntent);
            }
            
            // 알림 읽음 처리
            markReminderAsRead();
            
        } catch (Exception e) {
            // 구글 지도로 대체
            try {
                String googleMapUrl = "https://www.google.com/maps/dir/" + 
                    Uri.encode(reminder.departure) + "/" + Uri.encode(reminder.destination);
                Intent googleIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(googleMapUrl));
                startActivity(googleIntent);
                
                markReminderAsRead();
            } catch (Exception ex) {
                Toast.makeText(this, "지도 앱을 열 수 없습니다", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void markReminderAsRead() {
        if (reminder == null) return;
        
        Executors.newSingleThreadExecutor().execute(() -> {
            db.scheduleReminderDao().markNotificationSent(reminder.id);
        });
    }
}
