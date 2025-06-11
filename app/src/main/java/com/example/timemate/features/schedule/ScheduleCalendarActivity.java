package com.example.timemate.features.schedule;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
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
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import android.widget.CalendarView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 달력 기반 일정 화면
 */
public class ScheduleCalendarActivity extends AppCompatActivity {

    private static final String TAG = "ScheduleCalendarActivity";
    
    private AppDatabase database;
    private UserSession userSession;
    private ExecutorService executor;
    
    private CalendarView calendarView;
    private RecyclerView recyclerSchedules;
    private ScheduleListAdapter scheduleAdapter;
    private BottomNavigationView bottomNavigation;
    private FloatingActionButton fabAddSchedule;

    private List<Schedule> allSchedules = new ArrayList<>();
    private List<Schedule> selectedDateSchedules = new ArrayList<>();
    private String selectedDateString = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule_calendar);

        initViews();
        initServices();
        setupCalendar();
        setupRecyclerView();
        setupBottomNavigation();
        setupClickListeners();
        
        // 로그인 상태 확인
        if (!userSession.isLoggedIn()) {
            finish();
            return;
        }
        
        loadSchedules();
    }

    private void initViews() {
        calendarView = findViewById(R.id.calendarView);
        recyclerSchedules = findViewById(R.id.recyclerSchedules);
        bottomNavigation = findViewById(R.id.bottomNavigationView);
        fabAddSchedule = findViewById(R.id.fabAddSchedule);
    }

    private void initServices() {
        database = AppDatabase.getDatabase(this);
        userSession = UserSession.getInstance(this);
        executor = Executors.newSingleThreadExecutor();
    }

    private void setupCalendar() {
        // 달력 날짜 선택 리스너
        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            // 선택된 날짜 문자열 생성 (yyyy-MM-dd 형식)
            selectedDateString = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth);
            Log.d(TAG, "날짜 선택됨: " + selectedDateString);
            showSchedulesForSelectedDate();
        });

        // 오늘 날짜로 초기화
        Calendar today = Calendar.getInstance();
        selectedDateString = String.format(Locale.getDefault(), "%04d-%02d-%02d",
                                         today.get(Calendar.YEAR),
                                         today.get(Calendar.MONTH) + 1,
                                         today.get(Calendar.DAY_OF_MONTH));
        showSchedulesForSelectedDate();
    }

    private void setupRecyclerView() {
        scheduleAdapter = new ScheduleListAdapter(selectedDateSchedules, new ScheduleListAdapter.OnScheduleClickListener() {
            @Override
            public void onScheduleClick(Schedule schedule) {
                openScheduleDetail(schedule);
            }

            @Override
            public void onEditClick(Schedule schedule) {
                editSchedule(schedule);
            }

            @Override
            public void onDeleteClick(Schedule schedule) {
                deleteSchedule(schedule);
            }

            @Override
            public void onCompleteToggle(Schedule schedule) {
                toggleScheduleCompletion(schedule);
            }
        });
        
        recyclerSchedules.setLayoutManager(new LinearLayoutManager(this));
        recyclerSchedules.setAdapter(scheduleAdapter);
    }

    private void setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener(new BottomNavigationView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int itemId = item.getItemId();
                
                if (itemId == R.id.nav_home) {
                    startActivity(new Intent(ScheduleCalendarActivity.this, HomeActivity.class));
                    return true;
                } else if (itemId == R.id.nav_schedule) {
                    // 이미 일정 화면
                    return true;
                } else if (itemId == R.id.nav_friends) {
                    startActivity(new Intent(ScheduleCalendarActivity.this, FriendListActivity.class));
                    return true;
                } else if (itemId == R.id.nav_recommendation) {
                    startActivity(new Intent(ScheduleCalendarActivity.this, RecommendationActivity.class));
                    return true;
                } else if (itemId == R.id.nav_profile) {
                    startActivity(new Intent(ScheduleCalendarActivity.this, ProfileActivity.class));
                    return true;
                }
                return false;
            }
        });

        // 일정 탭 선택 상태로 설정
        bottomNavigation.setSelectedItemId(R.id.nav_schedule);
    }

    private void setupClickListeners() {
        fabAddSchedule.setOnClickListener(v -> {
            Intent intent = new Intent(this, ScheduleAddActivity.class);
            startActivity(intent);
        });
    }

    /**
     * 모든 일정 로드 및 달력에 표시
     */
    private void loadSchedules() {
        executor.execute(() -> {
            try {
                String currentUserId = userSession.getCurrentUserId();
                if (currentUserId == null) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "로그인이 필요합니다", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                    return;
                }

                List<Schedule> schedules = database.scheduleDao().getSchedulesByUserId(currentUserId);
                
                runOnUiThread(() -> {
                    allSchedules.clear();
                    allSchedules.addAll(schedules);

                    // 선택된 날짜의 일정 표시
                    showSchedulesForSelectedDate();

                    Log.d(TAG, "일정 로드 완료: " + schedules.size() + "개");
                });

            } catch (Exception e) {
                Log.e(TAG, "일정 로드 오류", e);
                runOnUiThread(() -> 
                    Toast.makeText(this, "일정 목록을 불러오는데 실패했습니다", Toast.LENGTH_SHORT).show());
            }
        });
    }

    /**
     * 선택된 날짜의 일정들 표시
     */
    private void showSchedulesForSelectedDate() {
        selectedDateSchedules.clear();

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        for (Schedule schedule : allSchedules) {
            try {
                Date scheduleDate = schedule.getScheduledDate();
                if (scheduleDate != null) {
                    String scheduleDateStr = dateFormat.format(scheduleDate);
                    if (selectedDateString.equals(scheduleDateStr)) {
                        selectedDateSchedules.add(schedule);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "날짜 비교 오류", e);
            }
        }

        scheduleAdapter.notifyDataSetChanged();

        Log.d(TAG, "선택된 날짜 (" + selectedDateString + ")의 일정: " + selectedDateSchedules.size() + "개");
    }

    /**
     * 일정 상세보기
     */
    private void openScheduleDetail(Schedule schedule) {
        try {
            if (schedule == null) {
                Toast.makeText(this, "일정 정보를 불러올 수 없습니다", Toast.LENGTH_SHORT).show();
                return;
            }

            String title = schedule.title != null ? schedule.title : "제목 없음";
            String departure = schedule.departure != null ? schedule.departure : "없음";
            String destination = schedule.destination != null ? schedule.destination : "없음";
            String memo = schedule.memo != null && !schedule.memo.trim().isEmpty() ? schedule.memo : "없음";

            // 날짜 시간 포맷팅
            String dateTimeStr = "날짜 미정";
            if (schedule.getScheduledDate() != null) {
                SimpleDateFormat formatter = new SimpleDateFormat("yyyy년 MM월 dd일 HH:mm", Locale.KOREAN);
                dateTimeStr = formatter.format(schedule.getScheduledDate());
            }

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

            new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(String.format("일시: %s\n출발: %s\n도착: %s\n메모: %s%s%s%s",
                    dateTimeStr, departure, destination, memo, routeInfoText, transportModesText, friendsText))
                .setPositiveButton("수정", (dialog, which) -> editSchedule(schedule))
                .setNegativeButton("삭제", (dialog, which) -> confirmDeleteSchedule(schedule))
                .setNeutralButton("닫기", null)
                .show();
        } catch (Exception e) {
            Toast.makeText(this, "일정 상세보기 중 오류가 발생했습니다", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Error opening schedule detail", e);
        }
    }

    /**
     * 일정 수정
     */
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
     * 일정 삭제 확인
     */
    private void confirmDeleteSchedule(Schedule schedule) {
        new AlertDialog.Builder(this)
            .setTitle("일정 삭제")
            .setMessage("정말로 이 일정을 삭제하시겠습니까?")
            .setPositiveButton("삭제", (dialog, which) -> deleteSchedule(schedule))
            .setNegativeButton("취소", null)
            .show();
    }

    /**
     * 일정 삭제
     */
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

    /**
     * 일정 완료 상태 토글
     */
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }

}
