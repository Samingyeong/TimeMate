package com.example.timemate;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.RoomDatabase;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;
import java.util.concurrent.Executors;

public class ScheduleListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ScheduleAdapter adapter;
    private AppDatabase db;
    private UserSession userSession;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule_list);

        userSession = new UserSession(this);

        // 로그인 상태 확인
        if (!userSession.isLoggedIn()) {
            finish();
            return;
        }

        recyclerView = findViewById(R.id.recyclerSchedule);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        FloatingActionButton fabAdd = findViewById(R.id.fabAddSchedule);
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigationView);

        // 현재 메뉴 선택 표시 (일정관리 → 맛집추천으로 변경됨)
        bottomNav.setSelectedItemId(R.id.menu_recommendation);

        db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "timeMate-db")
                .fallbackToDestructiveMigration()
                .setJournalMode(RoomDatabase.JournalMode.AUTOMATIC)
                .setQueryExecutor(Executors.newSingleThreadExecutor())
                .build();

        loadSchedules();

        // FAB 클릭 처리
        fabAdd.setOnClickListener(v -> {
            startActivity(new Intent(ScheduleListActivity.this, ScheduleAddActivity.class));
        });

        // 바텀 네비게이션 클릭 처리
        bottomNav.setOnItemSelectedListener(new BottomNavigationView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();
                if (id == R.id.menu_recommendation) {
                    return true; // 현재 화면 (임시)
                } else if (id == R.id.menu_home) {
                    startActivity(new Intent(ScheduleListActivity.this, HomeActivity.class));
                    finish();
                    return true;
                } else if (id == R.id.menu_friends) {
                    startActivity(new Intent(ScheduleListActivity.this, FriendListActivity.class));
                    finish();
                    return true;
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
        if (currentUserId == null) return;

        Executors.newSingleThreadExecutor().execute(() -> {
            List<Schedule> schedules = db.scheduleDao().getSchedulesByUserId(currentUserId);
            runOnUiThread(() -> {
                adapter = new ScheduleAdapter(schedules);
                recyclerView.setAdapter(adapter);
            });
        });
    }
}