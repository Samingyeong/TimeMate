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

        // 4) 캘린더 및 OOTD 추천 시스템 초기화
        setupCalendar();
        setupOotdRecommendation();

        // 5) RecyclerView 설정
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerOotd.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

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
        
        // 캘린더 관련 UI
        calendarView = findViewById(R.id.calendarView);
        btnPrevMonth = findViewById(R.id.btnPrevMonth);
        btnNextMonth = findViewById(R.id.btnNextMonth);
        textCurrentMonth = findViewById(R.id.textCurrentMonth);

        // OOTD 추천 시스템
        recyclerOotd = findViewById(R.id.recyclerOotd);
        textOotdDescription = findViewById(R.id.textOotdDescription);
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

    private void setupCalendar() {
        if (calendarView != null) {
            // 현재 월 표시 업데이트
            updateCurrentMonthDisplay();

            // 일정이 있는 날짜 로드
            loadScheduleDates();

            // 캘린더 날짜 클릭 리스너
            calendarView.setOnDateClickListener(this::showScheduleDetailDialog);

            // 월 이동 버튼 리스너
            btnPrevMonth.setOnClickListener(v -> {
                calendarView.previousMonth();
                updateCurrentMonthDisplay();
                loadScheduleDates();
            });

            btnNextMonth.setOnClickListener(v -> {
                calendarView.nextMonth();
                updateCurrentMonthDisplay();
                loadScheduleDates();
            });
        }
    }

    private void updateCurrentMonthDisplay() {
        if (textCurrentMonth != null && calendarView != null) {
            textCurrentMonth.setText(calendarView.getCurrentMonthYear());
        }
    }

    private void loadScheduleDates() {
        String currentUserId = userSession.getCurrentUserId();
        if (currentUserId == null || calendarView == null) return;

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                // 현재 표시된 월의 모든 일정 조회
                Calendar cal = calendarView.getCalendar();
                int year = cal.get(Calendar.YEAR);
                int month = cal.get(Calendar.MONTH);

                // 해당 월의 첫날과 마지막날 계산
                Calendar startCal = Calendar.getInstance();
                startCal.set(year, month, 1);
                Calendar endCal = Calendar.getInstance();
                endCal.set(year, month, startCal.getActualMaximum(Calendar.DAY_OF_MONTH));

                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                String startDate = dateFormat.format(startCal.getTime());
                String endDate = dateFormat.format(endCal.getTime());

                List<Schedule> monthSchedules = db.scheduleDao().getSchedulesByDateRange(currentUserId, startDate, endDate);

                // 일정이 있는 날짜들을 Set으로 변환
                Set<String> scheduleDates = new HashSet<>();
                for (Schedule schedule : monthSchedules) {
                    String scheduleDate = dateFormat.format(new Date(schedule.dateTime));
                    scheduleDates.add(scheduleDate);
                }

                runOnUiThread(() -> {
                    calendarView.setScheduleDates(scheduleDates);
                });

            } catch (Exception e) {
                // 에러 처리
            }
        });
    }

    private void showScheduleDetailDialog(Calendar selectedDate) {
        String currentUserId = userSession.getCurrentUserId();
        if (currentUserId == null) return;

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String selectedDateStr = dateFormat.format(selectedDate.getTime());

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                List<Schedule> daySchedules = db.scheduleDao().getSchedulesByDate(currentUserId, selectedDateStr);

                runOnUiThread(() -> {
                    if (daySchedules.isEmpty()) {
                        // 일정이 없는 경우 간단한 다이얼로그
                        showEmptyScheduleDialog(selectedDate);
                    } else {
                        // 일정이 있는 경우 상세 다이얼로그
                        showScheduleDetailDialog(selectedDate, daySchedules);
                    }
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "일정을 불러올 수 없습니다", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void showEmptyScheduleDialog(Calendar selectedDate) {
        SimpleDateFormat displayFormat = new SimpleDateFormat("yyyy년 MM월 dd일", Locale.KOREAN);
        String dateStr = displayFormat.format(selectedDate.getTime());

        new AlertDialog.Builder(this)
                .setTitle(dateStr)
                .setMessage("이 날짜에는 등록된 일정이 없습니다.")
                .setPositiveButton("일정 추가", (dialog, which) -> {
                    Intent intent = new Intent(this, ScheduleAddActivity.class);
                    startActivity(intent);
                })
                .setNegativeButton("닫기", null)
                .show();
    }

    private void showScheduleDetailDialog(Calendar selectedDate, List<Schedule> schedules) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_schedule_detail, null);

        TextView textDialogDate = dialogView.findViewById(R.id.textDialogDate);
        ViewPager2 viewPagerSchedules = dialogView.findViewById(R.id.viewPagerSchedules);
        LinearLayout layoutIndicator = dialogView.findViewById(R.id.layoutIndicator);
        Button btnAddSchedule = dialogView.findViewById(R.id.btnAddSchedule);
        Button btnViewAll = dialogView.findViewById(R.id.btnViewAll);

        SimpleDateFormat displayFormat = new SimpleDateFormat("yyyy년 MM월 dd일", Locale.KOREAN);
        textDialogDate.setText(displayFormat.format(selectedDate.getTime()));

        // ViewPager 어댑터 설정
        ScheduleDetailAdapter adapter = new ScheduleDetailAdapter(this, schedules);
        viewPagerSchedules.setAdapter(adapter);

        // 페이지 인디케이터 설정 (2개 이상일 때만 표시)
        if (schedules.size() > 1) {
            layoutIndicator.setVisibility(View.VISIBLE);
            setupPageIndicator(layoutIndicator, schedules.size(), viewPagerSchedules);
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        // 버튼 리스너
        btnAddSchedule.setOnClickListener(v -> {
            dialog.dismiss();
            Intent intent = new Intent(this, ScheduleAddActivity.class);
            startActivity(intent);
        });

        btnViewAll.setOnClickListener(v -> {
            dialog.dismiss();
            Intent intent = new Intent(this, ScheduleListActivity.class);
            startActivity(intent);
        });

        dialogView.findViewById(R.id.btnCloseDialog).setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void setupPageIndicator(LinearLayout layoutIndicator, int pageCount, ViewPager2 viewPager) {
        // 페이지 인디케이터 점들 생성
        for (int i = 0; i < pageCount; i++) {
            View dot = new View(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(20, 20);
            params.setMargins(8, 0, 8, 0);
            dot.setLayoutParams(params);
            dot.setBackgroundResource(R.drawable.indicator_dot_inactive);
            layoutIndicator.addView(dot);
        }

        // 첫 번째 점 활성화
        if (pageCount > 0) {
            layoutIndicator.getChildAt(0).setBackgroundResource(R.drawable.indicator_dot_active);
        }

        // ViewPager 페이지 변경 리스너
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                for (int i = 0; i < layoutIndicator.getChildCount(); i++) {
                    View dot = layoutIndicator.getChildAt(i);
                    if (i == position) {
                        dot.setBackgroundResource(R.drawable.indicator_dot_active);
                    } else {
                        dot.setBackgroundResource(R.drawable.indicator_dot_inactive);
                    }
                }
            }
        });
    }

    private void setupOotdRecommendation() {
        // OOTD 추천 기능 구현 (향후 확장)
        if (textOotdDescription != null) {
            textOotdDescription.setText("현재 날씨에 맞는 옷차림을 추천해드려요");
        }

        // 임시 OOTD 데이터
        if (recyclerOotd != null) {
            List<String> ootdItems = new ArrayList<>();
            ootdItems.add("가벼운 니트");
            ootdItems.add("청바지");
            ootdItems.add("스니커즈");

            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, ootdItems);
            // RecyclerView 어댑터는 향후 구현
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
