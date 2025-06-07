package com.example.timemate;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.room.Room;

import java.util.concurrent.Executors;

public class FriendAddActivity extends AppCompatActivity {

    private AppDatabase db;
    private EditText editFriendId, editFriendNickname;
    private Button btnAddFriend;
    private ImageButton btnBack;
    private UserSession userSession;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friend_add);

        userSession = new UserSession(this);

        // 로그인 상태 확인
        if (!userSession.isLoggedIn()) {
            finish();
            return;
        }

        initViews();
        setupDatabase();
        setupClickListeners();
    }

    private void initViews() {
        editFriendId = findViewById(R.id.editFriendId);
        editFriendNickname = findViewById(R.id.editFriendNickname);
        btnAddFriend = findViewById(R.id.btnAddFriend);
        btnBack = findViewById(R.id.btnBack);
    }

    private void setupDatabase() {
        db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "timeMate-db")
                .fallbackToDestructiveMigration()
                .build();
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());
        
        btnAddFriend.setOnClickListener(v -> addFriend());
    }

    private void addFriend() {
        String friendId = editFriendId.getText().toString().trim();
        String friendNickname = editFriendNickname.getText().toString().trim();

        if (friendId.isEmpty()) {
            Toast.makeText(this, "친구 ID를 입력해주세요", Toast.LENGTH_SHORT).show();
            editFriendId.requestFocus();
            return;
        }

        if (friendNickname.isEmpty()) {
            Toast.makeText(this, "친구 닉네임을 입력해주세요", Toast.LENGTH_SHORT).show();
            editFriendNickname.requestFocus();
            return;
        }

        String currentUserId = userSession.getCurrentUserId();

        Executors.newSingleThreadExecutor().execute(() -> {
            // 실제 사용자 존재 여부 확인
            User targetUser = db.userDao().getUserById(friendId);

            if (targetUser == null) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "❌ 존재하지 않는 사용자 ID입니다\n\n다른 계정을 만들어서 테스트해보세요!", Toast.LENGTH_LONG).show();
                });
                return;
            }

            Log.d("FriendAdd", "Found target user: " + targetUser.nickname + " (" + targetUser.userId + ")");

            // 자기 자신을 친구로 추가하는지 확인
            if (friendId.equals(currentUserId)) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "자기 자신을 친구로 추가할 수 없습니다", Toast.LENGTH_SHORT).show();
                });
                return;
            }

            // 이미 추가된 친구인지 확인
            Friend existingFriend = db.friendDao().getFriendById(friendId, currentUserId);

            if (existingFriend != null) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "이미 추가된 친구입니다", Toast.LENGTH_SHORT).show();
                });
                return;
            }

            // 새 친구 추가
            Friend newFriend = new Friend();
            newFriend.userId = currentUserId;
            newFriend.friendId = friendId;
            newFriend.friendNickname = friendNickname.isEmpty() ? targetUser.nickname : friendNickname;

            db.friendDao().insert(newFriend);

            Log.d("FriendAdd", "Friend added: " + newFriend.friendNickname + " (" + newFriend.friendId + ") for user " + currentUserId);

            runOnUiThread(() -> {
                Toast.makeText(this, "✅ " + newFriend.friendNickname + "님이 친구로 추가되었습니다!", Toast.LENGTH_SHORT).show();
                finish();
            });
        });
    }
}
