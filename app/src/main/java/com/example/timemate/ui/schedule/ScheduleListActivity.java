package com.example.timemate.ui.schedule;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.timemate.R;
import com.example.timemate.data.database.AppDatabase;
import com.example.timemate.data.model.Schedule;
import com.example.timemate.ui.home.HomeActivity;
import com.example.timemate.ui.friend.FriendListActivity;
import com.example.timemate.features.profile.ProfileActivity;
import com.example.timemate.ui.recommendation.RecommendationActivity;
import com.example.timemate.util.UserSession;
import com.example.timemate.ui.home.CalendarView;
import com.example.timemate.ui.home.ScheduleDetailAdapter;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

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
 * 일정 목록 화면
 * - 사용자의 모든 일정 표시
 * - 일정 추가 FAB
 * - 바텀 네비게이션
 */
public class ScheduleListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TextView textEmptySchedule;
    private ImprovedScheduleAdapter adapter;
    private AppDatabase db;
    private UserSession userSession;

    // 캘린더 관련 UI
    private CalendarView calendarView;
    private Button btnPrevMonth, btnNextMonth;
    private TextView textCurrentMonth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule_list);

        userSession = UserSession.getInstance(this);

        // 로그인 상태 확인
        if (!userSession.isLoggedIn()) {
            finish();
            return;
        }

        initViews();
        setupDatabase();
        setupClickListeners();
        setupBottomNavigation();
        loadSchedules();
    }

    private void initViews() {
        // RecyclerView 초기화
        recyclerView = findViewById(R.id.recyclerSchedules);
        textEmptySchedule = findViewById(R.id.textEmptySchedule);

        if (recyclerView != null) {
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
        }

        // 캘린더 관련 UI
        calendarView = findViewById(R.id.calendarView);
        btnPrevMonth = findViewById(R.id.btnPrevMonth);
        btnNextMonth = findViewById(R.id.btnNextMonth);
        textCurrentMonth = findViewById(R.id.textCurrentMonth);

        setupCalendar();
    }

    private void setupDatabase() {
        db = AppDatabase.getDatabase(this);
    }

    private void setupClickListeners() {
        FloatingActionButton fabAdd = findViewById(R.id.fabAddSchedule);
        fabAdd.setOnClickListener(v -> {
            startActivity(new Intent(ScheduleListActivity.this, com.example.timemate.features.schedule.ScheduleAddActivity.class));
        });
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigationView);
        bottomNav.setSelectedItemId(R.id.nav_schedule); // 일정관리 화면

        bottomNav.setOnItemSelectedListener(new BottomNavigationView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();
                if (id == R.id.nav_home) {
                    startActivity(new Intent(ScheduleListActivity.this, HomeActivity.class));
                    finish();
                    return true;
                } else if (id == R.id.nav_schedule) {
                    return true; // 현재 화면
                } else if (id == R.id.nav_friends) {
                    startActivity(new Intent(ScheduleListActivity.this, FriendListActivity.class));
                    finish();
                    return true;
                } else if (id == R.id.nav_recommendation) {
                    startActivity(new Intent(ScheduleListActivity.this, RecommendationActivity.class));
                    finish();
                    return true;
                } else if (id == R.id.nav_profile) {
                    startActivity(new Intent(ScheduleListActivity.this, ProfileActivity.class));
                    finish();
                    return true;
                }
                return false;
            }
        });
    }

    private void loadSchedules() {
        Log.d("ScheduleListActivity", "🔄 일정 로드 시작");

        String currentUserId = userSession.getCurrentUserId();
        Log.d("ScheduleListActivity", "현재 사용자 ID: " + currentUserId);

        if (currentUserId == null) {
            Log.e("ScheduleListActivity", "❌ 사용자 ID가 null입니다");
            showEmptyState();
            return;
        }

        // 먼저 빈 어댑터 설정
        setupEmptyAdapter();

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                Log.d("ScheduleListActivity", "📊 데이터베이스에서 일정 조회 중...");
                List<Schedule> schedules = db.scheduleDao().getSchedulesByUserId(currentUserId);
                Log.d("ScheduleListActivity", "📊 조회된 일정 수: " + (schedules != null ? schedules.size() : "null"));

                // 일정 데이터 상세 로그
                if (schedules != null && !schedules.isEmpty()) {
                    for (int i = 0; i < Math.min(schedules.size(), 3); i++) {
                        Schedule s = schedules.get(i);
                        Log.d("ScheduleListActivity", String.format("일정 %d: ID=%d, 제목=%s, 날짜=%s, 시간=%s",
                            i+1, s.id, s.title, s.date, s.time));
                    }
                }

                runOnUiThread(() -> {
                    if (schedules == null || schedules.isEmpty()) {
                        Log.d("ScheduleListActivity", "📭 일정이 없어서 Empty State 표시");
                        showEmptyState();
                    } else {
                        Log.d("ScheduleListActivity", "📋 일정이 있어서 목록 표시");
                        showSchedules(schedules);
                    }
                });
            } catch (Exception e) {
                Log.e("ScheduleListActivity", "❌ 일정 로드 중 오류", e);
                runOnUiThread(() -> {
                    showEmptyState();
                    Toast.makeText(this, "일정 목록을 불러오는데 실패했습니다: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void setupEmptyAdapter() {
        if (recyclerView != null) {
            List<Schedule> emptyList = new ArrayList<>();
            adapter = new ImprovedScheduleAdapter(emptyList, this);
            recyclerView.setAdapter(adapter);
        }
    }

    private void showSchedules(List<Schedule> schedules) {
        if (recyclerView != null) {
            adapter = new ImprovedScheduleAdapter(schedules, this);
            recyclerView.setAdapter(adapter);
            recyclerView.setVisibility(View.VISIBLE);
        }
        if (textEmptySchedule != null) {
            textEmptySchedule.setVisibility(View.GONE);
        }
    }

    private void showEmptyState() {
        if (recyclerView != null) {
            recyclerView.setVisibility(View.GONE);
        }
        if (textEmptySchedule != null) {
            textEmptySchedule.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 화면이 다시 보일 때마다 일정 목록 새로고침
        loadSchedules();
        // 캘린더 날짜 새로고침
        if (calendarView != null) {
            loadScheduleDates();
        }
    }

    private void setupCalendar() {
        if (calendarView != null) {
            // 현재 월 표시 업데이트
            updateCurrentMonthDisplay();

            // 일정이 있는 날짜 로드
            loadScheduleDates();

            // 캘린더 날짜 클릭 리스너
            calendarView.setOnDateClickListener(this::onDateClicked);

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

                // getSchedulesByDateRange는 2개 파라미터만 받으므로 수정
                List<Schedule> monthSchedules = db.scheduleDao().getSchedulesByDateRange(startDate, endDate);

                // 일정이 있는 날짜들을 Set으로 변환
                Set<String> scheduleDates = new HashSet<>();
                for (Schedule schedule : monthSchedules) {
                    scheduleDates.add(schedule.date); // date 필드는 이미 yyyy-MM-dd 형식
                }

                runOnUiThread(() -> {
                    calendarView.setScheduleDates(scheduleDates);
                });

            } catch (Exception e) {
                // 에러 처리
            }
        });
    }

    private void onDateClicked(Calendar selectedDate) {
        String currentUserId = userSession.getCurrentUserId();
        if (currentUserId == null) return;

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String selectedDateStr = dateFormat.format(selectedDate.getTime());

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                // getSchedulesByDate 메서드가 없으므로 getSchedulesByUserAndDateRange 사용
                List<Schedule> daySchedules = db.scheduleDao().getSchedulesByUserAndDateRange(currentUserId, selectedDateStr, selectedDateStr);

                runOnUiThread(() -> {
                    SimpleDateFormat displayFormat = new SimpleDateFormat("yyyy년 MM월 dd일", Locale.KOREAN);
                    String dateStr = displayFormat.format(selectedDate.getTime());

                    if (daySchedules.isEmpty()) {
                        // 일정이 없는 경우
                        new AlertDialog.Builder(this)
                                .setTitle(dateStr)
                                .setMessage("이 날짜에는 등록된 일정이 없습니다.")
                                .setPositiveButton("일정 추가", (dialog, which) -> {
                                    Intent intent = new Intent(this, com.example.timemate.features.schedule.ScheduleAddActivity.class);
                                    startActivity(intent);
                                })
                                .setNegativeButton("닫기", null)
                                .show();
                    } else {
                        // 일정이 있는 경우 - 간단한 목록 표시
                        StringBuilder scheduleList = new StringBuilder();
                        for (int i = 0; i < daySchedules.size(); i++) {
                            Schedule schedule = daySchedules.get(i);
                            scheduleList.append(String.format("%d. %s (%s)\n   %s → %s\n\n",
                                    i + 1, schedule.title, schedule.time,
                                    schedule.departure, schedule.destination));
                        }

                        new AlertDialog.Builder(this)
                                .setTitle(dateStr + " 일정")
                                .setMessage(scheduleList.toString())
                                .setPositiveButton("일정 추가", (dialog, which) -> {
                                    Intent intent = new Intent(this, com.example.timemate.features.schedule.ScheduleAddActivity.class);
                                    startActivity(intent);
                                })
                                .setNegativeButton("닫기", null)
                                .show();
                    }
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "일정을 불러올 수 없습니다", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
}
