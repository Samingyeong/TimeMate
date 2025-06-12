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
    private EditText editFriendNickname;
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
        editFriendNickname = findViewById(R.id.editFriendNickname);
        btnAddFriend = findViewById(R.id.btnAddFriend);
        btnBack = findViewById(R.id.btnBack);
    }

    private void initServices() {
        try {
            database = AppDatabase.getDatabase(this);
            userSession = UserSession.getInstance(this);
            executor = Executors.newSingleThreadExecutor();

            // UserSession 초기화 검증
            boolean isLoggedIn = userSession.isLoggedIn();
            String currentUserId = userSession.getCurrentUserId();
            String currentUserName = userSession.getCurrentUserName();

            Log.d(TAG, "서비스 초기화 완료");
            Log.d(TAG, "로그인 상태: " + isLoggedIn);
            Log.d(TAG, "현재 사용자 ID: " + currentUserId);
            Log.d(TAG, "현재 사용자 이름: " + currentUserName);

            if (!isLoggedIn) {
                Log.w(TAG, "⚠️ 로그인 상태가 아님");
                // 자동으로 테스트 사용자로 로그인 시도
                attemptTestUserLogin();
            } else if (currentUserId == null || currentUserId.trim().isEmpty()) {
                Log.w(TAG, "⚠️ 현재 사용자 ID가 설정되지 않음");
                // 자동으로 테스트 사용자로 로그인 시도
                attemptTestUserLogin();
            }

        } catch (Exception e) {
            Log.e(TAG, "서비스 초기화 오류", e);
            Toast.makeText(this, "앱 초기화 중 오류가 발생했습니다", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 테스트 사용자 자동 로그인 시도
     */
    private void attemptTestUserLogin() {
        executor.execute(() -> {
            try {
                Log.d(TAG, "테스트 사용자 자동 로그인 시도");

                // 기본 테스트 사용자 ID
                String testUserId = "testuser";
                String testUserName = "테스트사용자";

                // 데이터베이스에서 테스트 사용자 확인
                User testUser = database.userDao().getUserById(testUserId);

                if (testUser == null) {
                    // 테스트 사용자가 없으면 생성
                    Log.d(TAG, "테스트 사용자 생성 중...");
                    testUser = new User();
                    testUser.userId = testUserId;
                    testUser.nickname = testUserName;
                    testUser.email = testUserId + "@test.com";
                    testUser.password = "test123";

                    long userId = database.userDao().insert(testUser);

                    if (userId > 0) {
                        Log.d(TAG, "테스트 사용자 생성 성공: " + testUserId);
                    } else {
                        Log.e(TAG, "테스트 사용자 생성 실패");
                        runOnUiThread(() -> {
                            Toast.makeText(this, "테스트 사용자 생성에 실패했습니다", Toast.LENGTH_SHORT).show();
                        });
                        return;
                    }
                }

                // UserSession에 로그인 정보 설정
                final String finalTestUserId = testUserId;
                final String finalTestUserName = testUserName;
                final String finalTestUserEmail = testUser.email;

                runOnUiThread(() -> {
                    userSession.login(finalTestUserId, finalTestUserName, finalTestUserEmail, true);
                    Log.d(TAG, "테스트 사용자 자동 로그인 완료: " + finalTestUserId);
                    Toast.makeText(this, "테스트 사용자로 자동 로그인되었습니다", Toast.LENGTH_SHORT).show();
                });

            } catch (Exception e) {
                Log.e(TAG, "테스트 사용자 자동 로그인 오류", e);
                runOnUiThread(() -> {
                    Toast.makeText(this, "자동 로그인에 실패했습니다: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());
        
        btnAddFriend.setOnClickListener(v -> {
            String friendId = editFriendId.getText().toString().trim();
            String friendNickname = editFriendNickname.getText().toString().trim();

            if (friendId.isEmpty()) {
                Toast.makeText(this, "친구 ID를 입력해주세요", Toast.LENGTH_SHORT).show();
                editFriendId.requestFocus();
                return;
            }

            if (friendNickname.isEmpty()) {
                Toast.makeText(this, "닉네임을 입력해주세요", Toast.LENGTH_SHORT).show();
                editFriendNickname.requestFocus();
                return;
            }

            // 현재 사용자 ID 확인 (없으면 기본값 사용)
            String currentUserId = userSession.getCurrentUserId();
            if (currentUserId == null || currentUserId.trim().isEmpty()) {
                Log.w(TAG, "사용자 ID가 null - 기본 테스트 사용자 사용");
                currentUserId = "user1"; // 기본 테스트 사용자 ID

                // UserSession에 기본 사용자 정보 설정
                userSession.login(currentUserId, "사용자1", "user1@test.com", true);
                Toast.makeText(this, "기본 사용자(user1)로 설정되었습니다", Toast.LENGTH_SHORT).show();
            }

            Log.d(TAG, "최종 사용자 ID: " + currentUserId);

            if (friendId.equals(currentUserId)) {
                Toast.makeText(this, "자신을 친구로 추가할 수 없습니다", Toast.LENGTH_SHORT).show();
                return;
            }

            // 테스트용 사용자 생성 (친구 ID가 존재하지 않는 경우)
            createTestUserIfNeeded(friendId, friendNickname);
        });
    }

    /**
     * 테스트용 사용자 생성 (친구 ID가 존재하지 않는 경우)
     */
    private void createTestUserIfNeeded(String friendId, String friendNickname) {
        executor.execute(() -> {
            try {
                // 먼저 사용자가 존재하는지 확인
                User existingUser = database.userDao().getUserById(friendId);

                if (existingUser == null) {
                    Log.d(TAG, "사용자가 존재하지 않음, 테스트용 사용자 생성: " + friendId);

                    // 테스트용 사용자 생성
                    User testUser = new User();
                    testUser.userId = friendId;
                    testUser.nickname = friendNickname + "_실제이름"; // 실제 이름은 닉네임과 구분
                    testUser.email = friendId + "@test.com";
                    testUser.password = "test123"; // 테스트용 비밀번호

                    long userId = database.userDao().insert(testUser);

                    if (userId > 0) {
                        Log.d(TAG, "테스트용 사용자 생성 성공: " + friendId);
                        runOnUiThread(() -> {
                            Toast.makeText(this, "테스트용 사용자 '" + friendId + "'를 생성했습니다", Toast.LENGTH_SHORT).show();
                        });
                    } else {
                        Log.e(TAG, "테스트용 사용자 생성 실패");
                        runOnUiThread(() -> {
                            Toast.makeText(this, "사용자 생성에 실패했습니다", Toast.LENGTH_SHORT).show();
                            resetAddButton();
                        });
                        return;
                    }
                }

                // 사용자가 존재하거나 생성되었으면 친구 추가 진행
                addFriend(friendId, friendNickname);

            } catch (Exception e) {
                Log.e(TAG, "테스트용 사용자 생성 중 오류", e);
                runOnUiThread(() -> {
                    Toast.makeText(this, "사용자 생성 중 오류: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    resetAddButton();
                });
            }
        });
    }

    private void addFriend(String friendId, String friendNickname) {
        // 로딩 상태 표시
        runOnUiThread(() -> {
            btnAddFriend.setEnabled(false);
            btnAddFriend.setText("추가 중...");
        });

        executor.execute(() -> {
            try {
                String currentUserId = userSession.getCurrentUserId();
                String currentUserName = userSession.getCurrentUserName();

                // 사용자 정보가 없으면 기본값 사용
                if (currentUserId == null || currentUserId.trim().isEmpty()) {
                    currentUserId = "user1";
                    currentUserName = "사용자1";
                    Log.w(TAG, "사용자 정보가 없어 기본값 사용: " + currentUserId);

                    // UserSession에 기본 정보 설정
                    final String finalCurrentUserId = currentUserId;
                    final String finalCurrentUserName = currentUserName;

                    runOnUiThread(() -> {
                        userSession.login(finalCurrentUserId, finalCurrentUserName, "user1@test.com", true);
                    });
                }

                if (currentUserName == null || currentUserName.trim().isEmpty()) {
                    currentUserName = "사용자1";
                    Log.w(TAG, "사용자 이름이 없어 기본값 사용: " + currentUserName);
                }

                Log.d(TAG, "친구 추가 시작: " + friendId + " (닉네임: " + friendNickname + ")");
                Log.d(TAG, "현재 사용자 ID: " + currentUserId + ", 이름: " + currentUserName);

                // 0. 현재 사용자가 데이터베이스에 존재하는지 확인하고 없으면 생성
                User currentUser = database.userDao().getUserById(currentUserId);
                if (currentUser == null) {
                    Log.d(TAG, "현재 사용자가 DB에 없음, 생성 중: " + currentUserId);
                    currentUser = new User();
                    currentUser.userId = currentUserId;
                    currentUser.nickname = currentUserName;
                    currentUser.email = currentUserId + "@test.com";
                    currentUser.password = "test123";

                    long userId = database.userDao().insert(currentUser);
                    if (userId > 0) {
                        Log.d(TAG, "현재 사용자 생성 성공: " + currentUserId);
                    } else {
                        Log.e(TAG, "현재 사용자 생성 실패");
                        runOnUiThread(() -> {
                            Toast.makeText(this, "사용자 정보 생성에 실패했습니다", Toast.LENGTH_SHORT).show();
                            resetAddButton();
                        });
                        return;
                    }
                }

                // 1. 친구 사용자가 존재하는지 확인
                User friendUser = database.userDao().getUserById(friendId);
                if (friendUser == null) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "존재하지 않는 사용자입니다", Toast.LENGTH_SHORT).show();
                        resetAddButton();
                    });
                    return;
                }

                // 2. 이미 친구인지 확인
                Friend existingFriend = database.friendDao().getFriendship(currentUserId, friendId);
                if (existingFriend != null) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "이미 친구이거나 요청이 진행 중입니다", Toast.LENGTH_SHORT).show();
                        resetAddButton();
                    });
                    return;
                }

                // 3. 친구 관계 생성 (사용자가 입력한 닉네임 사용)
                Log.d(TAG, "Friend 객체 생성 시작 - currentUserId: " + currentUserId + ", friendId: " + friendId + ", nickname: " + friendNickname);

                Friend newFriend = new Friend(currentUserId, friendId, friendNickname);
                newFriend.acceptFriendRequest(); // isAccepted = true로 설정

                Log.d(TAG, "newFriend 생성 완료 - userId: " + newFriend.userId + ", friendUserId: " + newFriend.friendUserId);

                long friendId1 = database.friendDao().insert(newFriend);
                Log.d(TAG, "newFriend 삽입 결과: " + friendId1);

                // 4. 역방향 친구 관계도 생성 (상대방 입장에서는 실제 이름 사용)
                Log.d(TAG, "reverseFriend 객체 생성 시작 - friendId: " + friendId + ", currentUserId: " + currentUserId + ", currentUserName: " + currentUserName);

                Friend reverseFriend = new Friend(friendId, currentUserId, currentUserName);
                reverseFriend.acceptFriendRequest(); // isAccepted = true로 설정

                Log.d(TAG, "reverseFriend 생성 완료 - userId: " + reverseFriend.userId + ", friendUserId: " + reverseFriend.friendUserId);

                long friendId2 = database.friendDao().insert(reverseFriend);
                Log.d(TAG, "reverseFriend 삽입 결과: " + friendId2);

                if (friendId1 > 0 && friendId2 > 0) {
                    Log.d(TAG, "친구 추가 성공: " + friendNickname);
                    runOnUiThread(() -> {
                        Toast.makeText(this, "'" + friendNickname + "'님이 친구로 추가되었습니다!", Toast.LENGTH_SHORT).show();
                        clearInputFields();

                        // 결과를 부모 Activity에 전달
                        setResult(RESULT_OK);
                        finish();
                    });
                } else {
                    Log.e(TAG, "친구 추가 실패: DB 삽입 오류");
                    runOnUiThread(() -> {
                        Toast.makeText(this, "친구 추가에 실패했습니다", Toast.LENGTH_SHORT).show();
                        resetAddButton();
                    });
                }

            } catch (Exception e) {
                Log.e(TAG, "친구 추가 중 오류", e);
                runOnUiThread(() -> {
                    Toast.makeText(this, "오류가 발생했습니다: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    resetAddButton();
                });
            }
        });
    }

    /**
     * 추가 버튼 상태 초기화
     */
    private void resetAddButton() {
        btnAddFriend.setEnabled(true);
        btnAddFriend.setText("친구 추가");
    }

    /**
     * 입력 필드 초기화
     */
    private void clearInputFields() {
        editFriendId.setText("");
        editFriendNickname.setText("");
        resetAddButton();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
}
