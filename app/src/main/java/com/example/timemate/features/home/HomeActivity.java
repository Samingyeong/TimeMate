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
import com.example.timemate.utils.NavigationHelper;
import com.example.timemate.features.home.adapter.TomorrowReminderAdapter;
import com.example.timemate.ScheduleReminder;
import com.example.timemate.ScheduleReminderDao;
import com.example.timemate.util.UserSession;
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

        // UI 초기화 (빠른 작업들만)
        initViews();
        initServices();
        setupRecyclerViews();
        setupClickListeners();
        setupBottomNavigation();

        // 무거운 데이터 로딩은 UI 렌더링 후 비동기로 실행
        scheduleDataLoading();
    }

    /**
     * UI 렌더링 완료 후 데이터 로딩을 비동기로 실행
     */
    private void scheduleDataLoading() {
        // UI 렌더링이 완료된 후 데이터 로딩 시작
        new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
            // 즉시 표시할 수 있는 가벼운 데이터부터 로드
            loadGreeting();

            // 무거운 작업들은 추가 지연 후 실행
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                loadDataAsync();
            }, 100); // 100ms 후 실행
        });
    }

    /**
     * 무거운 데이터 로딩 작업들을 비동기로 실행
     */
    private void loadDataAsync() {
        // 백그라운드 스레드에서 실행
        new Thread(() -> {
            try {
                // 날씨 정보 로드 (네트워크 호출)
                loadWeatherInfo();

                // 일정 데이터 로드 (데이터베이스 조회)
                loadTodaySchedules();
                loadTomorrowSchedules();
                loadTomorrowReminders();

                Log.d("HomeActivity", "✅ 비동기 데이터 로딩 완료");

            } catch (Exception e) {
                Log.e("HomeActivity", "❌ 비동기 데이터 로딩 오류", e);
                runOnUiThread(() -> {
                    // 오류 시 기본값 표시
                    showWeatherError();
                });
            }
        }).start();
    }

    private void initViews() {
        // 레이아웃에 존재하지 않는 뷰들은 null로 설정
        textWeatherInfo = null; // 레이아웃에 없음
        textGreeting = null; // 레이아웃에 없음
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
        try {
            Log.d("HomeActivity", "🔧 NavigationHelper를 사용한 바텀 네비게이션 설정");
            NavigationHelper.setupBottomNavigation(this, R.id.nav_home);
            Log.d("HomeActivity", "✅ 바텀 네비게이션 설정 완료");
        } catch (Exception e) {
            Log.e("HomeActivity", "❌ 바텀 네비게이션 설정 오류", e);
            e.printStackTrace();
        }
    }



    private void loadGreeting() {
        try {
            UserSession userSession = UserSession.getInstance(this);
            String userName = userSession.getCurrentUserName();

            // textGreeting이 없으므로 textCityName을 활용하여 사용자 정보 표시
            if (textCityName != null) {
                String greeting = (userName != null && !userName.isEmpty()) ?
                    userName + "님의 TimeMate" : "TimeMate";
                // 도시명 대신 사용자 인사말 표시는 하지 않고, 로그만 남김
                Log.d("HomeActivity", "사용자 인사: " + greeting);
            }
        } catch (Exception e) {
            Log.e("HomeActivity", "인사말 로드 오류", e);
        }
    }

    private void loadWeatherInfo() {
        try {
            weatherService.getCurrentWeather("Seoul", new WeatherService.WeatherCallback() {
                @Override
                public void onSuccess(WeatherService.WeatherInfo weather) {
                    runOnUiThread(() -> {
                        try {
                            // textWeatherInfo는 없으므로 로그만 남김
                            Log.d("HomeActivity", "날씨 정보 로드 성공: " + weather.getDisplayText());

                            // 날씨 섹션 세부 정보 업데이트
                            updateWeatherSection(weather);
                        } catch (Exception e) {
                            Log.e("HomeActivity", "날씨 정보 UI 업데이트 오류", e);
                        }
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        try {
                            Log.e("HomeActivity", "날씨 정보를 불러올 수 없습니다");
                            // 오류 시 기본값 표시
                            showWeatherError();
                        } catch (Exception e) {
                            Log.e("HomeActivity", "날씨 오류 UI 업데이트 오류", e);
                        }
                    });
                }
            });
        } catch (Exception e) {
            Log.e("HomeActivity", "날씨 서비스 호출 오류", e);
            showWeatherError();
        }
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
    protected void onPause() {
        super.onPause();
        Log.d("HomeActivity", "🔄 onPause - 백그라운드 작업 일시 정지");

        // 백그라운드로 이동 시 무거운 작업들 일시 정지
        try {
            if (weatherService != null) {
                // 진행 중인 네트워크 요청이 있다면 취소하지는 않지만 새로운 요청은 방지
                Log.d("HomeActivity", "WeatherService 일시 정지");
            }
        } catch (Exception e) {
            Log.e("HomeActivity", "onPause 중 오류", e);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d("HomeActivity", "🛑 onStop - 리소스 일시 정리");

        try {
            // 메모리 정리 힌트
            System.gc();
            Log.d("HomeActivity", "✅ onStop 메모리 정리 완료");
        } catch (Exception e) {
            Log.e("HomeActivity", "onStop 중 오류", e);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("HomeActivity", "🧹 HomeActivity 리소스 정리 시작");

        try {
            // WeatherService 안전하게 종료
            if (weatherService != null) {
                Log.d("HomeActivity", "WeatherService 종료 중...");
                weatherService.shutdown();
                weatherService = null;
                Log.d("HomeActivity", "✅ WeatherService 종료 완료");
            }

            // Presenter 정리
            if (presenter != null) {
                Log.d("HomeActivity", "HomePresenter 정리 중...");
                presenter.destroy();
                presenter = null;
                Log.d("HomeActivity", "✅ HomePresenter 정리 완료");
            }

            // 어댑터들 정리
            if (todayAdapter != null) {
                todayAdapter = null;
            }
            if (tomorrowAdapter != null) {
                tomorrowAdapter = null;
            }
            if (reminderAdapter != null) {
                reminderAdapter = null;
            }

            Log.d("HomeActivity", "✅ HomeActivity 리소스 정리 완료");

        } catch (Exception e) {
            Log.e("HomeActivity", "❌ HomeActivity 리소스 정리 중 오류", e);
        }
    }
}
