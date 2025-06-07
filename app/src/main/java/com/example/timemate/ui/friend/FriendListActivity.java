package com.example.timemate.ui.friend;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.timemate.R;
import com.example.timemate.data.database.AppDatabase;
import com.example.timemate.data.model.Friend;
import com.example.timemate.ui.home.HomeActivity;
import com.example.timemate.ui.recommendation.RecommendationActivity;
import com.example.timemate.ui.profile.ProfileActivity;
import com.example.timemate.util.UserSession;
import com.example.timemate.FriendAddActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

/**
 * 친구 목록 화면
 * - 사용자의 친구 목록 표시
 * - 친구 추가 기능
 * - 친구 관리 기능
 */
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
        loadFriends();
    }

    private void initViews() {
        listView = findViewById(R.id.friendListView);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, friendNames);
        listView.setAdapter(adapter);
    }

    private void setupDatabase() {
        db = AppDatabase.getDatabase(this);
    }

    private void setupClickListeners() {
        // 친구 추가 버튼
        findViewById(R.id.btnAddFriend).setOnClickListener(v -> {
            Intent intent = new Intent(this, FriendAddActivity.class);
            startActivity(intent);
        });
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigationView);
        bottomNav.setSelectedItemId(R.id.menu_friends);

        bottomNav.setOnItemSelectedListener(new BottomNavigationView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();
                if (id == R.id.menu_home) {
                    startActivity(new Intent(FriendListActivity.this, HomeActivity.class));
                    finish();
                    return true;
                } else if (id == R.id.menu_friends) {
                    return true; // 현재 화면
                } else if (id == R.id.menu_recommendation) {
                    startActivity(new Intent(FriendListActivity.this, RecommendationActivity.class));
                    finish();
                    return true;
                } else if (id == R.id.menu_profile) {
                    startActivity(new Intent(FriendListActivity.this, ProfileActivity.class));
                    finish();
                    return true;
                }
                return false;
            }
        });
    }

    private void loadFriends() {
        String currentUserId = userSession.getCurrentUserId();
        if (currentUserId == null) {
            return;
        }

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                List<Friend> friends = db.friendDao().getFriendsByUserId(currentUserId);
                friendNames.clear();
                
                for (Friend friend : friends) {
                    friendNames.add(friend.friendNickname + " (" + friend.friendUserId + ")");
                }
                
                runOnUiThread(() -> adapter.notifyDataSetChanged());
            } catch (Exception e) {
                runOnUiThread(() -> {
                    // 에러 처리
                    friendNames.clear();
                    friendNames.add("친구 목록을 불러올 수 없습니다");
                    adapter.notifyDataSetChanged();
                });
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 화면이 다시 보일 때마다 친구 목록 새로고침
        loadFriends();
    }
}
