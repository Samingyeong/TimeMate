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
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.timemate.NaverPlaceSearchService;
import com.example.timemate.R;
import com.example.timemate.data.database.AppDatabase;
import com.example.timemate.data.model.Schedule;
import com.example.timemate.ui.recommendation.RecommendationAdapter;
import com.example.timemate.ui.schedule.ScheduleAddActivity;
import com.example.timemate.ui.schedule.ScheduleListActivity;
import com.example.timemate.ui.schedule.ImprovedScheduleAdapter;
import com.example.timemate.ui.friend.FriendListActivity;
import com.example.timemate.ui.profile.ProfileActivity;
import com.example.timemate.ui.recommendation.RecommendationActivity;
import com.example.timemate.network.api.WeatherService;
import com.example.timemate.util.UserSession;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
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

    // UI 컴포넌트
    private LinearLayout layoutWeather;
    private RecyclerView recyclerView;
    private TextView textEmptySchedule;
    private LinearLayout cardAddSchedule;
    private LinearLayout cardViewSchedules;
    private ImageButton btnNotifications;

    // 내일 일정 알림 카드
    private com.google.android.material.card.MaterialCardView cardTomorrowReminder;
    private TextView textTomorrowTitle, textTomorrowRoute, textTomorrowDuration, textTomorrowDeparture;
    
    // 일정 기반 추천 시스템 (준비 중)
    private Spinner spinnerSchedules;
    private RecyclerView recyclerRecommendations;
    private TextView textRecommendationTitle;
    // 추천 기능은 향후 구현 예정
    
    // 데이터 및 서비스
    private AppDatabase db;
    private ImprovedScheduleAdapter adapter;
    private WeatherService weatherService;
    private UserSession userSession;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // 1) 사용자 세션 및 데이터베이스 초기화
        userSession = UserSession.getInstance(this);
        db = AppDatabase.getDatabase(this);

        // 2) UI 컴포넌트 초기화
        initViews();

        // 3) 날씨 서비스 초기화 및 날씨 정보 로드
        weatherService = new WeatherService();
        loadWeatherData();

        // 4) 추천 시스템 초기화 (임시 비활성화)
        setupRecommendationSystem();

        // 5) RecyclerView 설정
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerRecommendations.setLayoutManager(new LinearLayoutManager(this));

        // 6) 클릭 이벤트 설정
        setupClickListeners();

        // 7) 바텀 네비게이션 설정
        setupBottomNavigation();

        // 8) 데이터 로드
        loadRecentSchedules();
        loadTomorrowReminders();

        // 9) 위치 권한 확인
        checkLocationPermission();
    }

    private void initViews() {
        layoutWeather = findViewById(R.id.layoutWeather);
        recyclerView = findViewById(R.id.recyclerHomeSchedule);
        textEmptySchedule = findViewById(R.id.textEmptySchedule);
        cardAddSchedule = findViewById(R.id.cardAddSchedule);
        cardViewSchedules = findViewById(R.id.cardViewSchedules);
        btnNotifications = findViewById(R.id.btnNotifications);
        
        // 내일 일정 알림 카드
        cardTomorrowReminder = findViewById(R.id.cardTomorrowReminder);
        textTomorrowTitle = findViewById(R.id.textTomorrowTitle);
        textTomorrowRoute = findViewById(R.id.textTomorrowRoute);
        textTomorrowDuration = findViewById(R.id.textTomorrowDuration);
        textTomorrowDeparture = findViewById(R.id.textTomorrowDeparture);
        
        // 일정 기반 추천 시스템
        spinnerSchedules = findViewById(R.id.spinnerSchedules);
        recyclerRecommendations = findViewById(R.id.recyclerRecommendations);
        textRecommendationTitle = findViewById(R.id.textRecommendationTitle);
    }

    private void setupClickListeners() {
        // 일정 추가 카드 클릭
        cardAddSchedule.setOnClickListener(v -> {
            Intent intent = new Intent(this, ScheduleAddActivity.class);
            startActivity(intent);
        });

        // 일정 보기 카드 클릭
        cardViewSchedules.setOnClickListener(v -> {
            Intent intent = new Intent(this, ScheduleListActivity.class);
            startActivity(intent);
        });

        // 알림 버튼 클릭
        btnNotifications.setOnClickListener(v -> {
            Toast.makeText(this, "알림 기능 준비 중입니다", Toast.LENGTH_SHORT).show();
        });
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigationView);
        bottomNav.setSelectedItemId(R.id.menu_home);

        bottomNav.setOnItemSelectedListener(new BottomNavigationView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();
                if (id == R.id.menu_home) {
                    return true; // 현재 화면
                } else if (id == R.id.menu_friends) {
                    startActivity(new Intent(HomeActivity.this, FriendListActivity.class));
                    finish();
                    return true;
                } else if (id == R.id.menu_recommendation) {
                    startActivity(new Intent(HomeActivity.this, RecommendationActivity.class));
                    finish();
                    return true;
                } else if (id == R.id.menu_profile) {
                    startActivity(new Intent(HomeActivity.this, ProfileActivity.class));
                    finish();
                    return true;
                }
                return false;
            }
        });
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
        if (layoutWeather != null) {
            layoutWeather.removeAllViews();
            
            TextView weatherText = new TextView(this);
            weatherText.setText(String.format("서울 %s %.1f°C", 
                weather.description, weather.temperature));
            weatherText.setTextSize(16);
            weatherText.setTextColor(getColor(R.color.text_primary));
            
            layoutWeather.addView(weatherText);
        }
    }

    private void loadRecentSchedules() {
        String currentUserId = userSession.getCurrentUserId();
        if (currentUserId == null) {
            setupEmptyAdapter();
            return;
        }

        setupEmptyAdapter();

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                List<Schedule> allSchedules = db.scheduleDao().getSchedulesByUserId(currentUserId);
                List<Schedule> recentSchedules;
                if (allSchedules.size() > 5) {
                    recentSchedules = allSchedules.subList(0, 5);
                } else {
                    recentSchedules = allSchedules;
                }

                runOnUiThread(() -> {
                    adapter = new ImprovedScheduleAdapter(recentSchedules, HomeActivity.this);
                    recyclerView.setAdapter(adapter);
                    
                    if (recentSchedules.isEmpty()) {
                        recyclerView.setVisibility(View.GONE);
                        textEmptySchedule.setVisibility(View.VISIBLE);
                    } else {
                        recyclerView.setVisibility(View.VISIBLE);
                        textEmptySchedule.setVisibility(View.GONE);
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(HomeActivity.this, "일정 로드 중 오류 발생", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void setupEmptyAdapter() {
        if (recyclerView != null) {
            List<Schedule> emptyList = new ArrayList<>();
            adapter = new ImprovedScheduleAdapter(emptyList, HomeActivity.this);
            recyclerView.setAdapter(adapter);
            
            recyclerView.setVisibility(View.GONE);
            if (textEmptySchedule != null) {
                textEmptySchedule.setVisibility(View.VISIBLE);
            }
        }
    }

    private void loadTomorrowReminders() {
        // 내일 일정 알림 로직 (기존 코드 유지)
        String currentUserId = userSession.getCurrentUserId();
        if (currentUserId == null) return;

        Executors.newSingleThreadExecutor().execute(() -> {
            Calendar tomorrow = Calendar.getInstance();
            tomorrow.add(Calendar.DAY_OF_MONTH, 1);
            
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            String tomorrowDate = dateFormat.format(tomorrow.getTime());
            
            List<Schedule> tomorrowSchedules = db.scheduleDao().getSchedulesByDate(currentUserId, tomorrowDate);
            
            runOnUiThread(() -> {
                if (!tomorrowSchedules.isEmpty()) {
                    Schedule firstSchedule = tomorrowSchedules.get(0);
                    showTomorrowReminder(firstSchedule);
                } else {
                    hideTomorrowReminder();
                }
            });
        });
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

    private void setupRecommendationSystem() {
        // Spinner 초기화
        if (spinnerSchedules != null) {
            List<String> items = new ArrayList<>();
            items.add("추천 기능 준비 중입니다");
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, items);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerSchedules.setAdapter(adapter);
        }

        // 추천 RecyclerView는 향후 구현 예정
        if (recyclerRecommendations != null) {
            recyclerRecommendations.setVisibility(View.GONE);
        }
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
    protected void onResume() {
        super.onResume();
        loadTomorrowReminders();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 향후 API 서비스 정리 코드 추가 예정
    }
}
