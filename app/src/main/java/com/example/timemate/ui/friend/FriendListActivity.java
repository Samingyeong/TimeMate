package com.example.timemate.ui.friend;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
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
            showAddFriendDialog();
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

    private void showAddFriendDialog() {
        // 사용자 ID 입력 다이얼로그
        EditText editUserId = new EditText(this);
        editUserId.setHint("친구의 사용자 ID를 입력하세요");
        editUserId.setPadding(50, 30, 50, 30);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("친구 추가")
                .setMessage("추가할 친구의 사용자 ID를 입력해주세요")
                .setView(editUserId)
                .setPositiveButton("추가", (dialog, which) -> {
                    String friendUserId = editUserId.getText().toString().trim();

                    if (friendUserId.isEmpty()) {
                        Toast.makeText(this, "사용자 ID를 입력해주세요", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (friendUserId.equals(userSession.getCurrentUserId())) {
                        Toast.makeText(this, "자신을 친구로 추가할 수 없습니다", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    addFriendByUserId(friendUserId);
                })
                .setNegativeButton("취소", null)
                .show();

        editUserId.requestFocus();
    }

    private void addFriendByUserId(String friendUserId) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                // 1. 해당 사용자 ID가 존재하는지 확인
                com.example.timemate.data.model.User targetUser = db.userDao().getUserById(friendUserId);

                if (targetUser == null || !targetUser.isActive) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "존재하지 않는 사용자 ID입니다", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                // 2. 이미 친구인지 확인
                String currentUserId = userSession.getCurrentUserId();
                Friend existingFriend = db.friendDao().getFriend(currentUserId, friendUserId);

                if (existingFriend != null) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "이미 친구로 등록된 사용자입니다", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                // 3. 친구 관계 생성 (양방향)
                Friend friend1 = new Friend();
                friend1.userId = currentUserId;
                friend1.friendUserId = friendUserId;
                friend1.friendNickname = targetUser.nickname;
                friend1.status = "accepted"; // 바로 수락된 상태로
                friend1.createdAt = System.currentTimeMillis();

                Friend friend2 = new Friend();
                friend2.userId = friendUserId;
                friend2.friendUserId = currentUserId;
                friend2.friendNickname = userSession.getCurrentNickname();
                friend2.status = "accepted";
                friend2.createdAt = System.currentTimeMillis();

                db.friendDao().insert(friend1);
                db.friendDao().insert(friend2);

                runOnUiThread(() -> {
                    Toast.makeText(this, targetUser.nickname + "님이 친구로 추가되었습니다", Toast.LENGTH_SHORT).show();
                    loadFriends(); // 친구 목록 새로고침
                });

            } catch (Exception e) {
                Log.e("FriendList", "친구 추가 오류", e);
                runOnUiThread(() -> {
                    Toast.makeText(this, "친구 추가 중 오류가 발생했습니다", Toast.LENGTH_SHORT).show();
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
