package com.example.timemate.features.schedule;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.timemate.R;
import com.example.timemate.data.database.AppDatabase;
import com.example.timemate.data.model.Schedule;
import com.example.timemate.util.UserSession;
import com.example.timemate.features.home.HomeActivity;
import com.example.timemate.features.friend.FriendListActivity;
import com.example.timemate.features.profile.ProfileActivity;
import com.example.timemate.features.schedule.adapter.ScheduleListAdapter;
import com.example.timemate.ui.recommendation.RecommendationActivity;
import com.example.timemate.features.home.CalendarView;
import com.example.timemate.utils.NavigationHelper;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 일정 목록 화면
 */
public class ScheduleListActivity extends AppCompatActivity {

    private static final String TAG = "ScheduleListActivity";
    private static final int REQUEST_ADD_SCHEDULE = 1001;
    
    private AppDatabase database;
    private UserSession userSession;
    private ExecutorService executor;
    
    private RecyclerView recyclerSchedules;
    private ScheduleListAdapter scheduleAdapter;
    private BottomNavigationView bottomNavigation;
    private FloatingActionButton fabAddSchedule;
    private View layoutEmptyState;

    // 캘린더 관련
    private CalendarView calendarView;
    private TextView textCurrentMonth;
    private Button btnPrevMonth, btnNextMonth;

    private List<Schedule> scheduleList = new ArrayList<>();
    private Set<String> scheduleDates = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            Log.d(TAG, "🚀 ScheduleListActivity onCreate 시작");

            // 레이아웃 설정
            Log.d(TAG, "📱 레이아웃 설정 중...");
            setContentView(R.layout.activity_schedule_list);
            Log.d(TAG, "✅ 레이아웃 설정 완료");

            // 뷰 초기화
            Log.d(TAG, "🔧 뷰 초기화 중...");
            initViews();
            Log.d(TAG, "✅ 뷰 초기화 완료");

            // 서비스 초기화
            Log.d(TAG, "⚙️ 서비스 초기화 중...");
            initServices();
            Log.d(TAG, "✅ 서비스 초기화 완료");

