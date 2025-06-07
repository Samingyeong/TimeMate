// HomeActivity.java – 홈화면 전체 코드 (바텀 네비 + 일정 리스트 + DB 연결)
package com.example.timemate;

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
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public class HomeActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 1002;

    private TextView textWelcome;
    private TextView textCurrentDate;
    private TextView textTemperature;
    private TextView textCityName;
    private TextView textWeatherDescription;
    private TextView textFeelsLike;
    private TextView textHumidity;
    private LinearLayout layoutWeather;
    private RecyclerView recyclerView;
    private TextView textEmptySchedule;
    private LinearLayout cardAddSchedule;
    private LinearLayout cardViewSchedules;
    private ImageButton btnNotifications;

    // 내일 일정 알림 카드
    private com.google.android.material.card.MaterialCardView cardTomorrowReminder;
    private TextView textTomorrowTitle, textTomorrowRoute, textTomorrowDuration, textTomorrowDeparture;

    // 일정 기반 추천 시스템 (향후 구현)
    private Spinner spinnerSchedules;
    private RecyclerView recyclerRecommendations;
    private TextView textRecommendationTitle;

    private AppDatabase db;
    private ImprovedScheduleAdapter adapter;
    private WeatherService weatherService;
    private UserSession userSession;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // 1) 뷰 바인딩
        textWelcome = findViewById(R.id.textWelcome);
        textCurrentDate = findViewById(R.id.textCurrentDate);
        textTemperature = findViewById(R.id.textTemperature);
        textCityName = findViewById(R.id.textCityName);
        textWeatherDescription = findViewById(R.id.textWeatherDescription);
        textFeelsLike = findViewById(R.id.textFeelsLike);
        textHumidity = findViewById(R.id.textHumidity);
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

        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigationView);

        // 현재 메뉴 선택 표시
        bottomNav.setSelectedItemId(R.id.menu_home);

        // 사용자 세션 초기화
        userSession = new UserSession(this);

        // 로그인 상태 확인
        if (!userSession.isLoggedIn()) {
            // 로그인되지 않은 경우 로그인 화면으로 이동
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        // 2) 사용자 정보 및 날짜 설정
        setupWelcomeMessage();
        setupCurrentDate();

        // 3) 날씨 서비스 초기화 및 날씨 정보 로드
        weatherService = new WeatherService();
        loadWeatherData();

        // 4) 추천 시스템 초기화 (임시 비활성화)
        // geocodingService = new NaverGeocodingService();
        // placeSearchService = new NaverPlaceSearchService();
        setupRecommendationSystem();

        // 5) RecyclerView 설정
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerRecommendations.setLayoutManager(new LinearLayoutManager(this));

        // 5) Room DB 인스턴스 준비
        db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "timeMate-db")
                .fallbackToDestructiveMigration()
                .build();

        // 6) 일정 데이터 비동기 로드 (최근 5개만)
        loadRecentSchedules();

        // 6) 빠른 액션 버튼 클릭 처리
        cardAddSchedule.setOnClickListener(v -> {
            startActivity(new Intent(HomeActivity.this, com.example.timemate.ui.schedule.ScheduleAddActivity.class));
        });

        cardViewSchedules.setOnClickListener(v -> {
            startActivity(new Intent(HomeActivity.this, com.example.timemate.ui.schedule.ScheduleListActivity.class));
        });

        // 알림 버튼 클릭 처리
        btnNotifications.setOnClickListener(v -> {
            startActivity(new Intent(HomeActivity.this, NotificationActivity.class));
        });

        // 테스트용: 알림 버튼 길게 누르면 즉시 알림 작업 실행
        btnNotifications.setOnLongClickListener(v -> {
            Toast.makeText(this, "일정 알림 작업을 즉시 실행합니다...", Toast.LENGTH_SHORT).show();
            ScheduleReminderManager.runReminderWorkNow(this);
            return true;
        });

        // 알림 권한 요청
        requestNotificationPermission();

        // 테스트용 알림 생성 (앱 시작 시 한 번만)
        createTestNotifications();

        // 내일 일정 알림 로드
        loadTomorrowReminders();

        // 7) 바텀 네비게이션 클릭 처리
        bottomNav.setOnItemSelectedListener(new BottomNavigationView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();
                if (id == R.id.menu_recommendation) {
                    startActivity(new Intent(HomeActivity.this, com.example.timemate.ui.recommendation.RecommendationActivity.class));
                    finish();
                    return true;
                } else if (id == R.id.menu_home) {
                    return true; // 현재 화면이므로 아무것도 하지 않음
                } else if (id == R.id.menu_friends) {
                    startActivity(new Intent(HomeActivity.this, com.example.timemate.ui.friend.FriendListActivity.class));
                    finish();
                    return true;
                } else if (id == R.id.menu_profile) {
                    startActivity(new Intent(HomeActivity.this, com.example.timemate.ui.profile.ProfileActivity.class));
                    finish();
                    return true;
                }
                return false;
            }
        });
    }

    private void setupWelcomeMessage() {
        String userName = userSession.getCurrentUserNickname();
        String userId = userSession.getCurrentUserId();
        textWelcome.setText(userName + "님, 환영합니다!\n(" + userId + ")");
    }

    private void setupCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy년 MM월 dd일 EEEE", Locale.KOREAN);
        String currentDate = sdf.format(new Date());
        textCurrentDate.setText(currentDate + " - 오늘의 일정을 확인해보세요");
    }

    private void loadWeatherData() {
        // 기본적으로 서울 날씨를 로드
        loadWeatherByCity("Seoul");

        // 날씨 카드 클릭 시 도시 변경 기능 추가
        layoutWeather.setOnClickListener(v -> {
            // 현재 도시가 서울이면 대전으로, 대전이면 서울로 변경
            String currentCity = textCityName.getText().toString();
            if (currentCity.contains("서울") || currentCity.contains("Seoul")) {
                loadWeatherByCity("Daejeon");
                Toast.makeText(this, "대전 날씨로 변경", Toast.LENGTH_SHORT).show();
            } else {
                loadWeatherByCity("Seoul");
                Toast.makeText(this, "서울 날씨로 변경", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadWeatherByCity(String cityName) {
        weatherService.getWeatherByCity(cityName, new WeatherService.WeatherCallback() {
            @Override
            public void onSuccess(WeatherData weatherData) {
                runOnUiThread(() -> updateWeatherUI(weatherData));
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    textTemperature.setText("--°");
                    textCityName.setText("오류");
                    textWeatherDescription.setText("날씨 정보를 불러올 수 없습니다");
                    textFeelsLike.setText("");
                    textHumidity.setText("");
                    Toast.makeText(HomeActivity.this, "날씨 정보 로드 실패: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void updateWeatherUI(WeatherData weatherData) {
        textTemperature.setText(weatherData.getTemperatureCelsius() + "°");
        textCityName.setText(weatherData.getCityName());
        textWeatherDescription.setText(weatherData.getDescriptionKorean());
        textFeelsLike.setText("체감 " + weatherData.getFeelsLikeCelsius() + "°");
        textHumidity.setText("습도 " + weatherData.getHumidity() + "%");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            // 권한 결과와 관계없이 서울 날씨를 표시
            loadWeatherByCity("Seoul");
        } else if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "알림 권한이 허용되었습니다", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "알림 권한이 거부되었습니다. 설정에서 수동으로 허용해주세요", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    NOTIFICATION_PERMISSION_REQUEST_CODE);
            }
        }
    }

    private void loadRecentSchedules() {
        String currentUserId = userSession.getCurrentUserId();
        if (currentUserId == null) {
            // 사용자 ID가 없으면 빈 어댑터 설정
            setupEmptyAdapter();
            return;
        }

        // 먼저 빈 어댑터 설정하여 레이아웃 오류 방지
        setupEmptyAdapter();

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                List<Schedule> allSchedules = db.scheduleDao().getSchedulesByUserId(currentUserId);
                // 최근 5개만 표시
                List<Schedule> recentSchedules;
                if (allSchedules.size() > 5) {
                    recentSchedules = allSchedules.subList(0, 5);
                } else {
                    recentSchedules = allSchedules;
                }

                runOnUiThread(() -> {
                    adapter = new ImprovedScheduleAdapter(recentSchedules, HomeActivity.this);
                    recyclerView.setAdapter(adapter);

                    // 빈 상태 처리
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

            // 초기에는 빈 상태 표시
            recyclerView.setVisibility(View.GONE);
            if (textEmptySchedule != null) {
                textEmptySchedule.setVisibility(View.VISIBLE);
            }
        }
    }

    private void createTestNotifications() {
        // 이미 테스트 알림이 있는지 확인
        Executors.newSingleThreadExecutor().execute(() -> {
            List<Notification> existingNotifications = db.notificationDao().getAllNotifications();

            if (existingNotifications.isEmpty()) {
                // 테스트 알림들 생성
                Notification friendInvite = Notification.createFriendInvite(
                    "친구123",
                    "카페에서 만나기",
                    "1"
                );

                Notification departureReminder = Notification.createDepartureReminder(
                    "도서관 스터디",
                    "2"
                );

                db.notificationDao().insert(friendInvite);
                db.notificationDao().insert(departureReminder);
            }
        });
    }

    private void loadTomorrowReminders() {
        String currentUserId = userSession.getCurrentUserId();
        if (currentUserId == null) return;

        Executors.newSingleThreadExecutor().execute(() -> {
            // 내일 날짜 계산
            Calendar tomorrow = Calendar.getInstance();
            tomorrow.add(Calendar.DAY_OF_MONTH, 1);
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREAN);
            String tomorrowDate = dateFormat.format(tomorrow.getTime());

            // 내일 일정 알림 조회
            List<ScheduleReminder> reminders = db.scheduleReminderDao().getRemindersByDate(tomorrowDate);

            runOnUiThread(() -> {
                if (!reminders.isEmpty()) {
                    // 첫 번째 알림만 표시 (여러 개인 경우 가장 빠른 시간)
                    ScheduleReminder firstReminder = reminders.get(0);
                    displayTomorrowReminder(firstReminder);
                } else {
                    cardTomorrowReminder.setVisibility(View.GONE);
                }
            });
        });
    }

    private void displayTomorrowReminder(ScheduleReminder reminder) {
        textTomorrowTitle.setText(reminder.title);
        textTomorrowRoute.setText(reminder.departure + " → " + reminder.destination);
        textTomorrowDuration.setText("예상 " + reminder.durationMinutes + "분");
        textTomorrowDeparture.setText("추천 출발 " + reminder.recommendedDepartureTime);

        cardTomorrowReminder.setVisibility(View.VISIBLE);

        // 카드 클릭 시 상세 화면으로 이동
        cardTomorrowReminder.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, ScheduleReminderDetailActivity.class);
            intent.putExtra("reminder_id", reminder.scheduleId);
            startActivity(intent);
        });
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

    // 추천 시스템 메서드들 (향후 구현 예정)

    @Override
    protected void onResume() {
        super.onResume();
        // 화면이 다시 보일 때마다 내일 일정 알림 새로고침
        loadTomorrowReminders();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 향후 API 서비스 정리 코드 추가 예정
    }
}
