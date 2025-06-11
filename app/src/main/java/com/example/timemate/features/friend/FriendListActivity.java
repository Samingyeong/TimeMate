package com.example.timemate.features.friend;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.timemate.R;
import com.example.timemate.data.database.AppDatabase;
import com.example.timemate.data.model.Friend;
import com.example.timemate.core.util.UserSession;
import com.example.timemate.features.home.HomeActivity;
import com.example.timemate.features.schedule.ScheduleListActivity;
import com.example.timemate.features.profile.ProfileActivity;
import com.example.timemate.features.friend.adapter.FriendListAdapter;
import com.example.timemate.ui.recommendation.RecommendationActivity;
import com.example.timemate.utils.NavigationHelper;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 친구 목록 화면
 */
public class FriendListActivity extends AppCompatActivity {

    private static final String TAG = "FriendListActivity";
    
    private AppDatabase database;
    private UserSession userSession;
    private ExecutorService executor;
    
    private RecyclerView recyclerFriends;
    private FriendListAdapter friendAdapter;
    private BottomNavigationView bottomNavigation;
    private FloatingActionButton fabAddFriend;
    
    private List<Friend> friendList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friend_list);

        initViews();
        initServices();
        setupRecyclerView();
        setupBottomNavigation();
        setupClickListeners();
        
        // 로그인 상태 확인
        if (!userSession.isLoggedIn()) {
            finish();
            return;
        }
        
        loadFriends();
    }

    private void initViews() {
        recyclerFriends = findViewById(R.id.recyclerFriends);
        bottomNavigation = findViewById(R.id.bottomNavigationView);
        fabAddFriend = findViewById(R.id.fabAddFriend);
    }

    private void initServices() {
        database = AppDatabase.getDatabase(this);
        userSession = UserSession.getInstance(this);
        executor = Executors.newSingleThreadExecutor();
    }

    private void setupRecyclerView() {
        friendAdapter = new FriendListAdapter(friendList, new FriendListAdapter.OnFriendClickListener() {
            @Override
            public void onFriendClick(Friend friend) {
                // 친구 삭제 확인 다이얼로그 표시
                showDeleteFriendDialog(friend);
            }

            @Override
            public void onAcceptClick(Friend friend) {
                acceptFriendRequest(friend);
            }

            @Override
            public void onRejectClick(Friend friend) {
                rejectFriendRequest(friend);
            }

            @Override
            public void onDeleteClick(Friend friend) {
                deleteFriend(friend);
            }
        });
        
        recyclerFriends.setLayoutManager(new LinearLayoutManager(this));
        recyclerFriends.setAdapter(friendAdapter);
    }

    private void setupBottomNavigation() {
        try {
            android.util.Log.d(TAG, "공통 네비게이션 헬퍼 사용");
            NavigationHelper.setupBottomNavigation(this, R.id.nav_friends);
        } catch (Exception e) {
            android.util.Log.e(TAG, "바텀 네비게이션 설정 오류", e);
        }
    }

    private void setupClickListeners() {
        fabAddFriend.setOnClickListener(v -> {
            Intent intent = new Intent(this, FriendAddActivity.class);
            startActivity(intent);
        });
    }

    private void loadFriends() {
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

                List<Friend> friends = database.friendDao().getFriendsByUserId(currentUserId);
                
                runOnUiThread(() -> {
                    friendList.clear();
                    friendList.addAll(friends);
                    friendAdapter.notifyDataSetChanged();
                });

            } catch (Exception e) {
                runOnUiThread(() -> 
                    Toast.makeText(this, "친구 목록을 불러오는데 실패했습니다", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void acceptFriendRequest(Friend friend) {
        executor.execute(() -> {
            try {
                int updated = database.friendDao().updateFriendStatus(friend.id, true);
                if (updated > 0) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "친구 요청을 수락했습니다", Toast.LENGTH_SHORT).show();
                        loadFriends(); // 목록 새로고침
                    });
                }
            } catch (Exception e) {
                runOnUiThread(() -> 
                    Toast.makeText(this, "친구 요청 수락에 실패했습니다", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void rejectFriendRequest(Friend friend) {
        executor.execute(() -> {
            try {
                int deleted = database.friendDao().delete(friend);
                if (deleted > 0) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "친구 요청을 거절했습니다", Toast.LENGTH_SHORT).show();
                        loadFriends(); // 목록 새로고침
                    });
                }
            } catch (Exception e) {
                runOnUiThread(() -> 
                    Toast.makeText(this, "친구 요청 거절에 실패했습니다", Toast.LENGTH_SHORT).show());
            }
        });
    }

    /**
     * 친구 삭제 확인 다이얼로그 표시
     */
    private void showDeleteFriendDialog(Friend friend) {
        try {
            new AlertDialog.Builder(this)
                .setTitle("친구 삭제")
                .setMessage("'" + friend.getFriendName() + "'님을 친구 목록에서 삭제하시겠습니까?")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton("삭제", (dialog, which) -> {
                    deleteFriend(friend);
                })
                .setNegativeButton("취소", null)
                .show();
        } catch (Exception e) {
            android.util.Log.e(TAG, "친구 삭제 다이얼로그 표시 오류", e);
            Toast.makeText(this, "다이얼로그 표시 중 오류가 발생했습니다", Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteFriend(Friend friend) {
        executor.execute(() -> {
            try {
                int deleted = database.friendDao().delete(friend);
                if (deleted > 0) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "친구를 삭제했습니다", Toast.LENGTH_SHORT).show();
                        loadFriends(); // 목록 새로고침
                    });
                }
            } catch (Exception e) {
                runOnUiThread(() ->
                    Toast.makeText(this, "친구 삭제에 실패했습니다", Toast.LENGTH_SHORT).show());
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadFriends(); // 화면 복귀 시 새로고침
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
}
