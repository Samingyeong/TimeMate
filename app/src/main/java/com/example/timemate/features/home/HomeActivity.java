package com.example.timemate.features.home;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.timemate.R;
import com.example.timemate.data.model.Schedule;
import com.example.timemate.data.database.AppDatabase;
import com.example.timemate.features.schedule.ScheduleAddActivity;
import com.example.timemate.features.schedule.ScheduleListActivity;
import com.example.timemate.features.friend.FriendListActivity;
import com.example.timemate.features.profile.ProfileActivity;
import com.example.timemate.features.home.service.WeatherService;
import com.example.timemate.ui.recommendation.RecommendationActivity;
import com.example.timemate.features.home.adapter.TodayScheduleAdapter;
import com.example.timemate.features.home.adapter.TomorrowReminderAdapter;
import com.example.timemate.ScheduleReminder;
import com.example.timemate.ScheduleReminderDao;
import com.example.timemate.core.util.UserSession;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 홈 화면 - 메인 대시보드
 * 기능: 날씨, 오늘/내일 일정, 빠른 액션
 */
public class HomeActivity extends AppCompatActivity {

    // UI 컴포넌트
    private TextView textWeatherInfo;
    private TextView textGreeting;
    private RecyclerView recyclerTodaySchedule;
    private RecyclerView recyclerTomorrowSchedule;

    // 날씨 정보 UI 컴포넌트들
    private TextView textTemperature;
    private TextView textCityName;
    private TextView textWeatherDescription;
    private TextView textFeelsLike;
    private TextView textHumidity;

    // 빠른 액션 버튼들
    private LinearLayout btnQuickAddSchedule;
    private LinearLayout btnViewAllSchedules;

    // RecyclerView
    private RecyclerView recyclerTomorrowReminders;

    // 어댑터
    private TodayScheduleAdapter todayAdapter;
    private TodayScheduleAdapter tomorrowAdapter;
    private TomorrowReminderAdapter reminderAdapter;

