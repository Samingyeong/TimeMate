package com.example.timemate;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.room.Room;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class FriendListActivity extends AppCompatActivity {

    private AppDatabase db;
    private ListView listView;
    private ArrayAdapter<String> adapter;
    private List<String> friendNames = new ArrayList<>();
    private UserSession userSession;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friend_list);

        userSession = new UserSession(this);

        // 로그인 상태 확인
        if (!userSession.isLoggedIn()) {
            finish();
            return;
        }

        listView = findViewById(R.id.friendListView);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, friendNames);
        listView.setAdapter(adapter);

        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigationView);

        // 친구 추가 버튼 (FAB 대신 헤더에 추가)
        findViewById(R.id.btnAddFriend).setOnClickListener(v -> {
            Intent intent = new Intent(this, FriendAddActivity.class);
            startActivity(intent);
        });

        // 현재 메뉴 선택 표시
        bottomNav.setSelectedItemId(R.id.menu_friends);

        db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "timeMate-db")
                .fallbackToDestructiveMigration()
                .build();

        loadFriends();

        // 바텀 네비게이션 클릭 처리
        bottomNav.setOnItemSelectedListener(new BottomNavigationView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();
                if (id == R.id.menu_recommendation) {
                    startActivity(new Intent(FriendListActivity.this, com.example.timemate.ui.recommendation.RecommendationActivity.class));
                    finish();
                    return true;
                } else if (id == R.id.menu_home) {
                    startActivity(new Intent(FriendListActivity.this, HomeActivity.class));
                    finish();
                    return true;
                } else if (id == R.id.menu_friends) {
                    return true; // 현재 화면이므로 아무것도 하지 않음
                } else if (id == R.id.menu_profile) {
                    startActivity(new Intent(FriendListActivity.this, ProfileActivity.class));
                    finish();
                    return true;
                }
                return false;
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadFriends(); // 화면이 다시 보일 때마다 친구 목록 새로고침
    }

    private void loadFriends() {
        String currentUserId = userSession.getCurrentUserId();
        if (currentUserId == null) return;

        Executors.newSingleThreadExecutor().execute(() -> {
            List<Friend> friends = db.friendDao().getFriendsByUserId(currentUserId);
            friendNames.clear();
            for (Friend f : friends) {
                friendNames.add(f.friendNickname + " (" + f.friendId + ")");
            }
            runOnUiThread(() -> adapter.notifyDataSetChanged());
        });
    }
}
