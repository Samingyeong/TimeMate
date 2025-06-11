package com.example.timemate.ui.home;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.timemate.NaverPlaceSearchService;
import com.example.timemate.R;
import com.example.timemate.data.database.AppDatabase;
import com.example.timemate.data.model.Schedule;
import com.example.timemate.ui.recommendation.RecommendationAdapter;
import com.example.timemate.features.schedule.ScheduleAddActivity;
import com.example.timemate.ui.schedule.ScheduleListActivity;
import com.example.timemate.ui.schedule.ImprovedScheduleAdapter;
import com.example.timemate.ui.friend.FriendListActivity;
import com.example.timemate.features.profile.ProfileActivity;
import com.example.timemate.ui.recommendation.RecommendationActivity;
import com.example.timemate.network.api.WeatherService;
import com.example.timemate.util.UserSession;
import com.example.timemate.utils.NavigationHelper;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.Executors;

/**
 * 홈 화면 - 메인 대시보드
 * - 날씨 정보 표시
 * - 빠른 액션 (일정 추가, 일정 보기)
 * - 최근 일정 목록
 * - 내일 일정 알림
 * - 일정 기반 맛집 추천 (준비 중)
 */
public class HomeActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private static final int REQUEST_ADD_SCHEDULE = 1002;

    // UI 컴포넌트
    private LinearLayout layoutWeather;
    private RecyclerView recyclerView;
    private TextView textEmptySchedule;
    private LinearLayout cardAddSchedule;
    private LinearLayout cardViewSchedules;
    private ImageButton btnNotifications;

    // 날씨 관련 UI
    private TextView textTemperature, textCityName, textWeatherDescription;
    private TextView textFeelsLike, textHumidity;

    // 환영 메시지 UI
    private TextView textWelcome, textCurrentDate;

    // 내일 일정 알림 카드
    private com.google.android.material.card.MaterialCardView cardTomorrowReminder;
    private TextView textTomorrowTitle, textTomorrowRoute, textTomorrowDuration, textTomorrowDeparture;
    
    // 캘린더 관련 UI
    private CalendarView calendarView;
    private Button btnPrevMonth, btnNextMonth;
    private TextView textCurrentMonth;

    // OOTD 추천 시스템
    private RecyclerView recyclerOotd;
    private TextView textOotdDescription;
    
    // 데이터 및 서비스
    private AppDatabase db;
    private ImprovedScheduleAdapter adapter;
    private WeatherService weatherService;
    private UserSession userSession;

    // 현재 날씨 정보 캐시
    private WeatherService.WeatherData currentWeatherData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            android.util.Log.d("HomeActivity", "HomeActivity 시작");

            setContentView(R.layout.activity_home);

            // 1) 사용자 세션 및 데이터베이스 초기화
            try {
                // 앱 데이터 디렉토리 안전성 확인
                java.io.File appDataDir = getFilesDir();
                if (appDataDir != null && appDataDir.exists()) {
                    android.util.Log.d("HomeActivity", "앱 데이터 디렉토리 확인 완료: " + appDataDir.getAbsolutePath());
                } else {
                    android.util.Log.w("HomeActivity", "앱 데이터 디렉토리 접근 불가");
                }

                userSession = UserSession.getInstance(this);
                db = AppDatabase.getDatabase(this);
                android.util.Log.d("HomeActivity", "데이터베이스 및 세션 초기화 완료");
            } catch (Exception e) {
                android.util.Log.e("HomeActivity", "데이터베이스 초기화 실패", e);

                // 초기화 실패해도 앱 계속 실행
                Toast.makeText(this, "일부 기능이 제한될 수 있습니다", Toast.LENGTH_SHORT).show();
            }

            // 2) UI 컴포넌트 초기화
            try {
                initViews();
                android.util.Log.d("HomeActivity", "UI 초기화 완료");
            } catch (Exception e) {
                android.util.Log.e("HomeActivity", "UI 초기화 실패", e);
            }

            // 3) 날씨 서비스 초기화 및 날씨 정보 로드
            try {
                weatherService = new WeatherService();
                loadWeatherData();
                android.util.Log.d("HomeActivity", "날씨 서비스 초기화 완료");
            } catch (Exception e) {
                android.util.Log.e("HomeActivity", "날씨 서비스 초기화 실패", e);
            }

            // 4) OOTD 추천 시스템 초기화
            try {
                setupOotdRecommendation();
            } catch (Exception e) {
                android.util.Log.e("HomeActivity", "OOTD 초기화 실패", e);
            }

            // 5) RecyclerView 설정
            try {
                if (recyclerOotd != null) {
                    recyclerOotd.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
                }
            } catch (Exception e) {
                android.util.Log.e("HomeActivity", "RecyclerView 설정 실패", e);
            }

            // 6) 클릭 이벤트 설정
            try {
                setupClickListeners();
            } catch (Exception e) {
                android.util.Log.e("HomeActivity", "클릭 리스너 설정 실패", e);
            }

            // 7) 바텀 네비게이션 설정
            try {
                setupBottomNavigation();
                android.util.Log.d("HomeActivity", "하단 네비게이션 설정 완료");
            } catch (Exception e) {
                android.util.Log.e("HomeActivity", "하단 네비게이션 설정 실패", e);
            }

            // 8) 데이터 로드
            try {
                loadTomorrowReminders();
            } catch (Exception e) {
                android.util.Log.e("HomeActivity", "내일 일정 로드 실패", e);
            }

            // 9) 위치 권한 확인
            try {
                checkLocationPermission();
            } catch (Exception e) {
                android.util.Log.e("HomeActivity", "위치 권한 확인 실패", e);
            }

            android.util.Log.d("HomeActivity", "HomeActivity 초기화 완료");

        } catch (Exception e) {
            android.util.Log.e("HomeActivity", "HomeActivity 초기화 중 치명적 오류", e);
            e.printStackTrace();

            Toast.makeText(this, "홈화면 초기화 중 오류가 발생했습니다", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void initViews() {
        layoutWeather = findViewById(R.id.layoutWeather);
        // 일정 추가/보기 버튼 (새로운 ID로 수정)
        cardAddSchedule = findViewById(R.id.btnQuickAddSchedule);
        cardViewSchedules = findViewById(R.id.btnViewAllSchedules);
        btnNotifications = findViewById(R.id.btnNotifications);

        // 날씨 관련 UI
        textTemperature = findViewById(R.id.textTemperature);
        textCityName = findViewById(R.id.textCityName);
        textWeatherDescription = findViewById(R.id.textWeatherDescription);
        textFeelsLike = findViewById(R.id.textFeelsLike);
        textHumidity = findViewById(R.id.textHumidity);

        // 환영 메시지 UI (현재 레이아웃에 없는 ID들은 주석 처리)
        // textWelcome = findViewById(R.id.textWelcome);
        // textCurrentDate = findViewById(R.id.textCurrentDate);

        // 내일 일정 알림 카드
        cardTomorrowReminder = findViewById(R.id.cardTomorrowReminder);
        textTomorrowTitle = findViewById(R.id.textTomorrowTitle);
        textTomorrowRoute = findViewById(R.id.textTomorrowRoute);
        textTomorrowDuration = findViewById(R.id.textTomorrowDuration);
        textTomorrowDeparture = findViewById(R.id.textTomorrowDeparture);

        // OOTD 추천 시스템
        recyclerOotd = findViewById(R.id.recyclerOotd);
        textOotdDescription = findViewById(R.id.textOotdDescription);

        // 오늘 일정 박스 UI 요소들
        TextView textTodayDate = findViewById(R.id.textTodayDate);
        RecyclerView recyclerTodayScheduleBox = findViewById(R.id.recyclerTodayScheduleBox);
        TextView textNoScheduleToday = findViewById(R.id.textNoScheduleToday);

        // 오늘 날짜 설정
        if (textTodayDate != null) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("M월 d일 (E)", Locale.KOREAN);
            textTodayDate.setText(dateFormat.format(new Date()));
        }

        // 오늘 일정 박스 RecyclerView 설정
        if (recyclerTodayScheduleBox != null) {
            recyclerTodayScheduleBox.setLayoutManager(new LinearLayoutManager(this));
            recyclerTodayScheduleBox.setNestedScrollingEnabled(false);
        }

        // 환영 메시지 설정
        setupWelcomeMessage();
    }

    private void setupClickListeners() {
        // 일정 추가 카드 클릭 (null 체크 추가)
        if (cardAddSchedule != null) {
            cardAddSchedule.setOnClickListener(v -> {
                try {
                    android.util.Log.d("HomeActivity", "일정 추가 버튼 클릭");
                    Intent intent = new Intent(this, com.example.timemate.features.schedule.ScheduleAddActivity.class);
                    startActivityForResult(intent, REQUEST_ADD_SCHEDULE);
                } catch (Exception e) {
                    android.util.Log.e("HomeActivity", "일정 추가 화면 이동 오류", e);
                    Toast.makeText(this, "일정 추가 화면을 열 수 없습니다", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // 일정 보기 카드 클릭 (null 체크 추가)
        if (cardViewSchedules != null) {
            cardViewSchedules.setOnClickListener(v -> {
                try {
                    android.util.Log.d("HomeActivity", "일정 보기 버튼 클릭");
                    Intent intent = new Intent(this, com.example.timemate.features.schedule.ScheduleListActivity.class);
                    startActivity(intent);
                } catch (Exception e) {
                    android.util.Log.e("HomeActivity", "일정 보기 화면 이동 오류", e);
                    Toast.makeText(this, "일정 보기 화면을 열 수 없습니다", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // 알림 버튼 클릭 (null 체크 추가)
        if (btnNotifications != null) {
            btnNotifications.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(this, com.example.timemate.features.notification.NotificationActivity.class);
                    startActivity(intent);
                } catch (Exception e) {
                    android.util.Log.e("HomeActivity", "알림 화면 열기 오류", e);
                    Toast.makeText(this, "알림 화면을 열 수 없습니다", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void setupBottomNavigation() {
        try {
            android.util.Log.d("HomeActivity", "공통 네비게이션 헬퍼 사용");
            NavigationHelper.setupBottomNavigation(this, R.id.nav_home);
        } catch (Exception e) {
            android.util.Log.e("HomeActivity", "바텀 네비게이션 설정 오류", e);
        }
    }



    private void loadWeatherData() {
        // 서울 기본 좌표 (추후 사용자 위치로 변경 가능)
        double latitude = 37.5665;
        double longitude = 126.9780;
        
        weatherService.getCurrentWeather(latitude, longitude, new WeatherService.WeatherCallback() {
            @Override
            public void onSuccess(WeatherService.WeatherData weather) {
                runOnUiThread(() -> updateWeatherUI(weather));
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(HomeActivity.this, "날씨 정보를 가져올 수 없습니다", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void updateWeatherUI(WeatherService.WeatherData weather) {
        // 날씨 데이터 캐시
        this.currentWeatherData = weather;

        // 온도 표시
        if (textTemperature != null) {
            textTemperature.setText(String.format("%.0f°", weather.temperature));
        }

        // 도시명 표시
        if (textCityName != null) {
            textCityName.setText(weather.cityName != null ? weather.cityName : "서울");
        }

        // 날씨 설명 표시
        if (textWeatherDescription != null) {
            textWeatherDescription.setText(weather.description);
        }

        // 체감온도 표시
        if (textFeelsLike != null) {
            textFeelsLike.setText(String.format("체감 %.0f°", weather.feelsLike));
        }

        // 습도 표시
        if (textHumidity != null) {
            textHumidity.setText(String.format("습도 %d%%", weather.humidity));
        }

        // 날씨 정보가 업데이트되면 OOTD 추천도 새로고침
        try {
            loadOOTDRecommendations();
        } catch (Exception e) {
            android.util.Log.e("HomeActivity", "OOTD 새로고침 오류", e);
        }
    }

    private void setupWelcomeMessage() {
        String currentUserId = userSession.getCurrentUserId();
        String nickname = userSession.getCurrentNickname();

        // 환영 메시지 설정
        if (textWelcome != null) {
            if (nickname != null && !nickname.isEmpty()) {
                textWelcome.setText(nickname + "님, 안녕하세요!");
            } else {
                textWelcome.setText("안녕하세요!");
            }
        }

        // 현재 날짜 설정
        if (textCurrentDate != null) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy년 MM월 dd일 EEEE", Locale.KOREAN);
            String currentDate = dateFormat.format(new Date());
            textCurrentDate.setText(currentDate + "의 일정을 확인해보세요");
        }
    }



    private void loadTomorrowReminders() {
        // 오늘과 내일 일정 로드
        String currentUserId = userSession.getCurrentUserId();
        if (currentUserId == null) return;

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

                // 오늘 날짜
                Calendar today = Calendar.getInstance();
                String todayDate = dateFormat.format(today.getTime());

                // 내일 날짜
                Calendar tomorrow = Calendar.getInstance();
                tomorrow.add(Calendar.DAY_OF_MONTH, 1);
                String tomorrowDate = dateFormat.format(tomorrow.getTime());

                // 오늘 일정 로드
                List<Schedule> todaySchedules = db.scheduleDao().getSchedulesByUserAndDateRange(currentUserId, todayDate, todayDate);

                // 내일 일정 로드
                List<Schedule> tomorrowSchedules = db.scheduleDao().getSchedulesByUserAndDateRange(currentUserId, tomorrowDate, tomorrowDate);

                runOnUiThread(() -> {
                    // 홈화면에 일정 표시
                    displayTodayTomorrowSchedules(todaySchedules, tomorrowSchedules);

                    // 내일 일정 알림 (기존 로직 유지)
                    if (!tomorrowSchedules.isEmpty()) {
                        Schedule firstSchedule = tomorrowSchedules.get(0);
                        showTomorrowReminder(firstSchedule);
                    } else {
                        hideTomorrowReminder();
                    }
                });

            } catch (Exception e) {
                android.util.Log.e("HomeActivity", "일정 로드 오류", e);
            }
        });
    }

    /**
     * 홈화면에 오늘/내일 일정 표시
     */
    private void displayTodayTomorrowSchedules(List<Schedule> todaySchedules, List<Schedule> tomorrowSchedules) {
        try {
            android.util.Log.d("HomeActivity", "📅 일정 표시 시작");
            android.util.Log.d("HomeActivity", "📋 오늘 일정: " + (todaySchedules != null ? todaySchedules.size() : 0) + "개");
            android.util.Log.d("HomeActivity", "📋 내일 일정: " + (tomorrowSchedules != null ? tomorrowSchedules.size() : 0) + "개");

            // 오늘 일정 표시 (기존 섹션)
            displayScheduleSection(todaySchedules, "📅 오늘의 일정", findViewById(R.id.recyclerTodaySchedule));

            // 오늘 일정 박스 업데이트
            updateTodayScheduleBox(todaySchedules);

            // 내일 일정 표시
            displayScheduleSection(tomorrowSchedules, "📅 내일의 일정", findViewById(R.id.recyclerTomorrowSchedule));

            // 일정이 없을 때 안내 메시지 (선택적)
            if ((todaySchedules == null || todaySchedules.isEmpty()) &&
                (tomorrowSchedules == null || tomorrowSchedules.isEmpty())) {
                android.util.Log.d("HomeActivity", "💡 오늘과 내일 모두 일정이 없습니다");
            }

        } catch (Exception e) {
            android.util.Log.e("HomeActivity", "❌ 일정 표시 오류", e);
        }
    }

    /**
     * 오늘 일정 박스 업데이트
     */
    private void updateTodayScheduleBox(List<Schedule> todaySchedules) {
        try {
            RecyclerView recyclerTodayScheduleBox = findViewById(R.id.recyclerTodayScheduleBox);
            TextView textNoScheduleToday = findViewById(R.id.textNoScheduleToday);

            if (recyclerTodayScheduleBox == null || textNoScheduleToday == null) {
                android.util.Log.w("HomeActivity", "오늘 일정 박스 UI 요소를 찾을 수 없습니다");
                return;
            }

            if (todaySchedules == null || todaySchedules.isEmpty()) {
                // 일정이 없을 때
                recyclerTodayScheduleBox.setVisibility(View.GONE);
                textNoScheduleToday.setVisibility(View.VISIBLE);
                android.util.Log.d("HomeActivity", "📅 오늘 일정 없음 - 안내 메시지 표시");
            } else {
                // 일정이 있을 때
                recyclerTodayScheduleBox.setVisibility(View.VISIBLE);
                textNoScheduleToday.setVisibility(View.GONE);

                // 어댑터 설정
                ImprovedScheduleAdapter adapter = new ImprovedScheduleAdapter(todaySchedules, this);

                recyclerTodayScheduleBox.setAdapter(adapter);
                android.util.Log.d("HomeActivity", "📅 오늘 일정 박스 업데이트 완료: " + todaySchedules.size() + "개");
            }

        } catch (Exception e) {
            android.util.Log.e("HomeActivity", "❌ 오늘 일정 박스 업데이트 오류", e);
        }
    }

    /**
     * 일정 섹션 표시
     */
    private void displayScheduleSection(List<Schedule> schedules, String sectionTitle, androidx.recyclerview.widget.RecyclerView recyclerView) {
        try {
            if (recyclerView == null) {
                android.util.Log.w("HomeActivity", "⚠️ RecyclerView가 null입니다: " + sectionTitle);
                return;
            }

            if (schedules == null || schedules.isEmpty()) {
                android.util.Log.d("HomeActivity", "📭 " + sectionTitle + " 일정이 없어서 숨김 처리");
                recyclerView.setVisibility(android.view.View.GONE);
                return;
            }

            // RecyclerView 표시
            recyclerView.setVisibility(android.view.View.VISIBLE);

            // 어댑터 설정
            HomeScheduleAdapter adapter = new HomeScheduleAdapter(this, schedules, sectionTitle);

            // 일정 클릭 리스너 설정
            adapter.setOnScheduleClickListener(schedule -> {
                try {
                    showScheduleDetailDialog(schedule);
                } catch (Exception e) {
                    android.util.Log.e("HomeActivity", "일정 클릭 처리 오류", e);
                }
            });

            recyclerView.setAdapter(adapter);

            // 레이아웃 매니저 설정 (수직 스크롤, 중첩 스크롤 비활성화)
            androidx.recyclerview.widget.LinearLayoutManager layoutManager =
                new androidx.recyclerview.widget.LinearLayoutManager(this);
            layoutManager.setOrientation(androidx.recyclerview.widget.LinearLayoutManager.VERTICAL);
            recyclerView.setLayoutManager(layoutManager);

            // 중첩 스크롤 비활성화 (홈화면 스크롤과 충돌 방지)
            recyclerView.setNestedScrollingEnabled(false);

            android.util.Log.d("HomeActivity", "✅ " + sectionTitle + " 표시 완료: " + schedules.size() + "개");

            // 일정 상세 정보 로깅
            for (int i = 0; i < Math.min(schedules.size(), 3); i++) {
                Schedule schedule = schedules.get(i);
                android.util.Log.d("HomeActivity", "  📌 " + (i+1) + ". " +
                    (schedule.title != null ? schedule.title : "제목없음") +
                    " (" + (schedule.time != null ? schedule.time : "시간미정") + ")");
            }

        } catch (Exception e) {
            android.util.Log.e("HomeActivity", "❌ 일정 섹션 표시 오류: " + sectionTitle, e);
        }
    }

    /**
     * 일정 상세보기 다이얼로그
     */
    private void showScheduleDetailDialog(Schedule schedule) {
        try {
            if (schedule == null) {
                Toast.makeText(this, "일정 정보를 불러올 수 없습니다", Toast.LENGTH_SHORT).show();
                return;
            }

            // 안전한 필드 접근
            String title = schedule.title != null ? schedule.title : "제목 없음";
            String date = schedule.date != null ? schedule.date : "날짜 미정";
            String time = schedule.time != null ? schedule.time : "시간 미정";
            String departure = schedule.departure != null && !schedule.departure.trim().isEmpty() ?
                              schedule.departure : "없음";
            String destination = schedule.destination != null && !schedule.destination.trim().isEmpty() ?
                                schedule.destination : "없음";
            String memo = schedule.memo != null && !schedule.memo.trim().isEmpty() ?
                         schedule.memo : "없음";
            String status = schedule.isCompleted ? "✅ 완료" : "⏳ 진행중";

            // 상세 정보 구성
            StringBuilder details = new StringBuilder();
            details.append("📅 날짜: ").append(date).append("\n");
            details.append("⏰ 시간: ").append(time).append("\n");
            details.append("🚀 출발: ").append(departure).append("\n");
            details.append("🎯 도착: ").append(destination).append("\n");
            details.append("📝 메모: ").append(memo).append("\n");
            details.append("📊 상태: ").append(status);

            // 경로 정보 추가
            if (schedule.routeInfo != null && !schedule.routeInfo.isEmpty()) {
                details.append("\n\n🗺️ 선택된 경로:\n").append(parseRouteInfo(schedule.routeInfo));
            }

            if (schedule.selectedTransportModes != null && !schedule.selectedTransportModes.isEmpty()) {
                details.append("\n🚌 교통수단: ").append(schedule.selectedTransportModes);
            }

            // 함께하는 친구 정보 추가
            try {
                List<String> friendNames = getSharedFriends(schedule.id);
                if (!friendNames.isEmpty()) {
                    details.append("\n\n👥 함께하는 친구:\n");
                    for (String friendName : friendNames) {
                        details.append("• ").append(friendName).append("\n");
                    }
                }
            } catch (Exception e) {
                android.util.Log.e("HomeActivity", "친구 정보 로드 오류", e);
            }

            // 다이얼로그 표시
            new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("📋 " + title)
                .setMessage(details.toString())
                .setPositiveButton("일정 보기", (dialog, which) -> {
                    // 일정 목록 화면으로 이동
                    Intent intent = new Intent(this, com.example.timemate.features.schedule.ScheduleListActivity.class);
                    startActivity(intent);
                })
                .setNeutralButton("일정 추가", (dialog, which) -> {
                    // 일정 추가 화면으로 이동
                    Intent intent = new Intent(this, com.example.timemate.features.schedule.ScheduleAddActivity.class);
                    startActivity(intent);
                })
                .setNegativeButton("닫기", null)
                .show();

            android.util.Log.d("HomeActivity", "✅ 일정 상세보기 표시: " + title);

        } catch (Exception e) {
            android.util.Log.e("HomeActivity", "❌ 일정 상세보기 오류", e);
            Toast.makeText(this, "일정 상세보기 중 오류가 발생했습니다", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * JSON 형태의 경로 정보를 사용자가 읽기 쉬운 형태로 변환 (실제 저장된 데이터만)
     */
    private String parseRouteInfo(String routeInfoJson) {
        try {
            if (routeInfoJson == null || routeInfoJson.isEmpty()) {
                return "경로 정보 없음";
            }

            android.util.Log.d("HomeActivity", "파싱할 경로 정보: " + routeInfoJson);

            // 간단한 JSON 파싱 (org.json 사용)
            org.json.JSONObject json = new org.json.JSONObject(routeInfoJson);

            StringBuilder result = new StringBuilder();

            // 출발지 → 도착지 (실제 저장된 값만)
            String departure = json.optString("departure", "");
            String destination = json.optString("destination", "");
            if (!departure.isEmpty() && !destination.isEmpty()) {
                result.append("📍 ").append(departure).append(" → ").append(destination).append("\n");
            }

            // 선택된 교통수단들 (실제 저장된 값만)
            org.json.JSONArray selectedModes = json.optJSONArray("selectedModes");
            if (selectedModes != null && selectedModes.length() > 0) {
                result.append("🚌 선택된 교통수단: ");
                for (int i = 0; i < selectedModes.length(); i++) {
                    if (i > 0) result.append(", ");
                    String mode = selectedModes.optString(i);
                    if (!mode.isEmpty()) {
                        result.append(mode);
                    }
                }
                result.append("\n");
            }

            // 경로 상세 정보 (실제 저장된 값만)
            org.json.JSONArray routes = json.optJSONArray("routes");
            if (routes != null && routes.length() > 0) {
                result.append("\n📊 경로 상세:\n");
                for (int i = 0; i < routes.length(); i++) {
                    org.json.JSONObject route = routes.optJSONObject(i);
                    if (route != null) {
                        String mode = route.optString("mode", "");
                        String name = route.optString("name", "");
                        String duration = route.optString("duration", "");
                        String cost = route.optString("cost", "");
                        String distance = route.optString("distance", "");

                        // 실제 데이터가 있는 경우만 표시
                        if (!mode.isEmpty()) {
                            String icon = getTransportIcon(mode);

                            result.append(icon).append(" ");

                            // 교통수단명 표시
                            if (!name.isEmpty()) {
                                result.append(name);
                            } else {
                                result.append(mode);
                            }

                            // 시간과 비용 정보 표시
                            boolean hasTimeOrCost = !duration.isEmpty() || !cost.isEmpty();
                            if (hasTimeOrCost) {
                                result.append(": ");

                                // 시간 정보
                                if (!duration.isEmpty()) {
                                    result.append("⏱️ ").append(duration);
                                }

                                // 비용 정보
                                if (!cost.isEmpty()) {
                                    if (!duration.isEmpty()) result.append(" | ");
                                    result.append("💰 ").append(cost);
                                }

                                // 거리 정보 (있는 경우)
                                if (!distance.isEmpty()) {
                                    result.append(" | 📏 ").append(distance);
                                }
                            }

                            result.append("\n");

                            android.util.Log.d("HomeActivity", "경로 파싱: " + mode + " - 시간: " + duration + ", 비용: " + cost);
                        }
                    }
                }
            }

            String finalResult = result.toString().trim();
            android.util.Log.d("HomeActivity", "파싱된 경로 정보: " + finalResult);

            return finalResult.isEmpty() ? "저장된 경로 정보가 없습니다" : finalResult;

        } catch (Exception e) {
            android.util.Log.e("HomeActivity", "경로 정보 파싱 오류", e);
            return "경로 정보를 불러올 수 없습니다";
        }
    }

    /**
     * 교통수단 모드에 따른 아이콘 반환
     */
    private String getTransportIcon(String mode) {
        switch (mode.toLowerCase()) {
            case "transit": return "🚌";
            case "driving": return "🚗";
            case "walking": return "🚶";
            case "bicycle": return "🚴";
            case "taxi": return "🚕";
            default: return "🚶";
        }
    }

    /**
     * 일정에 참여하는 친구들 목록 가져오기
     */
    private List<String> getSharedFriends(int scheduleId) {
        List<String> friendNames = new ArrayList<>();
        try {
            com.example.timemate.data.database.AppDatabase database =
                com.example.timemate.data.database.AppDatabase.getInstance(this);

            // SharedSchedule에서 수락된 친구들 찾기
            List<com.example.timemate.data.model.SharedSchedule> allSharedSchedules =
                database.sharedScheduleDao().getSharedSchedulesByScheduleId(scheduleId);

            for (com.example.timemate.data.model.SharedSchedule shared : allSharedSchedules) {
                // 수락된 상태인 친구들만 포함
                if ("accepted".equals(shared.status)) {
                    if (shared.invitedNickname != null && !shared.invitedNickname.isEmpty()) {
                        friendNames.add(shared.invitedNickname);
                    } else {
                        // 닉네임이 없으면 사용자 ID 사용
                        friendNames.add(shared.invitedUserId);
                    }
                }
            }

            android.util.Log.d("HomeActivity", "일정 " + scheduleId + "의 참여 친구 수: " + friendNames.size());

        } catch (Exception e) {
            android.util.Log.e("HomeActivity", "공유 친구 정보 로드 오류", e);
        }
        return friendNames;
    }

    private void showTomorrowReminder(Schedule schedule) {
        if (cardTomorrowReminder != null) {
            cardTomorrowReminder.setVisibility(View.VISIBLE);
            textTomorrowTitle.setText(schedule.title);
            textTomorrowRoute.setText(schedule.departure + " → " + schedule.destination);
            textTomorrowDuration.setText("예상 소요시간: 계산 중...");
            textTomorrowDeparture.setText("출발 시간: " + schedule.time);
        }
    }

    private void hideTomorrowReminder() {
        if (cardTomorrowReminder != null) {
            cardTomorrowReminder.setVisibility(View.GONE);
        }
    }



    private void setupOotdRecommendation() {
        try {
            android.util.Log.d("HomeActivity", "🎨 OOTD 추천 시스템 초기화 시작");

            if (textOotdDescription != null) {
                textOotdDescription.setText("현재 날씨에 맞는 옷차림을 추천해드려요");
            }

            if (recyclerOotd != null) {
                // RecyclerView 설정
                androidx.recyclerview.widget.LinearLayoutManager layoutManager =
                    new androidx.recyclerview.widget.LinearLayoutManager(this, androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL, false);
                recyclerOotd.setLayoutManager(layoutManager);

                // 초기 OOTD 로드
                loadOOTDRecommendations();
            }

            android.util.Log.d("HomeActivity", "✅ OOTD 추천 시스템 초기화 완료");
        } catch (Exception e) {
            android.util.Log.e("HomeActivity", "❌ OOTD 추천 시스템 초기화 오류", e);
        }
    }

    /**
     * 현재 날씨에 맞는 OOTD 추천 로드
     */
    private void loadOOTDRecommendations() {
        try {
            android.util.Log.d("HomeActivity", "🎨 OOTD 추천 로드 시작");

            // OOTD 추천 서비스 초기화
            com.example.timemate.features.ootd.OOTDRecommendationService ootdService =
                new com.example.timemate.features.ootd.OOTDRecommendationService(this);

            // 현재 날씨 정보 가져오기
            String currentWeather = "맑음"; // 기본값
            double currentTemperature = 20.0; // 기본값

            if (weatherService != null) {
                try {
                    // 실제 날씨 정보 사용 (weatherService에서 가져오기)
                    // 현재 날씨 정보가 있다면 사용
                    currentWeather = getCurrentWeatherCondition();
                    currentTemperature = getCurrentTemperature();
                } catch (Exception e) {
                    android.util.Log.w("HomeActivity", "날씨 정보 가져오기 실패, 기본값 사용", e);
                }
            }

            android.util.Log.d("HomeActivity", "현재 날씨: " + currentWeather + ", 온도: " + currentTemperature);

            // OOTD 추천 가져오기
            List<com.example.timemate.data.model.OOTDRecommendation> recommendations =
                ootdService.getRecommendations(currentWeather, currentTemperature);

            // 어댑터 설정
            com.example.timemate.adapters.OOTDRecommendationAdapter ootdAdapter =
                new com.example.timemate.adapters.OOTDRecommendationAdapter(recommendations, this::onOOTDClick);

            if (recyclerOotd != null) {
                recyclerOotd.setAdapter(ootdAdapter);
            }

            // 설명 텍스트 업데이트
            if (textOotdDescription != null) {
                String description = String.format("현재 %s, %.0f°C에 맞는 스타일을 추천해드려요",
                    currentWeather, currentTemperature);
                textOotdDescription.setText(description);
            }

            android.util.Log.d("HomeActivity", "✅ OOTD 추천 로드 완료: " + recommendations.size() + "개");

        } catch (Exception e) {
            android.util.Log.e("HomeActivity", "❌ OOTD 추천 로드 오류", e);

            // 오류 시 기본 메시지 표시
            if (textOotdDescription != null) {
                textOotdDescription.setText("OOTD 추천을 불러올 수 없습니다");
            }
        }
    }

    /**
     * OOTD 카드 클릭 처리
     */
    private void onOOTDClick(com.example.timemate.data.model.OOTDRecommendation recommendation) {
        try {
            android.util.Log.d("HomeActivity", "OOTD 클릭: " + recommendation.title);

            // OOTD 상세 정보 다이얼로그 표시
            showOOTDDetailDialog(recommendation);

        } catch (Exception e) {
            android.util.Log.e("HomeActivity", "OOTD 클릭 처리 오류", e);
            android.widget.Toast.makeText(this, "스타일 정보를 표시할 수 없습니다", android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * OOTD 상세 정보 다이얼로그 표시
     */
    private void showOOTDDetailDialog(com.example.timemate.data.model.OOTDRecommendation recommendation) {
        try {
            String message = String.format("🎨 %s\n\n📝 %s\n\n🏷️ 카테고리: %s\n🌡️ 날씨: %s\n🗓️ 계절: %s",
                recommendation.title,
                recommendation.description,
                recommendation.category,
                recommendation.weather,
                recommendation.season);

            if (recommendation.tags != null && !recommendation.tags.isEmpty()) {
                message += "\n\n#️⃣ 태그: " + recommendation.tags;
            }

            new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("💫 스타일 상세정보")
                .setMessage(message)
                .setPositiveButton("확인", null)
                .show();

        } catch (Exception e) {
            android.util.Log.e("HomeActivity", "OOTD 상세 다이얼로그 표시 오류", e);
        }
    }

    /**
     * 현재 날씨 조건 반환
     */
    private String getCurrentWeatherCondition() {
        if (currentWeatherData != null && currentWeatherData.description != null) {
            return currentWeatherData.description;
        }
        return "맑음"; // 기본값
    }

    /**
     * 현재 온도 반환
     */
    private double getCurrentTemperature() {
        if (currentWeatherData != null) {
            return currentWeatherData.temperature;
        }
        return 20.0; // 기본값
    }

    private void checkLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, 
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 
                    LOCATION_PERMISSION_REQUEST_CODE);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        try {
            android.util.Log.d("HomeActivity", "📱 Activity 결과 수신 - requestCode: " + requestCode + ", resultCode: " + resultCode);

            if (requestCode == REQUEST_ADD_SCHEDULE && resultCode == RESULT_OK) {
                android.util.Log.d("HomeActivity", "✅ 일정 추가 완료 - 홈화면 업데이트");

                // 일정이 추가되었으므로 오늘의 일정 새로고침
                loadTodaySchedules();

                // 내일의 일정도 새로고침 (혹시 내일 일정이 추가되었을 수도 있음)
                loadTomorrowReminders();

                // 추가된 일정이 오늘 일정인지 확인
                if (data != null && data.getBooleanExtra("is_today_schedule", false)) {
                    android.util.Log.d("HomeActivity", "📅 오늘 일정이 추가됨 - 특별 알림 표시");
                    Toast.makeText(this, "🎉 오늘의 일정이 추가되었습니다!", Toast.LENGTH_LONG).show();
                }
            }

        } catch (Exception e) {
            android.util.Log.e("HomeActivity", "❌ Activity 결과 처리 오류", e);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            android.util.Log.d("HomeActivity", "🔄 홈화면 재개 - 모든 일정 새로고침");

            // 오늘의 일정 새로고침
            loadTodaySchedules();

            // 내일의 일정 새로고침
            loadTomorrowReminders();

            // 날씨 정보 새로고침
            loadWeatherData();

        } catch (Exception e) {
            android.util.Log.e("HomeActivity", "❌ 홈화면 재개 중 오류", e);
        }
    }

    /**
     * 오늘의 일정 로드
     */
    private void loadTodaySchedules() {
        try {
            android.util.Log.d("HomeActivity", "📅 오늘의 일정 로드 시작");

            if (userSession == null || userSession.getCurrentUserId() == null) {
                android.util.Log.w("HomeActivity", "사용자 세션이 없어서 오늘의 일정 로드 건너뜀");
                return;
            }

            String currentUserId = userSession.getCurrentUserId();

            // 오늘 날짜 계산
            java.util.Calendar today = java.util.Calendar.getInstance();
            String todayDateString = String.format("%04d-%02d-%02d",
                today.get(java.util.Calendar.YEAR),
                today.get(java.util.Calendar.MONTH) + 1,
                today.get(java.util.Calendar.DAY_OF_MONTH));

            android.util.Log.d("HomeActivity", "📅 오늘 날짜: " + todayDateString);

            // 백그라운드에서 오늘의 일정 조회
            java.util.concurrent.Executors.newSingleThreadExecutor().execute(() -> {
                try {
                    List<Schedule> todaySchedules =
                        db.scheduleDao().getSchedulesByUserAndDateRange(currentUserId, todayDateString, todayDateString);

                    android.util.Log.d("HomeActivity", "📅 오늘의 일정 조회 완료: " + todaySchedules.size() + "개");

                    // UI 스레드에서 업데이트
                    runOnUiThread(() -> {
                        updateTodaySchedulesUI(todaySchedules);
                    });

                } catch (Exception e) {
                    android.util.Log.e("HomeActivity", "❌ 오늘의 일정 조회 오류", e);
                    runOnUiThread(() -> {
                        // 오류 시 빈 상태 표시
                        updateTodaySchedulesUI(new ArrayList<>());
                    });
                }
            });

        } catch (Exception e) {
            android.util.Log.e("HomeActivity", "❌ 오늘의 일정 로드 오류", e);
        }
    }

    /**
     * 오늘의 일정 UI 업데이트
     */
    private void updateTodaySchedulesUI(List<Schedule> schedules) {
        try {
            android.util.Log.d("HomeActivity", "🎨 오늘의 일정 UI 업데이트: " + schedules.size() + "개");

            // 기존의 updateTodayScheduleBox 메서드를 사용
            updateTodayScheduleBox(schedules);

        } catch (Exception e) {
            android.util.Log.e("HomeActivity", "❌ 오늘의 일정 UI 업데이트 오류", e);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        try {
            // 메모리 누수 방지를 위한 리소스 정리
            if (weatherService != null) {
                weatherService = null;
            }

            if (adapter != null) {
                adapter = null;
            }

            // 데이터베이스 참조 해제
            if (db != null) {
                db = null;
            }

            // 사용자 세션 참조 해제
            if (userSession != null) {
                userSession = null;
            }

            android.util.Log.d("HomeActivity", "HomeActivity 리소스 정리 완료");

        } catch (Exception e) {
            android.util.Log.e("HomeActivity", "리소스 정리 중 오류", e);
        }
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);

        try {
            android.util.Log.d("HomeActivity", "메모리 정리 요청: " + level);

            // 메모리 부족 시 불필요한 리소스 해제
            if (level >= TRIM_MEMORY_MODERATE) {
                // 캐시된 데이터 정리
                if (adapter != null) {
                    // 어댑터 데이터 정리 (구현 시)
                }

                // 가비지 컬렉션 힌트
                System.gc();
            }

        } catch (Exception e) {
            android.util.Log.e("HomeActivity", "메모리 정리 중 오류", e);
        }
    }
}
