package com.example.timemate.features.friend;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.timemate.R;
import com.example.timemate.data.database.AppDatabase;
import com.example.timemate.data.model.Friend;
import com.example.timemate.data.model.User;
import com.example.timemate.core.util.UserSession;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 친구 추가 화면
 */
public class FriendAddActivity extends AppCompatActivity {

    private static final String TAG = "FriendAddActivity";
    
    private AppDatabase database;
    private UserSession userSession;
    private ExecutorService executor;
    
    private EditText editFriendId;
    private Button btnAddFriend;
    private ImageButton btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friend_add);

        initViews();
        initServices();
        setupClickListeners();
    }

    private void initViews() {
        editFriendId = findViewById(R.id.editFriendId);
        btnAddFriend = findViewById(R.id.btnAddFriend);
        btnBack = findViewById(R.id.btnBack);
    }

    private void initServices() {
        database = AppDatabase.getDatabase(this);
        userSession = UserSession.getInstance(this);
        executor = Executors.newSingleThreadExecutor();
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());
        
        btnAddFriend.setOnClickListener(v -> {
            String friendId = editFriendId.getText().toString().trim();
            if (friendId.isEmpty()) {
                Toast.makeText(this, "친구 ID를 입력해주세요", Toast.LENGTH_SHORT).show();
                return;
            }
            
            String currentUserId = userSession.getCurrentUserId();
            if (friendId.equals(currentUserId)) {
                Toast.makeText(this, "자신을 친구로 추가할 수 없습니다", Toast.LENGTH_SHORT).show();
                return;
            }
            
            addFriend(friendId);
        });
    }

    private void addFriend(String friendId) {
        executor.execute(() -> {
            try {
                String currentUserId = userSession.getCurrentUserId();
                
                // 1. 친구 사용자가 존재하는지 확인
                User friendUser = database.userDao().getUserById(friendId);
                if (friendUser == null) {
                    runOnUiThread(() -> 
                        Toast.makeText(this, "존재하지 않는 사용자입니다", Toast.LENGTH_SHORT).show());
                    return;
                }
                
                // 2. 이미 친구인지 확인
                Friend existingFriend = database.friendDao().getFriendship(currentUserId, friendId);
                if (existingFriend != null) {
                    runOnUiThread(() -> 
                        Toast.makeText(this, "이미 친구이거나 요청이 진행 중입니다", Toast.LENGTH_SHORT).show());
                    return;
                }
                
                // 3. 친구 관계 생성 (바로 수락된 상태로)
                Friend newFriend = new Friend(currentUserId, friendId, friendUser.getName());
                newFriend.setStatus("accepted");

                long friendId1 = database.friendDao().insert(newFriend);

                // 4. 역방향 친구 관계도 생성 (상대방 입장에서)
                Friend reverseFriend = new Friend(friendId, currentUserId, userSession.getCurrentUserName());
                reverseFriend.setStatus("accepted");

                long friendId2 = database.friendDao().insert(reverseFriend);
                
                if (friendId1 > 0 && friendId2 > 0) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, friendUser.getName() + "님이 친구로 추가되었습니다", Toast.LENGTH_SHORT).show();
                        editFriendId.setText("");
                        finish();
                    });
                } else {
                    runOnUiThread(() ->
                        Toast.makeText(this, "친구 추가에 실패했습니다", Toast.LENGTH_SHORT).show());
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error adding friend", e);
                runOnUiThread(() -> 
                    Toast.makeText(this, "오류가 발생했습니다: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
}
