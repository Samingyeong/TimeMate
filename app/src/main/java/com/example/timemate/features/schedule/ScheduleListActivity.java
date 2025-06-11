package com.example.timemate.features.schedule;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
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
import com.example.timemate.core.util.UserSession;
import com.example.timemate.features.home.HomeActivity;
import com.example.timemate.features.friend.FriendListActivity;
import com.example.timemate.features.profile.ProfileActivity;
import com.example.timemate.features.schedule.adapter.ScheduleListAdapter;
import com.example.timemate.ui.recommendation.RecommendationActivity;
import com.example.timemate.ui.home.CalendarView;
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
            Log.d(TAG, "ScheduleListActivity onCreate 시작");
            setContentView(R.layout.activity_schedule_list);

            initViews();
            initServices();

            // UserSession 안전성 확인
            if (userSession == null) {
                Log.e(TAG, "UserSession 초기화 실패");
                Toast.makeText(this, "사용자 세션을 불러올 수 없습니다", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            setupRecyclerView();
            setupCalendar();
            setupBottomNavigation();
            setupClickListeners();

            // 로그인 상태 확인
            if (!userSession.isLoggedIn()) {
                Log.w(TAG, "사용자가 로그인되지 않음");
                Toast.makeText(this, "로그인이 필요합니다", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            // 데이터베이스 테이블 상태 확인
            verifyDatabaseTables();

            // 테스트 일정 생성 (개발용)
            createTestScheduleIfEmpty();

            loadSchedules();
            Log.d(TAG, "ScheduleListActivity onCreate 완료");

        } catch (Exception e) {
            Log.e(TAG, "ScheduleListActivity onCreate 오류", e);
            e.printStackTrace();
            Toast.makeText(this, "일정 화면을 불러오는 중 오류가 발생했습니다", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void initViews() {
        recyclerSchedules = findViewById(R.id.recyclerSchedules);
        bottomNavigation = findViewById(R.id.bottomNavigationView);
        fabAddSchedule = findViewById(R.id.fabAddSchedule);
        layoutEmptyState = findViewById(R.id.layoutEmptyState);

        // 캘린더 뷰들
        calendarView = findViewById(R.id.calendarView);
        textCurrentMonth = findViewById(R.id.textCurrentMonth);
        btnPrevMonth = findViewById(R.id.btnPrevMonth);
        btnNextMonth = findViewById(R.id.btnNextMonth);
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
        scheduleAdapter = new ScheduleListAdapter(scheduleList, new ScheduleListAdapter.OnScheduleClickListener() {
            @Override
            public void onScheduleClick(Schedule schedule) {
                // 일정 상세보기
                openScheduleDetail(schedule);
            }

            @Override
            public void onEditClick(Schedule schedule) {
                // 일정 수정
                editSchedule(schedule);
            }

            @Override
            public void onDeleteClick(Schedule schedule) {
                // 일정 삭제
                deleteSchedule(schedule);
            }

            @Override
            public void onCompleteToggle(Schedule schedule) {
                // 완료 상태 토글
                toggleScheduleCompletion(schedule);
            }
        });
        
        recyclerSchedules.setLayoutManager(new LinearLayoutManager(this));
        recyclerSchedules.setAdapter(scheduleAdapter);
    }

    /**
     * 캘린더 설정
     */
    private void setupCalendar() {
        try {
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

            // 이전/다음 월 버튼
            btnPrevMonth.setOnClickListener(v -> {
                calendarView.previousMonth();
                updateCurrentMonthDisplay();
                updateCalendarSchedules();
            });

            btnNextMonth.setOnClickListener(v -> {
                calendarView.nextMonth();
                updateCurrentMonthDisplay();
                updateCalendarSchedules();
            });

            Log.d(TAG, "캘린더 설정 완료");

        } catch (Exception e) {
            Log.e(TAG, "캘린더 설정 오류", e);
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
     * 특정 날짜의 일정 표시
     */
    private void showSchedulesForDate(Calendar date) {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            String selectedDateStr = dateFormat.format(date.getTime());

            // 해당 날짜의 일정 필터링
            List<Schedule> daySchedules = new ArrayList<>();
            for (Schedule schedule : scheduleList) {
                if (schedule.date != null && schedule.date.equals(selectedDateStr)) {
                    daySchedules.add(schedule);
                }
            }

            if (daySchedules.isEmpty()) {
                Toast.makeText(this, "선택한 날짜에 일정이 없습니다", Toast.LENGTH_SHORT).show();
            } else {
                showScheduleDetailDialog(daySchedules, selectedDateStr);
            }

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
                String currentUserId = userSession.getCurrentUserId();
                Log.d(TAG, "현재 사용자 ID: " + currentUserId);

                if (currentUserId == null) {
                    Log.e(TAG, "❌ 사용자 ID가 null입니다");
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
                List<Schedule> schedules = database.scheduleDao().getSchedulesByUserId(currentUserId);
                Log.d(TAG, "📊 조회된 내 일정 수: " + (schedules != null ? schedules.size() : "null"));

                // 공유된 일정도 추가 (수락된 것만)
                try {
                    List<com.example.timemate.data.model.SharedSchedule> sharedSchedules =
                        database.sharedScheduleDao().getSharedSchedulesByUserId(currentUserId);

                    int acceptedCount = 0;
                    for (com.example.timemate.data.model.SharedSchedule shared : sharedSchedules) {
                        if ("accepted".equals(shared.status)) {
                            try {
                                // 원본 일정 가져오기
                                Schedule originalSchedule = database.scheduleDao().getScheduleById(shared.originalScheduleId);
                                if (originalSchedule != null) {
                                    // 공유된 일정임을 표시하기 위해 제목에 표시 추가
                                    Schedule sharedScheduleCopy = new Schedule();
                                    sharedScheduleCopy.id = originalSchedule.id;
                                    sharedScheduleCopy.userId = originalSchedule.userId;
                                    sharedScheduleCopy.title = "👥 " + originalSchedule.title + " (with " + shared.creatorNickname + ")";
                                    sharedScheduleCopy.date = originalSchedule.date;
                                    sharedScheduleCopy.time = originalSchedule.time;
                                    sharedScheduleCopy.departure = originalSchedule.departure;
                                    sharedScheduleCopy.destination = originalSchedule.destination;
                                    sharedScheduleCopy.memo = originalSchedule.memo;
                                    sharedScheduleCopy.isCompleted = originalSchedule.isCompleted;
                                    sharedScheduleCopy.routeInfo = originalSchedule.routeInfo;
                                    sharedScheduleCopy.selectedTransportModes = originalSchedule.selectedTransportModes;

                                    schedules.add(sharedScheduleCopy);
                                    acceptedCount++;
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "공유 일정 처리 오류: " + shared.originalScheduleId, e);
                            }
                        }
                    }
                    Log.d(TAG, "📊 추가된 공유 일정 수: " + acceptedCount);
                } catch (Exception e) {
                    Log.e(TAG, "공유 일정 로드 오류", e);
                }

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

            // 경로 정보 추가
            String routeInfoText = "";
            if (schedule.routeInfo != null && !schedule.routeInfo.isEmpty()) {
                routeInfoText = "\n\n🗺️ 선택된 경로:\n" + parseRouteInfo(schedule.routeInfo);
            }

            String transportModesText = "";
            if (schedule.selectedTransportModes != null && !schedule.selectedTransportModes.isEmpty()) {
                transportModesText = "\n🚌 교통수단: " + schedule.selectedTransportModes;
            }

            // 함께하는 친구 정보 가져오기
            String friendsText = "";
            try {
                List<String> friendNames = getSharedFriends(schedule.id);
                if (!friendNames.isEmpty()) {
                    friendsText = "\n\n👥 함께하는 친구:\n";
                    for (String friendName : friendNames) {
                        friendsText += "• " + friendName + "\n";
                    }
                    friendsText = friendsText.trim();
                }
            } catch (Exception e) {
                Log.e(TAG, "친구 정보 로드 오류", e);
            }

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
                return "경로 정보 없음";
            }

            Log.d(TAG, "파싱할 경로 정보: " + routeInfoJson);

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

                            Log.d(TAG, "경로 파싱: " + mode + " - 시간: " + duration + ", 비용: " + cost);
                        }
                    }
                }
            }

            String finalResult = result.toString().trim();
            Log.d(TAG, "파싱된 경로 정보: " + finalResult);

            return finalResult.isEmpty() ? "저장된 경로 정보가 없습니다" : finalResult;

        } catch (Exception e) {
            Log.e(TAG, "경로 정보 파싱 오류", e);
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
            AppDatabase database = AppDatabase.getInstance(this);

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

            Log.d(TAG, "일정 " + scheduleId + "의 참여 친구 수: " + friendNames.size());

        } catch (Exception e) {
            Log.e(TAG, "공유 친구 정보 로드 오류", e);
        }
        return friendNames;
    }

    /**
     * iOS 스타일 일정 상세보기 다이얼로그
     */
    private void showIOSStyleScheduleDetail(Schedule schedule, String title, String date, String time,
                                          String departure, String destination, String memo,
                                          String routeInfoText, String friendsText) {
        try {
            // 커스텀 다이얼로그 뷰 생성
            View dialogView = getLayoutInflater().inflate(R.layout.dialog_schedule_detail_ios, null);

            // 뷰 요소들 찾기
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

            // 친구 정보 표시/숨김
            if (friendsText != null && !friendsText.isEmpty()) {
                textFriends.setText(friendsText.replace("👥 함께하는 친구:\n", "").replace("• ", "👤 "));
                cardFriends.setVisibility(View.VISIBLE);
            } else {
                cardFriends.setVisibility(View.GONE);
            }

            // 상태 설정
            String status = schedule.isCompleted ? "✅ 완료" : "⏳ 진행중";
            textStatus.setText(status);

            // 다이얼로그 생성
            AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();

            // 버튼 리스너 설정
            btnEdit.setOnClickListener(v -> {
                dialog.dismiss();
                editSchedule(schedule);
            });

            btnDelete.setOnClickListener(v -> {
                dialog.dismiss();
                confirmDeleteSchedule(schedule);
            });

            btnClose.setOnClickListener(v -> dialog.dismiss());

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
            Log.e(TAG, "iOS 스타일 다이얼로그 표시 오류", e);
            // 폴백: 기본 다이얼로그 사용
            new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(String.format("날짜: %s %s\n출발: %s\n도착: %s\n메모: %s%s%s",
                    date, time, departure, destination, memo, routeInfoText, friendsText))
                .setPositiveButton("수정", (dialog, which) -> editSchedule(schedule))
                .setNegativeButton("삭제", (dialog, which) -> confirmDeleteSchedule(schedule))
                .setNeutralButton("닫기", null)
                .show();
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
        loadSchedules(); // 화면 복귀 시 새로고침
        
        // 일정 탭 선택 상태 유지
        bottomNavigation.setSelectedItemId(R.id.nav_schedule);
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

        if (requestCode == REQUEST_ADD_SCHEDULE && resultCode == RESULT_OK) {
            Log.d(TAG, "✅ 일정 추가 완료, 목록 새로고침");
            // 일정 추가 완료 후 목록 새로고침
            loadSchedules();
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
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
}
