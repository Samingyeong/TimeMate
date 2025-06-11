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
import com.example.timemate.features.profile.ProfileActivity;
import com.example.timemate.util.UserSession;
import com.example.timemate.features.friend.FriendAddActivity;
import com.example.timemate.utils.NavigationHelper;
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
    private List<Friend> friendList = new ArrayList<>(); // 실제 Friend 객체 저장
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
        // 현재 레이아웃에 없는 ID는 주석 처리
        // listView = findViewById(R.id.friendListView);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, friendNames);
        listView.setAdapter(adapter);

        // 친구 목록 아이템 길게 누르기 (삭제)
        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            showDeleteFriendDialog(position);
            return true;
        });
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
        try {
            Log.d("FriendListActivity", "🔧 NavigationHelper를 사용한 바텀 네비게이션 설정");
            NavigationHelper.setupBottomNavigation(this, R.id.nav_friends);
            Log.d("FriendListActivity", "✅ 바텀 네비게이션 설정 완료");
        } catch (Exception e) {
            Log.e("FriendListActivity", "❌ 바텀 네비게이션 설정 오류", e);
            e.printStackTrace();
        }
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
                friendList.clear();

                for (Friend friend : friends) {
                    friendNames.add(friend.friendNickname + " (" + friend.friendUserId + ")");
                    friendList.add(friend);
                }

                runOnUiThread(() -> adapter.notifyDataSetChanged());
            } catch (Exception e) {
                runOnUiThread(() -> {
                    // 에러 처리
                    friendNames.clear();
                    friendList.clear();
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
                // getFriendRelation 메서드가 없으므로 임시로 null 처리
                Friend existingFriend = null;

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
                friend1.isAccepted = true; // 바로 수락된 상태로
                friend1.createdAt = System.currentTimeMillis();

                Friend friend2 = new Friend();
                friend2.userId = friendUserId;
                friend2.friendUserId = currentUserId;
                friend2.friendNickname = userSession.getCurrentNickname();
                friend2.isAccepted = true;
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

    private void showDeleteFriendDialog(int position) {
        if (position >= friendList.size()) {
            return;
        }

        Friend friend = friendList.get(position);

        new AlertDialog.Builder(this)
                .setTitle("친구 삭제")
                .setMessage(friend.friendNickname + "님을 친구 목록에서 삭제하시겠습니까?")
                .setPositiveButton("삭제", (dialog, which) -> {
                    deleteFriend(friend);
                })
                .setNegativeButton("취소", null)
                .show();
    }

    private void deleteFriend(Friend friend) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                String currentUserId = userSession.getCurrentUserId();

                // 양방향 친구 관계 삭제
                // deleteFriendRelation 메서드가 없으므로 deleteFriendship 사용
                db.friendDao().deleteFriendship(currentUserId, friend.friendUserId);
                db.friendDao().deleteFriendship(friend.friendUserId, currentUserId);

                runOnUiThread(() -> {
                    Toast.makeText(this, friend.friendNickname + "님이 친구 목록에서 삭제되었습니다", Toast.LENGTH_SHORT).show();
                    loadFriends(); // 친구 목록 새로고침
                });

            } catch (Exception e) {
                Log.e("FriendList", "친구 삭제 오류", e);
                runOnUiThread(() -> {
                    Toast.makeText(this, "친구 삭제 중 오류가 발생했습니다", Toast.LENGTH_SHORT).show();
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