    // 서비스
    private WeatherService weatherService;
    private HomePresenter presenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        initViews();
        initServices();
        setupRecyclerViews();
        setupClickListeners();
        setupBottomNavigation();
        loadData();
    }

    private void initViews() {
        // textWeatherInfo = findViewById(R.id.textWeatherInfo);
        // textGreeting = findViewById(R.id.textGreeting);
        recyclerTodaySchedule = findViewById(R.id.recyclerTodaySchedule);
        recyclerTomorrowSchedule = findViewById(R.id.recyclerTomorrowSchedule);

        // 내일 출발 추천 카드 RecyclerView (현재 레이아웃에 없으므로 null로 설정)
        recyclerTomorrowReminders = null; // 향후 레이아웃에 추가될 예정

        // 날씨 정보 UI 컴포넌트들 초기화
        textTemperature = findViewById(R.id.textTemperature);
        textCityName = findViewById(R.id.textCityName);
        textWeatherDescription = findViewById(R.id.textWeatherDescription);
        textFeelsLike = findViewById(R.id.textFeelsLike);
        textHumidity = findViewById(R.id.textHumidity);

        // 빠른 액션 버튼들 초기화
        btnQuickAddSchedule = findViewById(R.id.btnQuickAddSchedule);
        btnViewAllSchedules = findViewById(R.id.btnViewAllSchedules);
    }

    private void initServices() {
        weatherService = new WeatherService();
        presenter = new HomePresenter(this);
    }

    private void setupRecyclerViews() {
        // 오늘 일정 RecyclerView
        todayAdapter = new TodayScheduleAdapter(schedule -> {
            // 일정 클릭 시 상세보기
            openScheduleDetail(schedule);
        });
        recyclerTodaySchedule.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        recyclerTodaySchedule.setAdapter(todayAdapter);

        // 내일 일정 RecyclerView
        tomorrowAdapter = new TodayScheduleAdapter(schedule -> {
            openScheduleDetail(schedule);
        });
        recyclerTomorrowSchedule.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        recyclerTomorrowSchedule.setAdapter(tomorrowAdapter);

        // 내일 출발 추천 카드 RecyclerView (있는 경우에만)
        if (recyclerTomorrowReminders != null) {
            reminderAdapter = new TomorrowReminderAdapter(this);
            recyclerTomorrowReminders.setLayoutManager(new LinearLayoutManager(this));
            recyclerTomorrowReminders.setAdapter(reminderAdapter);
        }
    }

    private void setupClickListeners() {
        // 빠른 일정 추가
        if (btnQuickAddSchedule != null) {
            btnQuickAddSchedule.setOnClickListener(v -> {
                Intent intent = new Intent(this, ScheduleAddActivity.class);
                startActivity(intent);
            });
        }

        // 전체 일정 보기
        if (btnViewAllSchedules != null) {
            btnViewAllSchedules.setOnClickListener(v -> {
                try {
                    Log.d("HomeActivity", "🔍 일정보기 버튼 클릭됨");

                    // Activity 상태 확인
                    if (isFinishing() || isDestroyed()) {
                        Log.w("HomeActivity", "❌ Activity가 종료 중이므로 화면 전환을 건너뜁니다");
                        return;
                    }

                    // UserSession 상태 확인
                    UserSession userSession = UserSession.getInstance(this);
                    if (userSession == null || !userSession.isLoggedIn()) {
                        Log.w("HomeActivity", "❌ 사용자가 로그인되지 않음");
                        Toast.makeText(this, "로그인이 필요합니다", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Log.d("HomeActivity", "✅ 사용자 로그인 상태 확인 완료");

                    // 안전한 Activity 전환
                    safeStartActivity(ScheduleListActivity.class);

                } catch (Exception e) {
                    Log.e("HomeActivity", "❌ 일정보기 버튼 클릭 오류", e);
                    e.printStackTrace();
                    Toast.makeText(this, "일정 화면을 열 수 없습니다: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Log.w("HomeActivity", "⚠️ btnViewAllSchedules가 null입니다");
        }
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigationView);
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_home);

            bottomNav.setOnItemSelectedListener(new BottomNavigationView.OnItemSelectedListener() {
                @Override
                public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                    try {
                        int id = item.getItemId();
                        Log.d("HomeActivity", "🧭 BottomNavigation 클릭: " + item.getTitle());

                        if (id == R.id.nav_home) {
                            return true; // 현재 화면
                        } else if (id == R.id.nav_schedule) {
                            Log.d("HomeActivity", "📅 일정 화면으로 이동");
                            safeStartActivity(com.example.timemate.features.schedule.ScheduleListActivity.class);
                            return true;
                        } else if (id == R.id.nav_friends) {
                            Log.d("HomeActivity", "👥 친구 화면으로 이동");
                            safeStartActivity(com.example.timemate.features.friend.FriendListActivity.class);
                            return true;
                        } else if (id == R.id.nav_recommendation) {
                            Log.d("HomeActivity", "🎯 추천 화면으로 이동");
                            safeStartActivity(RecommendationActivity.class);
                            return true;
                        } else if (id == R.id.nav_profile) {
                            Log.d("HomeActivity", "👤 프로필 화면으로 이동");
                            safeStartActivity(com.example.timemate.features.profile.ProfileActivity.class);
                            return true;
                        }
                        return false;

                    } catch (Exception e) {
                        Log.e("HomeActivity", "❌ BottomNavigation 클릭 오류", e);
                        e.printStackTrace();
                        Toast.makeText(HomeActivity.this, "화면 전환 중 오류가 발생했습니다", Toast.LENGTH_SHORT).show();
                        return false;
                    }
                }
            });
        }
    }

    private void loadData() {
        loadGreeting();
        loadWeatherInfo();
        loadTodaySchedules();
        loadTomorrowSchedules();
        loadTomorrowReminders(); // 내일 출발 추천 카드 로드
    }

    private void loadGreeting() {
        UserSession userSession = UserSession.getInstance(this);
        String userName = userSession.getCurrentUserName();
        if (userName != null && !userName.isEmpty()) {
            textGreeting.setText("안녕하세요, " + userName + "님!");
        } else {
            textGreeting.setText("안녕하세요!");
        }
    }

    private void loadWeatherInfo() {
        weatherService.getCurrentWeather("Seoul", new WeatherService.WeatherCallback() {
            @Override
            public void onSuccess(WeatherService.WeatherInfo weather) {
                runOnUiThread(() -> {
                    // 기본 텍스트 업데이트
                    textWeatherInfo.setText(weather.getDisplayText());

                    // 날씨 섹션 세부 정보 업데이트
                    updateWeatherSection(weather);
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    textWeatherInfo.setText("날씨 정보를 불러올 수 없습니다");
                    // 오류 시 기본값 표시
                    showWeatherError();
                });
            }
        });
    }

    private void updateWeatherSection(WeatherService.WeatherInfo weather) {
        if (textTemperature != null) {
            textTemperature.setText(String.format("%.0f°", weather.getTemperature()));
        }
        if (textCityName != null) {
            textCityName.setText(weather.getCityName());
        }
        if (textWeatherDescription != null) {
            textWeatherDescription.setText(weather.getDescription());
        }
        if (textFeelsLike != null) {
            textFeelsLike.setText(String.format("체감 %.0f°", weather.getFeelsLike()));
        }
        if (textHumidity != null) {
            textHumidity.setText(String.format("습도 %d%%", weather.getHumidity()));
        }
    }

    private void showWeatherError() {
        if (textTemperature != null) textTemperature.setText("--°");
        if (textCityName != null) textCityName.setText("위치 확인 중...");
        if (textWeatherDescription != null) textWeatherDescription.setText("날씨 정보 로딩 실패");
        if (textFeelsLike != null) textFeelsLike.setText("체감 --°");
        if (textHumidity != null) textHumidity.setText("습도 --%");
    }

    private void loadTodaySchedules() {
        presenter.loadTodaySchedules(schedules -> {
            runOnUiThread(() -> {
                todayAdapter.updateSchedules(schedules);
            });
        });
    }

    private void loadTomorrowSchedules() {
        presenter.loadTomorrowSchedules(schedules -> {
            runOnUiThread(() -> {
                tomorrowAdapter.updateSchedules(schedules);
            });
        });
    }

    /**
     * 내일 출발 추천 카드 로드
     */
    private void loadTomorrowReminders() {
        if (reminderAdapter == null) {
            return; // RecyclerView가 없으면 스킵
        }

        try {
            // 백그라운드에서 리마인더 조회
            new Thread(() -> {
                try {
                    AppDatabase db = AppDatabase.getInstance(this);
                    ScheduleReminderDao reminderDao = db.scheduleReminderDao();

                    // 내일 날짜의 활성 리마인더 조회
                    Calendar tomorrow = Calendar.getInstance();
                    tomorrow.add(Calendar.DAY_OF_MONTH, 1);
                    String tomorrowDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(tomorrow.getTime());

                    List<ScheduleReminder> reminders = reminderDao.getRemindersByDate(tomorrowDate);

                    // UI 스레드에서 어댑터 업데이트
                    runOnUiThread(() -> {
                        if (reminderAdapter != null) {
                            reminderAdapter.updateReminders(reminders);
                            Log.d("HomeActivity", "내일 출발 추천 카드 로드 완료: " + reminders.size() + "개");
                        }
                    });

                } catch (Exception e) {
                    Log.e("HomeActivity", "리마인더 로드 오류", e);
                    e.printStackTrace();
                }
            }).start();

        } catch (Exception e) {
            Log.e("HomeActivity", "loadTomorrowReminders 오류", e);
            e.printStackTrace();
        }
    }

    private void openScheduleDetail(Schedule schedule) {
        try {
            Log.d("HomeActivity", "일정 상세보기 클릭: " + schedule.title);

            // Activity 상태 확인
            if (isFinishing() || isDestroyed()) {
                Log.w("HomeActivity", "Activity가 종료 중이므로 화면 전환을 건너뜁니다");
                return;
            }

            // 일정 수정 화면으로 안전하게 이동
            Intent intent = new Intent(this, ScheduleAddActivity.class);
            intent.putExtra("schedule_id", schedule.id);
            intent.putExtra("edit_mode", true);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);

            Log.d("HomeActivity", "일정 상세보기 화면 전환 성공");

        } catch (Exception e) {
            Log.e("HomeActivity", "일정 상세보기 화면 전환 오류", e);
            e.printStackTrace();
            Toast.makeText(this, "일정 상세보기를 열 수 없습니다", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 안전한 Activity 전환 (실제 디바이스에서 크래시 방지)
     */
    private void safeStartActivity(Class<?> targetActivity) {
        try {
            Log.d("HomeActivity", "🚀 safeStartActivity 시작: " + targetActivity.getSimpleName());

            // Activity 상태 재확인
            if (isFinishing() || isDestroyed()) {
                Log.w("HomeActivity", "❌ Activity가 종료 중이므로 화면 전환을 건너뜁니다");
                return;
            }

            // 메모리 상태 확인
            Runtime runtime = Runtime.getRuntime();
            long freeMemory = runtime.freeMemory();
            long totalMemory = runtime.totalMemory();
            double memoryUsage = (double)(totalMemory - freeMemory) / totalMemory * 100;

            Log.d("HomeActivity", "📊 메모리 사용률: " + String.format("%.1f%%", memoryUsage));

            if (memoryUsage > 85) {
                Log.w("HomeActivity", "⚠️ 메모리 사용률 높음, 가비지 컬렉션 실행");
                System.gc();

                // 잠시 대기 후 재시도
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                    executeActivityTransition(targetActivity);
                }, 200);
            } else {
                executeActivityTransition(targetActivity);
            }

        } catch (Exception e) {
            Log.e("HomeActivity", "❌ safeStartActivity 오류: " + targetActivity.getSimpleName(), e);
            e.printStackTrace();
            Toast.makeText(this, "화면 전환 중 오류가 발생했습니다: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 실제 Activity 전환 실행
     */
    private void executeActivityTransition(Class<?> targetActivity) {
        try {
            Log.d("HomeActivity", "🔄 Activity 전환 실행: " + targetActivity.getSimpleName());

            // 최종 상태 확인
            if (isFinishing() || isDestroyed()) {
                Log.w("HomeActivity", "❌ 전환 직전 Activity 종료 감지");
                return;
            }

            Intent intent = new Intent(this, targetActivity);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

            // Intent 유효성 확인
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
                Log.d("HomeActivity", "✅ Activity 전환 성공: " + targetActivity.getSimpleName());
            } else {
                Log.e("HomeActivity", "❌ 대상 Activity를 찾을 수 없음: " + targetActivity.getSimpleName());
                Toast.makeText(this, "해당 화면을 찾을 수 없습니다", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            Log.e("HomeActivity", "❌ executeActivityTransition 오류: " + targetActivity.getSimpleName(), e);
            e.printStackTrace();
            Toast.makeText(this, "화면 전환 실행 중 오류: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 화면 복귀 시 데이터 새로고침
        loadTodaySchedules();
        loadTomorrowSchedules();
        loadTomorrowReminders(); // 내일 출발 추천 카드도 새로고침
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (weatherService != null) {
            weatherService.shutdown();
        }
        if (presenter != null) {
            presenter.destroy();
        }
    }
}