            // UserSession 안전성 확인
            if (userSession == null) {
                Log.e(TAG, "❌ UserSession 초기화 실패");
                Toast.makeText(this, "사용자 세션을 불러올 수 없습니다", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            Log.d(TAG, "✅ UserSession 확인 완료");

            // RecyclerView 설정
            Log.d(TAG, "📋 RecyclerView 설정 중...");
            setupRecyclerView();
            Log.d(TAG, "✅ RecyclerView 설정 완료");

            // 캘린더 설정
            Log.d(TAG, "🗓️ 캘린더 설정 중...");
            setupCalendar();
            Log.d(TAG, "✅ 캘린더 설정 완료");

            // 바텀 네비게이션 설정
            Log.d(TAG, "🧭 바텀 네비게이션 설정 중...");
            setupBottomNavigation();
            Log.d(TAG, "✅ 바텀 네비게이션 설정 완료");

            // 클릭 리스너 설정
            Log.d(TAG, "👆 클릭 리스너 설정 중...");
            setupClickListeners();
            Log.d(TAG, "✅ 클릭 리스너 설정 완료");

            // 로그인 상태 확인
            if (!userSession.isLoggedIn()) {
                Log.w(TAG, "⚠️ 사용자가 로그인되지 않음");
                Toast.makeText(this, "로그인이 필요합니다", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            Log.d(TAG, "✅ 로그인 상태 확인 완료");

            // 데이터베이스 테이블 상태 확인
            Log.d(TAG, "🗄️ 데이터베이스 확인 중...");
            verifyDatabaseTables();

            // 테스트 일정 생성 (개발용)
            Log.d(TAG, "🧪 테스트 데이터 확인 중...");
            createTestScheduleIfEmpty();

            // 일정 로드
            Log.d(TAG, "📊 일정 로드 중...");
            loadSchedules();

            Log.d(TAG, "🎉 ScheduleListActivity onCreate 완료");

        } catch (Exception e) {
            Log.e(TAG, "❌ ScheduleListActivity onCreate 오류", e);
            e.printStackTrace();

            // 사용자에게 구체적인 오류 정보 제공
            String errorMessage = "일정 화면을 불러오는 중 오류가 발생했습니다";
            if (e.getMessage() != null) {
                errorMessage += ": " + e.getMessage();
            }

            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();

            // 안전하게 종료
            try {
                finish();
            } catch (Exception finishException) {
                Log.e(TAG, "finish() 호출 중 오류", finishException);
            }
        }
    }

    private void initViews() {
        try {
            Log.d(TAG, "🔧 뷰 초기화 시작");

            // 필수 뷰들
            recyclerSchedules = findViewById(R.id.recyclerSchedules);
            if (recyclerSchedules == null) {
                throw new RuntimeException("recyclerSchedules를 찾을 수 없습니다");
            }

            bottomNavigation = findViewById(R.id.bottomNavigationView);
            if (bottomNavigation == null) {
                throw new RuntimeException("bottomNavigationView를 찾을 수 없습니다");
            }

            fabAddSchedule = findViewById(R.id.fabAddSchedule);
            if (fabAddSchedule == null) {
                throw new RuntimeException("fabAddSchedule를 찾을 수 없습니다");
            }

            layoutEmptyState = findViewById(R.id.layoutEmptyState);
            // layoutEmptyState는 선택사항이므로 null 체크만

            // 캘린더 뷰들 (안전하게 초기화)
            try {
                calendarView = findViewById(R.id.calendarView);
                textCurrentMonth = findViewById(R.id.textCurrentMonth);
                btnPrevMonth = findViewById(R.id.btnPrevMonth);
                btnNextMonth = findViewById(R.id.btnNextMonth);
                Log.d(TAG, "✅ 캘린더 뷰 초기화 완료");
            } catch (Exception e) {
                Log.w(TAG, "⚠️ 캘린더 뷰 초기화 실패 (선택사항)", e);
                // 캘린더 뷰는 선택사항이므로 계속 진행
            }

            Log.d(TAG, "✅ 뷰 초기화 완료");

        } catch (Exception e) {
            Log.e(TAG, "❌ 뷰 초기화 오류", e);
            throw e; // 상위로 예외 전파
        }
    }

    private void initServices() {
        try {
            Log.d(TAG, "서비스 초기화 시작");

            // 데이터베이스 초기화
            database = AppDatabase.getDatabase(this);
            if (database == null) {
                throw new RuntimeException("데이터베이스 초기화 실패");
            }
            Log.d(TAG, "데이터베이스 초기화 완료");

            // UserSession 초기화
            userSession = UserSession.getInstance(this);
            if (userSession == null) {
                throw new RuntimeException("UserSession 초기화 실패");
            }
            Log.d(TAG, "UserSession 초기화 완료");

            // Executor 초기화
            executor = Executors.newSingleThreadExecutor();
            Log.d(TAG, "서비스 초기화 완료");

        } catch (Exception e) {
            Log.e(TAG, "서비스 초기화 중 오류", e);
            e.printStackTrace();
            throw e; // 상위로 예외 전파
        }
    }

    private void setupRecyclerView() {
        try {
            Log.d(TAG, "📋 RecyclerView 설정 시작");

            if (recyclerSchedules == null) {
                throw new RuntimeException("recyclerSchedules가 null입니다");
            }

            if (scheduleList == null) {
                scheduleList = new ArrayList<>();
                Log.d(TAG, "scheduleList 초기화 완료");
            }

            scheduleAdapter = new ScheduleListAdapter(scheduleList, new ScheduleListAdapter.OnScheduleClickListener() {
                @Override
                public void onScheduleClick(Schedule schedule) {
                    try {
                        Log.d(TAG, "일정 클릭: " + schedule.title);
                        openScheduleDetail(schedule);
                    } catch (Exception e) {
                        Log.e(TAG, "일정 클릭 처리 오류", e);
                    }
                }

                @Override
                public void onEditClick(Schedule schedule) {
                    try {
                        Log.d(TAG, "일정 수정 클릭: " + schedule.title);
                        editSchedule(schedule);
                    } catch (Exception e) {
                        Log.e(TAG, "일정 수정 처리 오류", e);
                    }
                }

                @Override
                public void onDeleteClick(Schedule schedule) {
                    try {
                        Log.d(TAG, "일정 삭제 클릭: " + schedule.title);
                        deleteSchedule(schedule);
                    } catch (Exception e) {
                        Log.e(TAG, "일정 삭제 처리 오류", e);
                    }
                }

                @Override
                public void onCompleteToggle(Schedule schedule) {
                    try {
                        Log.d(TAG, "일정 완료 토글: " + schedule.title);
                        toggleScheduleCompletion(schedule);
                    } catch (Exception e) {
                        Log.e(TAG, "일정 완료 토글 처리 오류", e);
                    }
                }
            });

            recyclerSchedules.setLayoutManager(new LinearLayoutManager(this));
            recyclerSchedules.setAdapter(scheduleAdapter);

            Log.d(TAG, "✅ RecyclerView 설정 완료");

        } catch (Exception e) {
            Log.e(TAG, "❌ RecyclerView 설정 오류", e);
            throw e; // 상위로 예외 전파
        }
    }

    /**
     * 캘린더 설정
     */
    private void setupCalendar() {
        try {
            Log.d(TAG, "🗓️ 캘린더 설정 시작");

            // 캘린더 뷰가 있는지 확인
            if (calendarView == null) {
                Log.w(TAG, "⚠️ CalendarView가 null입니다. 캘린더 기능을 건너뜁니다.");
                return;
            }

            // 현재 월 표시 업데이트
            updateCurrentMonthDisplay();

            // 캘린더 날짜 클릭 리스너
            calendarView.setOnDateClickListener(date -> {
                try {
                    Log.d(TAG, "캘린더 날짜 클릭: " + date.getTime());
                    showSchedulesForDate(date);
                } catch (Exception e) {
                    Log.e(TAG, "날짜 클릭 처리 오류", e);
                }
            });

            // 이전/다음 월 버튼 (null 체크)
            if (btnPrevMonth != null) {
                btnPrevMonth.setOnClickListener(v -> {
                    if (calendarView != null) {
                        calendarView.previousMonth();
                        updateCurrentMonthDisplay();
                        updateCalendarSchedules();
                    }
                });
            }

            if (btnNextMonth != null) {
                btnNextMonth.setOnClickListener(v -> {
                    if (calendarView != null) {
                        calendarView.nextMonth();
                        updateCurrentMonthDisplay();
                        updateCalendarSchedules();
                    }
                });
            }

            Log.d(TAG, "✅ 캘린더 설정 완료");

        } catch (Exception e) {
            Log.e(TAG, "❌ 캘린더 설정 오류", e);
            // 캘린더 설정 실패는 치명적이지 않으므로 계속 진행
        }
    }

    /**
     * 현재 월 표시 업데이트
     */
    private void updateCurrentMonthDisplay() {
        try {
            if (calendarView != null && textCurrentMonth != null) {
                textCurrentMonth.setText(calendarView.getCurrentMonthYear());
            }
        } catch (Exception e) {
            Log.e(TAG, "현재 월 표시 업데이트 오류", e);
        }
    }

    /**
     * 특정 날짜의 일정 표시 (과거 일정 포함)
     */
    private void showSchedulesForDate(Calendar date) {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            String selectedDateStr = dateFormat.format(date.getTime());

            // 데이터베이스에서 해당 날짜의 모든 일정 조회 (과거 일정 포함)
            String currentUserId = userSession.getCurrentUserId();
            if (currentUserId == null) {
                Toast.makeText(this, "로그인이 필요합니다", Toast.LENGTH_SHORT).show();
                return;
            }

            executor.execute(() -> {
                try {
                    // 해당 날짜의 모든 일정 조회 (과거 일정도 포함)
                    List<Schedule> daySchedules = database.scheduleDao().getSchedulesByUserAndDateRange(
                        currentUserId, selectedDateStr, selectedDateStr);

                    runOnUiThread(() -> {
                        if (daySchedules.isEmpty()) {
                            Toast.makeText(this, "선택한 날짜에 일정이 없습니다", Toast.LENGTH_SHORT).show();
                        } else {
                            showScheduleDetailDialog(daySchedules, selectedDateStr);
                        }
                    });

                } catch (Exception e) {
                    Log.e(TAG, "날짜별 일정 조회 오류", e);
                    runOnUiThread(() -> {
                        Toast.makeText(this, "일정을 불러올 수 없습니다", Toast.LENGTH_SHORT).show();
                    });
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "날짜별 일정 표시 오류", e);
            Toast.makeText(this, "일정을 불러올 수 없습니다", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 날짜별 일정 상세 다이얼로그
     */
    private void showScheduleDetailDialog(List<Schedule> schedules, String date) {
        try {
            StringBuilder message = new StringBuilder();
            message.append("📅 ").append(date).append("\n\n");

            for (int i = 0; i < schedules.size(); i++) {
                Schedule schedule = schedules.get(i);
                message.append("• ").append(schedule.title != null ? schedule.title : "제목 없음");

                if (schedule.time != null) {
                    message.append(" (").append(schedule.time).append(")");
                }

                if (i < schedules.size() - 1) {
                    message.append("\n");
                }
            }

            new AlertDialog.Builder(this)
                .setTitle("일정 목록")
                .setMessage(message.toString())
                .setPositiveButton("확인", null)
                .setNeutralButton("일정 추가", (dialog, which) -> {
                    Intent intent = new Intent(this, ScheduleAddActivity.class);
                    intent.putExtra("selected_date", date);
                    startActivityForResult(intent, REQUEST_ADD_SCHEDULE);
                })
                .show();

        } catch (Exception e) {
            Log.e(TAG, "일정 상세 다이얼로그 표시 오류", e);
        }
    }

    private void setupBottomNavigation() {
        try {
            Log.d(TAG, "공통 네비게이션 헬퍼 사용");
            NavigationHelper.setupBottomNavigation(this, R.id.nav_schedule);
        } catch (Exception e) {
            Log.e(TAG, "바텀 네비게이션 설정 오류", e);
        }
    }

    private void setupClickListeners() {
        fabAddSchedule.setOnClickListener(v -> {
            Intent intent = new Intent(this, ScheduleAddActivity.class);
            startActivityForResult(intent, REQUEST_ADD_SCHEDULE);
        });

        // 디버깅용: FAB 길게 누르면 테스트 사용자 생성
        fabAddSchedule.setOnLongClickListener(v -> {
            createTestUsers();
            return true;
        });

        // 디버깅용 강제 새로고침 (헤더 더블 탭)
        if (findViewById(R.id.layoutHeader) != null) {
            findViewById(R.id.layoutHeader).setOnClickListener(new View.OnClickListener() {
                private long lastClickTime = 0;

                @Override
                public void onClick(View v) {
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - lastClickTime < 500) { // 더블 탭
                        Log.d(TAG, "🔄 헤더 더블 탭 - 강제 새로고침 실행");
                        Toast.makeText(ScheduleListActivity.this, "강제 새로고침 중...", Toast.LENGTH_SHORT).show();
                        loadSchedules();
                        verifyDatabaseTables(); // 데이터베이스 상태 확인
                    }
                    lastClickTime = currentTime;
                }
            });
        }
    }

    /**
     * 데이터베이스 테이블 상태 확인
     */
    private void verifyDatabaseTables() {
        executor.execute(() -> {
            try {
                Log.d(TAG, "📊 데이터베이스 테이블 상태 확인 시작");

                // 간단한 쿼리로 테이블 존재 확인
                int scheduleCount = database.scheduleDao().getAllSchedules().size();
                Log.d(TAG, "✅ schedules 테이블 접근 성공, 총 일정 수: " + scheduleCount);

                // 사용자별 일정 수 확인
                String currentUserId = userSession.getCurrentUserId();
                if (currentUserId != null) {
                    int userScheduleCount = database.scheduleDao().getSchedulesByUserId(currentUserId).size();
                    Log.d(TAG, "👤 사용자(" + currentUserId + ") 일정 수: " + userScheduleCount);
                } else {
                    Log.w(TAG, "⚠️ 현재 사용자 ID가 null입니다");
                }

            } catch (Exception e) {
                Log.e(TAG, "❌ 데이터베이스 테이블 확인 중 오류", e);
                e.printStackTrace();

                runOnUiThread(() -> {
                    Toast.makeText(this, "데이터베이스 접근 오류가 발생했습니다", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    /**
     * 테스트 일정 생성 (개발용)
     */
    private void createTestScheduleIfEmpty() {
        executor.execute(() -> {
            try {
                String currentUserId = userSession.getCurrentUserId();
                if (currentUserId == null) return;

                // 기존 일정 수 확인
                List<Schedule> existingSchedules = database.scheduleDao().getSchedulesByUserId(currentUserId);

                if (existingSchedules.isEmpty()) {
                    Log.d(TAG, "🔧 일정이 없어서 테스트 일정 생성");

                    // 테스트 일정 생성
                    Schedule testSchedule = new Schedule();
                    testSchedule.userId = currentUserId;
                    testSchedule.title = "테스트 일정";
                    testSchedule.date = "2024-12-20";
                    testSchedule.time = "14:00";
                    testSchedule.departure = "서울역";
                    testSchedule.destination = "강남역";
                    testSchedule.memo = "테스트용 일정입니다";
                    testSchedule.isCompleted = false;
                    testSchedule.createdAt = System.currentTimeMillis();
                    testSchedule.updatedAt = System.currentTimeMillis();

                    long insertedId = database.scheduleDao().insert(testSchedule);
                    Log.d(TAG, "✅ 테스트 일정 생성 완료: ID=" + insertedId);

                    runOnUiThread(() -> {
                        Toast.makeText(this, "테스트 일정이 생성되었습니다", Toast.LENGTH_SHORT).show();
                    });
                } else {
                    Log.d(TAG, "📋 기존 일정이 " + existingSchedules.size() + "개 있어서 테스트 일정 생성 안함");
                }

            } catch (Exception e) {
                Log.e(TAG, "❌ 테스트 일정 생성 오류", e);
            }
        });
    }

    private void loadSchedules() {
        Log.d(TAG, "🔄 일정 로드 시작");

        executor.execute(() -> {
            try {
                // 강화된 사용자 세션 디버깅
                Log.d(TAG, "🔍 === 사용자 세션 디버깅 ===");
                Log.d(TAG, "🔍 로그인 상태: " + userSession.isLoggedIn());
                String currentUserId = userSession.getCurrentUserId();
                Log.d(TAG, "🔍 현재 사용자 ID: '" + currentUserId + "'");
                Log.d(TAG, "🔍 현재 사용자 이름: '" + userSession.getCurrentUserName() + "'");
                Log.d(TAG, "🔍 현재 사용자 이메일: '" + userSession.getCurrentUserEmail() + "'");
                Log.d(TAG, "🔍 ========================");

                if (currentUserId == null || currentUserId.trim().isEmpty()) {
                    Log.w(TAG, "⚠️ 사용자 ID가 null - 로그인 필요");
                    runOnUiThread(() -> {
                        Toast.makeText(this, "로그인이 필요합니다", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                    return;
                }

                // 데이터베이스 연결 확인
                if (database == null) {
                    Log.e(TAG, "❌ 데이터베이스가 null입니다");
                    runOnUiThread(() -> {
                        Toast.makeText(this, "데이터베이스 연결 오류", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                Log.d(TAG, "📊 데이터베이스에서 일정 조회 중...");

                // 오늘 날짜 계산
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                String todayDate = dateFormat.format(new java.util.Date());

                // 먼저 데이터베이스 전체 일정 수 확인 (디버깅용)
                List<Schedule> allSchedulesInDB = database.scheduleDao().getAllSchedules();
                Log.d(TAG, "🗄️ 데이터베이스 전체 일정 수: " + (allSchedulesInDB != null ? allSchedulesInDB.size() : "null"));

                // 사용자별 일정 수도 확인
                int userScheduleCount = database.scheduleDao().getScheduleCountByUserId(currentUserId);
                Log.d(TAG, "👤 현재 사용자(" + currentUserId + ")의 일정 수: " + userScheduleCount);

                // 현재 사용자의 일정만 조회
                List<Schedule> schedules = database.scheduleDao().getSchedulesByUserId(currentUserId);
                Log.d(TAG, "📊 현재 사용자(" + currentUserId + ")의 일정 수: " + (schedules != null ? schedules.size() : "null"));

                // 전체 일정 상세 정보 로그 (디버깅용)
                if (allSchedulesInDB != null && !allSchedulesInDB.isEmpty()) {
                    Log.d(TAG, "=== 데이터베이스 전체 일정 목록 ===");
                    for (Schedule s : allSchedulesInDB) {
                        Log.d(TAG, String.format("전체일정: ID=%d, 제목=%s, 사용자ID=%s, 날짜=%s",
                            s.id, s.title, s.userId, s.date));
                    }
                    Log.d(TAG, "=== 전체 일정 목록 끝 ===");
                }

                // 현재 사용자 일정 상세 정보 로그 (NULL 안전 처리)
                if (schedules != null && !schedules.isEmpty()) {
                    Log.d(TAG, "=== 현재 사용자 일정 목록 ===");
                    for (Schedule s : schedules) {
                        // NULL 안전 처리
                        String safeTitle = (s.title != null) ? s.title : "제목없음";
                        String safeUserId = (s.userId != null) ? s.userId : "사용자ID없음";
                        String safeDate = (s.date != null) ? s.date : "날짜없음";

                        Log.d(TAG, String.format("내일정: ID=%d, 제목=%s, 사용자ID=%s, 날짜=%s",
                            s.id, safeTitle, safeUserId, safeDate));
                    }
                    Log.d(TAG, "=== 현재 사용자 일정 목록 끝 ===");
                } else {
                    Log.w(TAG, "⚠️ 현재 사용자의 일정이 없습니다!");
                }

                // 공유 일정 기능 제거됨 - 개인 일정만 지원

                Log.d(TAG, "📊 총 일정 수: " + (schedules != null ? schedules.size() : "null"));

                // 일정 데이터 상세 로그
                if (schedules != null && !schedules.isEmpty()) {
                    for (int i = 0; i < Math.min(schedules.size(), 3); i++) {
                        Schedule s = schedules.get(i);
                        Log.d(TAG, String.format("일정 %d: ID=%d, 제목=%s, 날짜=%s, 시간=%s",
                            i+1, s.id, s.title, s.date, s.time));
                    }
                }

                runOnUiThread(() -> {
                    try {
                        Log.d(TAG, "🔄 UI 업데이트 시작");

                        if (scheduleList == null) {
                            Log.e(TAG, "❌ scheduleList가 null입니다");
                            return;
                        }

                        if (scheduleAdapter == null) {
                            Log.e(TAG, "❌ scheduleAdapter가 null입니다");
                            return;
                        }

                        scheduleList.clear();
                        if (schedules != null) {
                            scheduleList.addAll(schedules);
                        }
                        scheduleAdapter.notifyDataSetChanged();

                        // 캘린더에 일정 날짜 업데이트
                        updateCalendarSchedules();

                        // 디버깅: 화면에 사용자 정보 표시
                        showDebugInfo(currentUserId, schedules, allSchedulesInDB);

                        // Empty State 처리
                        if (schedules == null || schedules.isEmpty()) {
                            Log.d(TAG, "📭 일정이 없어서 Empty State 표시");
                            showEmptyState();
                        } else {
                            Log.d(TAG, "📋 일정이 있어서 목록 표시");
                            hideEmptyState();
                        }

                        Log.d(TAG, "✅ 일정 목록 업데이트 완료: " + (schedules != null ? schedules.size() : 0) + "개");

                    } catch (Exception uiException) {
                        Log.e(TAG, "❌ UI 업데이트 중 오류", uiException);
                        Toast.makeText(this, "화면 업데이트 중 오류가 발생했습니다", Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "❌ 일정 로드 중 오류", e);
                runOnUiThread(() -> {
                    Toast.makeText(this, "일정 목록을 불러오는데 실패했습니다: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void openScheduleDetail(Schedule schedule) {
        try {
            if (schedule == null) {
                Toast.makeText(this, "일정 정보를 불러올 수 없습니다", Toast.LENGTH_SHORT).show();
                return;
            }

            // 안전한 필드 접근
            String title = schedule.title != null ? schedule.title : "제목 없음";
            String date = schedule.date != null ? schedule.date : "날짜 미정";
            String time = schedule.time != null ? schedule.time : "시간 미정";
            String departure = schedule.departure != null ? schedule.departure : "없음";
            String destination = schedule.destination != null ? schedule.destination : "없음";
            String memo = schedule.memo != null && !schedule.memo.trim().isEmpty() ? schedule.memo : "없음";

            // 경로 정보 추가 (개선된 버전)
            String routeInfoText = "";
            if (schedule.routeInfo != null && !schedule.routeInfo.isEmpty()) {
                routeInfoText = parseRouteInfo(schedule.routeInfo);
                Log.d(TAG, "📍 경로 정보 파싱 결과: " + routeInfoText);
            } else if (schedule.selectedTransportModes != null && !schedule.selectedTransportModes.isEmpty()) {
                // routeInfo가 없어도 selectedTransportModes가 있으면 표시
                routeInfoText = "선택된 교통수단: " + schedule.selectedTransportModes;
                Log.d(TAG, "🚌 교통수단 정보: " + routeInfoText);
            } else {
                Log.d(TAG, "⚠️ 경로 정보 없음 - routeInfo: " + schedule.routeInfo + ", transportModes: " + schedule.selectedTransportModes);
            }

            // 공유 친구 기능 제거됨 - 개인 일정만 지원
            String friendsText = "";

            // iOS 스타일 다이얼로그로 표시
            showIOSStyleScheduleDetail(schedule, title, date, time, departure, destination, memo, routeInfoText, friendsText);
        } catch (Exception e) {
            Toast.makeText(this, "일정 상세보기 중 오류가 발생했습니다", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Error opening schedule detail", e);
        }
    }

    private void editSchedule(Schedule schedule) {
        Intent intent = new Intent(this, ScheduleAddActivity.class);
        intent.putExtra("schedule_id", schedule.id);
        intent.putExtra("edit_mode", true);
        startActivity(intent);
    }

    /**
     * JSON 형태의 경로 정보를 사용자가 읽기 쉬운 형태로 변환 (실제 저장된 데이터만)
     */
    private String parseRouteInfo(String routeInfoJson) {
        try {
            if (routeInfoJson == null || routeInfoJson.isEmpty()) {
                return "";
            }

            Log.d(TAG, "파싱할 경로 정보: " + routeInfoJson);

            // 간단한 JSON 파싱 (org.json 사용)
            org.json.JSONObject json = new org.json.JSONObject(routeInfoJson);

            StringBuilder result = new StringBuilder();

            // 선택된 교통수단들 (아이콘 없이 깔끔하게)
            org.json.JSONArray selectedModes = json.optJSONArray("selectedModes");
            if (selectedModes != null && selectedModes.length() > 0) {
                result.append("교통수단: ");
                for (int i = 0; i < selectedModes.length(); i++) {
                    if (i > 0) result.append(", ");
                    String mode = selectedModes.optString(i);
                    if (!mode.isEmpty()) {
                        result.append(mode);
                    }
                }
                result.append("\n");
            }

            // 경로 상세 정보 (깔끔하게 정리)
            org.json.JSONArray routes = json.optJSONArray("routes");
            if (routes != null && routes.length() > 0) {
                result.append("\n경로 상세:\n");
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
                            // 교통수단명 표시 (아이콘 없이)
                            if (!name.isEmpty()) {
                                result.append("• ").append(name);
                            } else {
                                result.append("• ").append(getTransportModeName(mode));
                            }

                            // 시간과 비용 정보 표시 (아이콘 최소화)
                            boolean hasTimeOrCost = !duration.isEmpty() || !cost.isEmpty();
                            if (hasTimeOrCost) {
                                result.append(" (");

                                // 시간 정보
                                if (!duration.isEmpty()) {
                                    result.append(duration);
                                }

                                // 비용 정보
                                if (!cost.isEmpty()) {
                                    if (!duration.isEmpty()) result.append(", ");
                                    result.append(cost);
                                }

                                // 거리 정보 (있는 경우)
                                if (!distance.isEmpty()) {
                                    if (!duration.isEmpty() || !cost.isEmpty()) result.append(", ");
                                    result.append(distance);
                                }

                                result.append(")");
                            }

                            result.append("\n");

                            Log.d(TAG, "경로 파싱: " + mode + " - 시간: " + duration + ", 비용: " + cost);
                        }
                    }
                }
            }

            String finalResult = result.toString().trim();
            Log.d(TAG, "파싱된 경로 정보: " + finalResult);

            return finalResult.isEmpty() ? "" : finalResult;

        } catch (Exception e) {
            Log.e(TAG, "경로 정보 파싱 오류", e);
            return "";
        }
    }

    /**
     * 교통수단 모드에 따른 한글 이름 반환 (아이콘 없이)
     */
    private String getTransportModeName(String mode) {
        switch (mode.toLowerCase()) {
            case "transit": return "대중교통";
            case "driving": return "자동차";
            case "walking": return "도보";
            case "bicycle": return "자전거";
            case "taxi": return "택시";
            default: return "도보";
        }
    }

    /**
     * 교통수단 모드에 따른 아이콘 반환 (필요시에만 사용)
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

    // 공유 친구 기능 제거됨 - 개인 일정만 지원

    /**
     * iOS 스타일 일정 상세보기 다이얼로그
     */
    private void showIOSStyleScheduleDetail(Schedule schedule, String title, String date, String time,
                                          String departure, String destination, String memo,
                                          String routeInfoText, String friendsText) {

        // 먼저 간단한 테스트로 레이아웃 인플레이션 확인
        Log.d(TAG, "🎨 iOS 스타일 일정 상세보기 다이얼로그 시작");
        Log.d(TAG, "📋 전달받은 데이터 - 제목: " + title + ", 날짜: " + date + ", 시간: " + time);

        try {
            // 커스텀 다이얼로그 뷰 생성 (더 상세한 오류 추적)
            View dialogView = null;
            try {
                Log.d(TAG, "🔧 레이아웃 인플레이터 가져오기 시작");
                LayoutInflater inflater = getLayoutInflater();
                if (inflater == null) {
                    throw new Exception("LayoutInflater가 null입니다");
                }

                Log.d(TAG, "🔧 레이아웃 파일 인플레이트 시작: dialog_schedule_detail_ios");
                dialogView = inflater.inflate(R.layout.dialog_schedule_detail_ios, null);

                if (dialogView == null) {
                    throw new Exception("인플레이트된 뷰가 null입니다");
                }

                Log.d(TAG, "✅ 다이얼로그 레이아웃 인플레이트 성공");

            } catch (android.view.InflateException inflateException) {
                Log.e(TAG, "❌ InflateException 발생", inflateException);
                throw new Exception("레이아웃 인플레이트 오류 (InflateException): " + inflateException.getMessage());
            } catch (Exception inflateException) {
                Log.e(TAG, "❌ 일반 인플레이트 오류", inflateException);
                throw new Exception("레이아웃 인플레이트 오류: " + inflateException.getMessage());
            }

            // 뷰 요소들 찾기 (안전한 접근)
            TextView textScheduleTitle = dialogView.findViewById(R.id.textScheduleTitle);
            TextView textScheduleDate = dialogView.findViewById(R.id.textScheduleDate);
            TextView textScheduleTime = dialogView.findViewById(R.id.textScheduleTime);
            TextView textDeparture = dialogView.findViewById(R.id.textDeparture);
            TextView textDestination = dialogView.findViewById(R.id.textDestination);
            TextView textMemo = dialogView.findViewById(R.id.textMemo);
            TextView textRouteInfo = dialogView.findViewById(R.id.textRouteInfo);
            TextView textFriends = dialogView.findViewById(R.id.textFriends);
            TextView textStatus = dialogView.findViewById(R.id.textStatus);

            View cardLocationInfo = dialogView.findViewById(R.id.cardLocationInfo);
            View cardMemo = dialogView.findViewById(R.id.cardMemo);
            View cardRouteInfo = dialogView.findViewById(R.id.cardRouteInfo);
            View cardFriends = dialogView.findViewById(R.id.cardFriends);

            Button btnEdit = dialogView.findViewById(R.id.btnEdit);
            Button btnDelete = dialogView.findViewById(R.id.btnDelete);
            Button btnClose = dialogView.findViewById(R.id.btnClose);

            // 필수 뷰 요소 확인
            if (textScheduleTitle == null) {
                Log.e(TAG, "❌ textScheduleTitle을 찾을 수 없음");
                throw new Exception("textScheduleTitle 누락");
            }
            if (btnEdit == null) {
                Log.e(TAG, "❌ btnEdit을 찾을 수 없음");
                throw new Exception("btnEdit 누락");
            }
            if (btnDelete == null) {
                Log.e(TAG, "❌ btnDelete을 찾을 수 없음");
                throw new Exception("btnDelete 누락");
            }

            Log.d(TAG, "✅ 모든 필수 뷰 요소 찾기 완료");

            // 데이터 설정
            textScheduleTitle.setText(title);
            textScheduleDate.setText(date);
            textScheduleTime.setText(time);
            textDeparture.setText(departure);
            textDestination.setText(destination);

            // 메모 표시/숨김
            if (memo != null && !memo.equals("없음") && !memo.trim().isEmpty()) {
                textMemo.setText(memo);
                cardMemo.setVisibility(View.VISIBLE);
            } else {
                cardMemo.setVisibility(View.GONE);
            }

            // 위치 정보 표시/숨김
            if ((departure != null && !departure.equals("없음")) ||
                (destination != null && !destination.equals("없음"))) {
                cardLocationInfo.setVisibility(View.VISIBLE);
            } else {
                cardLocationInfo.setVisibility(View.GONE);
            }

            // 경로 정보 표시/숨김
            if (routeInfoText != null && !routeInfoText.isEmpty()) {
                textRouteInfo.setText(routeInfoText);
                cardRouteInfo.setVisibility(View.VISIBLE);
            } else {
                cardRouteInfo.setVisibility(View.GONE);
            }

            // 친구 정보 표시/숨김 (아이콘 제거)
            if (friendsText != null && !friendsText.isEmpty() && textFriends != null && cardFriends != null) {
                // 아이콘 제거하고 깔끔하게 표시
                String cleanFriendsText = friendsText
                    .replace("👥 함께하는 친구:\n", "")
                    .replace("• ", "")
                    .replace("👤 ", "");
                textFriends.setText(cleanFriendsText);
                cardFriends.setVisibility(View.VISIBLE);
                Log.d(TAG, "✅ 친구 카드 표시");
            } else if (cardFriends != null) {
                cardFriends.setVisibility(View.GONE);
                Log.d(TAG, "✅ 친구 카드 숨김");
            }

            // 상태 설정 (아이콘 제거)
            String status = schedule.isCompleted ? "완료" : "진행중";
            if (textStatus != null) {
                textStatus.setText(status);
                Log.d(TAG, "✅ 상태 설정 완료: " + status);
            }

            // 다이얼로그 생성
            AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();

            // 버튼 리스너 설정
            btnEdit.setOnClickListener(v -> {
                Log.d(TAG, "✏️ 수정 버튼 클릭");
                dialog.dismiss();
                editSchedule(schedule);
            });

            btnDelete.setOnClickListener(v -> {
                Log.d(TAG, "🗑️ 삭제 버튼 클릭");
                dialog.dismiss();
                confirmDeleteSchedule(schedule);
            });

            // btnClose가 null일 수 있으므로 안전하게 처리
            if (btnClose != null) {
                btnClose.setOnClickListener(v -> {
                    Log.d(TAG, "❌ 닫기 버튼 클릭");
                    dialog.dismiss();
                });
            } else {
                Log.w(TAG, "⚠️ 닫기 버튼을 찾을 수 없음");
            }

            // 다이얼로그 표시
            dialog.show();

            // 다이얼로그 크기 조정
            if (dialog.getWindow() != null) {
                int screenWidth = getResources().getDisplayMetrics().widthPixels;
                dialog.getWindow().setLayout(
                    (int) (screenWidth * 0.9),
                    ViewGroup.LayoutParams.WRAP_CONTENT
                );
            }

        } catch (Exception e) {
            Log.e(TAG, "❌ iOS 스타일 다이얼로그 표시 오류", e);
            e.printStackTrace();

            // 폴백: 개선된 기본 다이얼로그 사용
            try {
                Log.d(TAG, "🔄 폴백 다이얼로그 표시 시작");

                StringBuilder message = new StringBuilder();
                message.append("날짜: ").append(date).append(" ").append(time).append("\n\n");

                if (!departure.equals("없음") || !destination.equals("없음")) {
                    message.append("위치 정보:\n");
                    if (!departure.equals("없음")) {
                        message.append("출발: ").append(departure).append("\n");
                    }
                    if (!destination.equals("없음")) {
                        message.append("도착: ").append(destination).append("\n");
                    }
                    message.append("\n");
                }

                if (!memo.equals("없음") && !memo.trim().isEmpty()) {
                    message.append("메모: ").append(memo).append("\n\n");
                }

                if (routeInfoText != null && !routeInfoText.isEmpty()) {
                    message.append("경로 정보:\n").append(routeInfoText).append("\n\n");
                }

                if (friendsText != null && !friendsText.isEmpty()) {
                    message.append("함께하는 친구:\n").append(friendsText).append("\n\n");
                }

                String status = schedule.isCompleted ? "완료" : "진행중";
                message.append("상태: ").append(status);

                new AlertDialog.Builder(this)
                    .setTitle(title)
                    .setMessage(message.toString().trim())
                    .setPositiveButton("수정", (dialog, which) -> {
                        Log.d(TAG, "폴백 다이얼로그에서 수정 버튼 클릭");
                        editSchedule(schedule);
                    })
                    .setNegativeButton("삭제", (dialog, which) -> {
                        Log.d(TAG, "폴백 다이얼로그에서 삭제 버튼 클릭");
                        confirmDeleteSchedule(schedule);
                    })
                    .setNeutralButton("닫기", null)
                    .setCancelable(true)
                    .show();

                Log.d(TAG, "✅ 폴백 다이얼로그 표시 완료");

            } catch (Exception fallbackException) {
                Log.e(TAG, "❌ 폴백 다이얼로그도 실패", fallbackException);
                Toast.makeText(this, "일정 상세보기를 표시할 수 없습니다: " + title, Toast.LENGTH_LONG).show();
            }
        }
    }

    private void confirmDeleteSchedule(Schedule schedule) {
        new AlertDialog.Builder(this)
            .setTitle("일정 삭제")
            .setMessage("정말로 이 일정을 삭제하시겠습니까?")
            .setPositiveButton("삭제", (dialog, which) -> deleteSchedule(schedule))
            .setNegativeButton("취소", null)
            .show();
    }

    private void deleteSchedule(Schedule schedule) {
        executor.execute(() -> {
            try {
                int deleted = database.scheduleDao().delete(schedule);
                if (deleted > 0) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "일정이 삭제되었습니다", Toast.LENGTH_SHORT).show();
                        loadSchedules(); // 목록 새로고침
                    });
                }
            } catch (Exception e) {
                runOnUiThread(() ->
                    Toast.makeText(this, "일정 삭제에 실패했습니다", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void toggleScheduleCompletion(Schedule schedule) {
        executor.execute(() -> {
            try {
                boolean newStatus = !schedule.isCompleted;
                int updated = database.scheduleDao().updateScheduleCompletion(schedule.id, newStatus);
                
                if (updated > 0) {
                    schedule.isCompleted = newStatus;
                    runOnUiThread(() -> {
                        scheduleAdapter.notifyDataSetChanged();
                        String message = newStatus ? "일정을 완료했습니다" : "일정을 미완료로 변경했습니다";
                        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                    });
                }
            } catch (Exception e) {
                runOnUiThread(() -> 
                    Toast.makeText(this, "일정 상태 변경에 실패했습니다", Toast.LENGTH_SHORT).show());
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "🔄 onResume - 화면 복귀, 일정 새로고침 시작");

        // 강제로 일정 새로고침
        loadSchedules();

        // 일정 탭 선택 상태 유지
        bottomNavigation.setSelectedItemId(R.id.nav_schedule);

        Log.d(TAG, "✅ onResume 완료");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "🔄 onPause - 백그라운드 이동");

        try {
            // 진행 중인 데이터베이스 작업이 있다면 완료될 때까지 대기하지 않음
            // 새로운 작업은 시작하지 않음
            Log.d(TAG, "✅ onPause 완료");
        } catch (Exception e) {
            Log.e(TAG, "❌ onPause 중 오류", e);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "🛑 onStop - 리소스 일시 정리");

        try {
            // 메모리 정리 힌트
            System.gc();
            Log.d(TAG, "✅ onStop 메모리 정리 완료");
        } catch (Exception e) {
            Log.e(TAG, "❌ onStop 중 오류", e);
        }
    }

    /**
     * Empty State 표시
     */
    private void showEmptyState() {
        if (layoutEmptyState != null) {
            layoutEmptyState.setVisibility(View.VISIBLE);
        }
        if (recyclerSchedules != null) {
            recyclerSchedules.setVisibility(View.GONE);
        }
    }

    /**
     * Empty State 숨김
     */
    private void hideEmptyState() {
        if (layoutEmptyState != null) {
            layoutEmptyState.setVisibility(View.GONE);
        }
        if (recyclerSchedules != null) {
            recyclerSchedules.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Log.d(TAG, "📱 onActivityResult - requestCode: " + requestCode + ", resultCode: " + resultCode);

        if (requestCode == REQUEST_ADD_SCHEDULE && resultCode == RESULT_OK) {
            Log.d(TAG, "✅ 일정 추가/수정 완료, 강력한 새로고침 시작");

            // 즉시 새로고침
            loadSchedules();

            // 1초 후 추가 새로고침 (데이터베이스 저장 완료 확실히 대기)
            new android.os.Handler().postDelayed(() -> {
                Log.d(TAG, "🔄 1차 딜레이 후 일정 새로고침 실행");
                loadSchedules();
                verifyDatabaseTables(); // 데이터베이스 상태 확인
            }, 1000);

            // 2초 후 최종 새로고침
            new android.os.Handler().postDelayed(() -> {
                Log.d(TAG, "🔄 2차 딜레이 후 최종 일정 새로고침 실행");
                loadSchedules();
            }, 2000);

            Toast.makeText(this, "일정이 저장되었습니다. 목록을 새로고침합니다.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 캘린더에 일정 날짜 업데이트
     */
    private void updateCalendarSchedules() {
        try {
            scheduleDates.clear();

            for (Schedule schedule : scheduleList) {
                if (schedule.date != null && !schedule.date.trim().isEmpty()) {
                    scheduleDates.add(schedule.date);
                }
            }

            if (calendarView != null) {
                calendarView.setScheduleDates(scheduleDates);
            }

            Log.d(TAG, "캘린더 일정 날짜 업데이트: " + scheduleDates.size() + "개 날짜");

        } catch (Exception e) {
            Log.e(TAG, "캘린더 일정 업데이트 오류", e);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        Log.d(TAG, "🧹 ScheduleListActivity 리소스 정리 시작");

        try {
            // ExecutorService 안전하게 종료
            if (executor != null && !executor.isShutdown()) {
                Log.d(TAG, "🔄 ExecutorService 종료 중...");
                executor.shutdown();

                // 5초 대기 후 강제 종료
                try {
                    if (!executor.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                        Log.w(TAG, "⚠️ ExecutorService 정상 종료 실패, 강제 종료 실행");
                        executor.shutdownNow();
                    }
                    Log.d(TAG, "✅ ExecutorService 종료 완료");
                } catch (InterruptedException e) {
                    Log.w(TAG, "⚠️ ExecutorService 종료 대기 중 인터럽트 발생");
                    executor.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }

            // 어댑터 정리
            if (scheduleAdapter != null) {
                scheduleAdapter = null;
                Log.d(TAG, "✅ ScheduleAdapter 정리 완료");
            }

            // 리스트 정리
            if (scheduleList != null) {
                scheduleList.clear();
                scheduleList = null;
                Log.d(TAG, "✅ ScheduleList 정리 완료");
            }

            // 데이터베이스 참조 정리
            database = null;
            userSession = null;

            Log.d(TAG, "✅ ScheduleListActivity 리소스 정리 완료");

        } catch (Exception e) {
            Log.e(TAG, "❌ 리소스 정리 중 오류", e);
        }
    }

    /**
     * 디버깅 정보를 화면에 표시
     */
    private void showDebugInfo(String currentUserId, List<Schedule> userSchedules, List<Schedule> allSchedules) {
        try {
            // 먼저 데이터베이스의 모든 사용자 확인
            executor.execute(() -> {
                try {
                    List<com.example.timemate.data.model.User> allUsers = database.userDao().getAllUsers();

                    runOnUiThread(() -> {
                        StringBuilder debugInfo = new StringBuilder();
                        debugInfo.append("🔍 디버깅 정보\n");
                        debugInfo.append("현재 사용자: ").append(currentUserId).append("\n");
                        debugInfo.append("내 일정 수: ").append(userSchedules != null ? userSchedules.size() : 0).append("\n");
                        debugInfo.append("전체 일정 수: ").append(allSchedules != null ? allSchedules.size() : 0).append("\n");
                        debugInfo.append("등록된 사용자 수: ").append(allUsers != null ? allUsers.size() : 0).append("\n\n");

                        if (allUsers != null && !allUsers.isEmpty()) {
                            debugInfo.append("등록된 사용자들:\n");
                            for (com.example.timemate.data.model.User user : allUsers) {
                                debugInfo.append("- ").append(user.nickname).append(" (").append(user.userId).append(")\n");
                            }
                            debugInfo.append("\n");
                        }

                        if (allSchedules != null && !allSchedules.isEmpty()) {
                            debugInfo.append("전체 일정 목록:\n");
                            for (Schedule s : allSchedules) {
                                debugInfo.append("- ").append(s.title).append(" (").append(s.userId).append(")\n");
                            }
                        }

                        // Toast로 표시 (길면 여러 번 나눠서)
                        String debugText = debugInfo.toString();
                        if (debugText.length() > 300) {
                            // 첫 번째 부분만 표시
                            String firstPart = debugText.substring(0, Math.min(300, debugText.length()));
                            Toast.makeText(this, firstPart + "...", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(this, debugText, Toast.LENGTH_LONG).show();
                        }
                    });

                } catch (Exception e) {
                    Log.e(TAG, "사용자 정보 조회 오류", e);
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "디버깅 정보 표시 오류", e);
        }
    }

    /**
     * 테스트 사용자들 생성 (디버깅용)
     */
    private void createTestUsers() {
        executor.execute(() -> {
            try {
                // 테스트 사용자 1
                com.example.timemate.data.model.User user1 = database.userDao().getUserById("test_user_1");
                if (user1 == null) {
                    user1 = new com.example.timemate.data.model.User();
                    user1.userId = "test_user_1";
                    user1.nickname = "테스트사용자1";
                    user1.email = "test1@example.com";
                    user1.password = "password";
                    database.userDao().insert(user1);
                    Log.d(TAG, "테스트 사용자 1 생성 완료");
                }

                // 테스트 사용자 2
                com.example.timemate.data.model.User user2 = database.userDao().getUserById("test_user_2");
                if (user2 == null) {
                    user2 = new com.example.timemate.data.model.User();
                    user2.userId = "test_user_2";
                    user2.nickname = "테스트사용자2";
                    user2.email = "test2@example.com";
                    user2.password = "password";
                    database.userDao().insert(user2);
                    Log.d(TAG, "테스트 사용자 2 생성 완료");
                }

                // 테스트 사용자 3
                com.example.timemate.data.model.User user3 = database.userDao().getUserById("test_user_3");
                if (user3 == null) {
                    user3 = new com.example.timemate.data.model.User();
                    user3.userId = "test_user_3";
                    user3.nickname = "테스트사용자3";
                    user3.email = "test3@example.com";
                    user3.password = "password";
                    database.userDao().insert(user3);
                    Log.d(TAG, "테스트 사용자 3 생성 완료");
                }

                runOnUiThread(() -> {
                    Toast.makeText(this, "테스트 사용자들이 생성되었습니다\ntest_user_1, test_user_2, test_user_3\n비밀번호: password", Toast.LENGTH_LONG).show();
                });

            } catch (Exception e) {
                Log.e(TAG, "테스트 사용자 생성 오류", e);
            }
        });
    }
}
