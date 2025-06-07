package com.example.timemate.ui.schedule;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.timemate.R;
import com.example.timemate.data.database.AppDatabase;
import com.example.timemate.data.model.Schedule;
import com.example.timemate.ui.home.HomeActivity;
import com.example.timemate.ui.friend.FriendListActivity;
import com.example.timemate.ui.profile.ProfileActivity;
import com.example.timemate.util.UserSession;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;
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
        recyclerView = findViewById(R.id.recyclerSchedule);
        textEmptySchedule = findViewById(R.id.textEmptySchedule);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    private void setupDatabase() {
        db = AppDatabase.getDatabase(this);
    }

    private void setupClickListeners() {
        FloatingActionButton fabAdd = findViewById(R.id.fabAddSchedule);
        fabAdd.setOnClickListener(v -> {
            startActivity(new Intent(ScheduleListActivity.this, ScheduleAddActivity.class));
        });
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigationView);
        bottomNav.setSelectedItemId(R.id.menu_recommendation); // 일정관리 → 맛집추천으로 변경됨

        bottomNav.setOnItemSelectedListener(new BottomNavigationView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();
                if (id == R.id.menu_home) {
                    startActivity(new Intent(ScheduleListActivity.this, HomeActivity.class));
                    finish();
                    return true;
                } else if (id == R.id.menu_friends) {
                    startActivity(new Intent(ScheduleListActivity.this, FriendListActivity.class));
                    finish();
                    return true;
                } else if (id == R.id.menu_recommendation) {
                    return true; // 현재 화면 (임시)
                } else if (id == R.id.menu_profile) {
                    startActivity(new Intent(ScheduleListActivity.this, ProfileActivity.class));
                    finish();
                    return true;
                }
                return false;
            }
        });
    }

    private void loadSchedules() {
        String currentUserId = userSession.getCurrentUserId();
        if (currentUserId == null) {
            showEmptyState();
            return;
        }

        // 먼저 빈 어댑터 설정
        setupEmptyAdapter();

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                List<Schedule> schedules = db.scheduleDao().getSchedulesByUserId(currentUserId);
                
                runOnUiThread(() -> {
                    if (schedules.isEmpty()) {
                        showEmptyState();
                    } else {
                        showSchedules(schedules);
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    showEmptyState();
                });
            }
        });
    }

    private void setupEmptyAdapter() {
        List<Schedule> emptyList = new ArrayList<>();
        adapter = new ImprovedScheduleAdapter(emptyList, this);
        recyclerView.setAdapter(adapter);
    }

    private void showSchedules(List<Schedule> schedules) {
        adapter = new ImprovedScheduleAdapter(schedules, this);
        recyclerView.setAdapter(adapter);
        recyclerView.setVisibility(View.VISIBLE);
        textEmptySchedule.setVisibility(View.GONE);
    }

    private void showEmptyState() {
        recyclerView.setVisibility(View.GONE);
        if (textEmptySchedule != null) {
            textEmptySchedule.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 화면이 다시 보일 때마다 일정 목록 새로고침
        loadSchedules();
    }
}
