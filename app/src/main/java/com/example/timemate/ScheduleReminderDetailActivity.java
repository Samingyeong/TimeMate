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

import com.example.timemate.data.database.AppDatabase;

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
            try {
                android.util.Log.d("ReminderDetail", "리마인더 데이터 로드 시작 - ID: " + reminderId);

                // ScheduleReminderDao를 사용하여 실제 데이터 로드
                ScheduleReminderDao reminderDao = db.scheduleReminderDao();
                if (reminderDao != null) {
                    reminder = reminderDao.getReminderByScheduleId(reminderId);
                    android.util.Log.d("ReminderDetail", "데이터베이스에서 리마인더 조회 완료: " + (reminder != null ? "성공" : "실패"));
                } else {
                    android.util.Log.e("ReminderDetail", "ScheduleReminderDao가 null입니다");
                }

                runOnUiThread(() -> {
                    try {
                        if (reminder != null) {
                            android.util.Log.d("ReminderDetail", "리마인더 데이터 표시: " + reminder.title);
                            displayReminderData();
                        } else {
                            android.util.Log.w("ReminderDetail", "리마인더 데이터가 null입니다");
                            // 데이터가 없어도 기본 정보로 화면 구성
                            createFallbackReminderData();
                        }
                    } catch (Exception e) {
                        android.util.Log.e("ReminderDetail", "UI 업데이트 중 오류", e);
                        e.printStackTrace();
                        Toast.makeText(this, "화면 표시 중 오류가 발생했습니다", Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (Exception e) {
                android.util.Log.e("ReminderDetail", "리마인더 데이터 로드 중 오류", e);
                e.printStackTrace();

                runOnUiThread(() -> {
                    // 오류 발생 시에도 기본 데이터로 화면 구성
                    createFallbackReminderData();
                });
            }
        });
    }

    /**
     * 데이터베이스에서 리마인더를 찾을 수 없을 때 기본 데이터 생성
     */
    private void createFallbackReminderData() {
        try {
            android.util.Log.d("ReminderDetail", "Fallback 리마인더 데이터 생성");

            // Intent에서 추가 정보 가져오기 (있다면)
            String title = getIntent().getStringExtra("title");
            String departure = getIntent().getStringExtra("departure");
            String destination = getIntent().getStringExtra("destination");
            String appointmentTime = getIntent().getStringExtra("appointment_time");

            // 기본 리마인더 객체 생성
            reminder = new ScheduleReminder();
            reminder.id = reminderId;
            reminder.title = title != null ? title : "일정 알림";
            reminder.departure = departure != null ? departure : "출발지";
            reminder.destination = destination != null ? destination : "도착지";
            reminder.appointmentTime = appointmentTime != null ? appointmentTime : "시간 미정";
            reminder.optimalTransport = "driving";
            reminder.durationMinutes = 30;
            reminder.recommendedDepartureTime = "지금";
            reminder.distance = "계산 중";
            reminder.tollFare = "0원";
            reminder.fuelPrice = "0원";
            reminder.routeSummary = reminder.departure + " → " + reminder.destination;

            android.util.Log.d("ReminderDetail", "Fallback 데이터 생성 완료: " + reminder.title);
            displayReminderData();

        } catch (Exception e) {
            android.util.Log.e("ReminderDetail", "Fallback 데이터 생성 중 오류", e);
            e.printStackTrace();
            Toast.makeText(this, "기본 정보로 표시합니다", Toast.LENGTH_SHORT).show();

            // 최소한의 데이터라도 설정
            if (reminder == null) {
                reminder = new ScheduleReminder();
                reminder.title = "일정 알림";
                reminder.departure = "출발지";
                reminder.destination = "도착지";
                reminder.optimalTransport = "driving";
            }
            displayReminderData();
        }
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
        try {
            android.util.Log.d("ReminderDetail", "길찾기 시작 - reminder: " + (reminder != null ? "존재" : "null"));

            if (reminder == null) {
                android.util.Log.w("ReminderDetail", "리마인더가 null입니다. 기본 데이터로 길찾기 시도");
                Toast.makeText(this, "일정 정보가 없어 기본 길찾기를 실행합니다", Toast.LENGTH_SHORT).show();

                // 기본 길찾기 실행
                startBasicNavigation();
                return;
            }

            android.util.Log.d("ReminderDetail", "길찾기 정보 - 출발: " + reminder.departure + ", 도착: " + reminder.destination);

            if (reminder.departure == null || reminder.destination == null ||
                reminder.departure.trim().isEmpty() || reminder.destination.trim().isEmpty()) {
                android.util.Log.w("ReminderDetail", "출발지 또는 도착지 정보가 없습니다");
                Toast.makeText(this, "출발지 또는 도착지 정보가 없습니다", Toast.LENGTH_SHORT).show();
                return;
            }
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
            android.util.Log.e("ReminderDetail", "네이버 지도 실행 중 오류", e);
            e.printStackTrace();

            // 구글 지도로 대체
            try {
                android.util.Log.d("ReminderDetail", "구글 지도로 대체 실행");
                String googleMapUrl = "https://www.google.com/maps/dir/" +
                    Uri.encode(reminder.departure) + "/" + Uri.encode(reminder.destination);
                Intent googleIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(googleMapUrl));
                startActivity(googleIntent);

                markReminderAsRead();
                android.util.Log.d("ReminderDetail", "구글 지도 실행 성공");
            } catch (Exception ex) {
                android.util.Log.e("ReminderDetail", "구글 지도 실행도 실패", ex);
                ex.printStackTrace();
                Toast.makeText(this, "지도 앱을 열 수 없습니다", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * 기본 길찾기 (리마인더 정보가 없을 때)
     */
    private void startBasicNavigation() {
        try {
            android.util.Log.d("ReminderDetail", "기본 길찾기 실행");

            // Intent에서 기본 정보 가져오기
            String departure = getIntent().getStringExtra("departure");
            String destination = getIntent().getStringExtra("destination");

            if (departure == null || destination == null) {
                departure = "현재 위치";
                destination = "목적지";
            }

            // 구글 지도로 기본 길찾기
            String googleMapUrl = "https://www.google.com/maps/dir/" +
                Uri.encode(departure) + "/" + Uri.encode(destination);
            Intent googleIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(googleMapUrl));
            startActivity(googleIntent);

            android.util.Log.d("ReminderDetail", "기본 길찾기 실행 완료");

        } catch (Exception e) {
            android.util.Log.e("ReminderDetail", "기본 길찾기 실행 중 오류", e);
            e.printStackTrace();
            Toast.makeText(this, "길찾기를 실행할 수 없습니다", Toast.LENGTH_SHORT).show();
        }
    }

    private void markReminderAsRead() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                android.util.Log.d("ReminderDetail", "리마인더 읽음 처리 시작");

                if (reminder != null) {
                    ScheduleReminderDao reminderDao = db.scheduleReminderDao();
                    if (reminderDao != null) {
                        reminderDao.markNotificationSent(reminder.id);
                        android.util.Log.d("ReminderDetail", "리마인더 읽음 처리 완료: " + reminder.id);
                    } else {
                        android.util.Log.w("ReminderDetail", "ScheduleReminderDao가 null입니다");
                    }
                } else {
                    android.util.Log.w("ReminderDetail", "리마인더가 null이므로 읽음 처리를 건너뜁니다");
                }

            } catch (Exception e) {
                android.util.Log.e("ReminderDetail", "리마인더 읽음 처리 중 오류", e);
                e.printStackTrace();
            }
        });
    }

    /**
     * 추가 디버깅을 위한 메서드
     */
    private void logReminderInfo() {
        android.util.Log.d("ReminderDetail", "=== 리마인더 정보 ===");
        android.util.Log.d("ReminderDetail", "reminderId: " + reminderId);
        android.util.Log.d("ReminderDetail", "reminder: " + (reminder != null ? "존재" : "null"));
        if (reminder != null) {
            android.util.Log.d("ReminderDetail", "title: " + reminder.title);
            android.util.Log.d("ReminderDetail", "departure: " + reminder.departure);
            android.util.Log.d("ReminderDetail", "destination: " + reminder.destination);
            android.util.Log.d("ReminderDetail", "transport: " + reminder.optimalTransport);
        }
        android.util.Log.d("ReminderDetail", "==================");
    }
}
