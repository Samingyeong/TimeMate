package com.example.timemate.features.friend;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
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
import com.example.timemate.util.UserSession;
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
    private static final int REQUEST_ADD_FRIEND = 1001;
    
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

        try {
            Log.d(TAG, "🚀 FriendListActivity onCreate 시작");
            setContentView(R.layout.activity_friend_list);

            initViews();
            initServices();
            setupRecyclerView();
            setupBottomNavigation();
            setupClickListeners();

            // 로그인 상태 확인
            Log.d(TAG, "🔍 로그인 상태 확인 중...");
            if (userSession == null) {
                Log.e(TAG, "❌ UserSession이 null입니다!");
                Toast.makeText(this, "사용자 세션 오류가 발생했습니다", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            boolean isLoggedIn = userSession.isLoggedIn();
            String currentUserId = userSession.getCurrentUserId();

            Log.d(TAG, "🔍 로그인 상태: " + isLoggedIn);
            Log.d(TAG, "🔍 현재 사용자 ID: " + currentUserId);

            if (!isLoggedIn || currentUserId == null || currentUserId.trim().isEmpty()) {
                Log.w(TAG, "⚠️ 로그인되지 않았거나 사용자 ID가 없음");
                Toast.makeText(this, "로그인이 필요합니다", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            Log.d(TAG, "✅ 로그인 상태 확인 완료");
            loadFriends();

            Log.d(TAG, "🎉 FriendListActivity onCreate 완료");

        } catch (Exception e) {
            Log.e(TAG, "❌ FriendListActivity onCreate 오류", e);
            Toast.makeText(this, "친구 목록을 불러오는 중 오류가 발생했습니다: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void initViews() {
        recyclerFriends = findViewById(R.id.recyclerFriends);
        bottomNavigation = findViewById(R.id.bottomNavigationView);
        fabAddFriend = findViewById(R.id.fabAddFriend);
    }

    private void initServices() {
        try {
            Log.d(TAG, "🔧 서비스 초기화 시작");

            database = AppDatabase.getDatabase(this);
            if (database == null) {
                throw new RuntimeException("데이터베이스 초기화 실패");
            }
            Log.d(TAG, "✅ 데이터베이스 초기화 완료");

            userSession = UserSession.getInstance(this);
            if (userSession == null) {
                throw new RuntimeException("UserSession 초기화 실패");
            }
            Log.d(TAG, "✅ UserSession 초기화 완료");

            executor = Executors.newSingleThreadExecutor();
            if (executor == null) {
                throw new RuntimeException("Executor 초기화 실패");
            }
            Log.d(TAG, "✅ Executor 초기화 완료");

            Log.d(TAG, "🎉 모든 서비스 초기화 완료");

        } catch (Exception e) {
            Log.e(TAG, "❌ 서비스 초기화 오류", e);
            throw new RuntimeException("서비스 초기화 실패: " + e.getMessage(), e);
        }
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
            startActivityForResult(intent, REQUEST_ADD_FRIEND);
        });
    }

    private void loadFriends() {
        if (executor == null || executor.isShutdown()) {
            Log.e(TAG, "❌ Executor가 null이거나 종료됨");
            Toast.makeText(this, "서비스 오류가 발생했습니다", Toast.LENGTH_SHORT).show();
            return;
        }

        executor.execute(() -> {
            try {
                Log.d(TAG, "📊 친구 목록 로드 시작");

                // 사용자 세션 재확인
                if (userSession == null) {
                    Log.e(TAG, "❌ UserSession이 null입니다!");
                    runOnUiThread(() -> {
                        Toast.makeText(this, "사용자 세션 오류", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                    return;
                }

                String currentUserId = userSession.getCurrentUserId();
                if (currentUserId == null || currentUserId.trim().isEmpty()) {
                    Log.w(TAG, "⚠️ 사용자 ID가 null - 로그인 필요");
                    runOnUiThread(() -> {
                        Toast.makeText(this, "로그인이 필요합니다", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                    return;
                }

                Log.d(TAG, "👤 현재 사용자 ID: " + currentUserId);

                // 데이터베이스 확인
                if (database == null || database.friendDao() == null) {
                    Log.e(TAG, "❌ 데이터베이스 또는 FriendDao가 null입니다!");
                    runOnUiThread(() -> {
                        Toast.makeText(this, "데이터베이스 오류", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                    return;
                }

                List<Friend> friends = database.friendDao().getFriendsByUserId(currentUserId);
                Log.d(TAG, "📋 친구 목록 조회 결과: " + (friends != null ? friends.size() : "null") + "개");

                runOnUiThread(() -> {
                    try {
                        if (friendList == null) {
                            Log.e(TAG, "❌ friendList가 null입니다!");
                            return;
                        }

                        if (friendAdapter == null) {
                            Log.e(TAG, "❌ friendAdapter가 null입니다!");
                            return;
                        }

                        friendList.clear();
                        if (friends != null) {
                            friendList.addAll(friends);
                            Log.d(TAG, "✅ 친구 목록 업데이트 완료: " + friendList.size() + "개");
                        }
                        friendAdapter.notifyDataSetChanged();

                        // 친구 목록이 비어있으면 안내 메시지
                        if (friendList.isEmpty()) {
                            Toast.makeText(this, "등록된 친구가 없습니다. 친구를 추가해보세요!", Toast.LENGTH_SHORT).show();
                        }

                    } catch (Exception uiException) {
                        Log.e(TAG, "❌ UI 업데이트 중 오류", uiException);
                        Toast.makeText(this, "화면 업데이트 중 오류가 발생했습니다", Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "❌ 친구 목록 로드 오류", e);
                runOnUiThread(() ->
                    Toast.makeText(this, "친구 목록을 불러오는데 실패했습니다: " + e.getMessage(), Toast.LENGTH_LONG).show());
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_ADD_FRIEND && resultCode == RESULT_OK) {
            Log.d(TAG, "친구 추가 완료 - 목록 새로고침");
            Toast.makeText(this, "친구 목록을 새로고침합니다", Toast.LENGTH_SHORT).show();
            loadFriends(); // 친구 추가 완료 시 목록 새로고침
        }
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
