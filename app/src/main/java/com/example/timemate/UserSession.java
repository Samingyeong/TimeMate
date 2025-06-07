package com.example.timemate;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class UserSession {
    private static final String PREF_NAME = "user_session";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_NICKNAME = "nickname";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";

    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;
    private Context context;

    public UserSession(Context context) {
        this.context = context;
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = prefs.edit();
    }

    // 로그인 세션 생성
    public void createLoginSession(User user) {
        editor.putString(KEY_USER_ID, user.userId);
        editor.putString(KEY_NICKNAME, user.nickname);
        editor.putString(KEY_EMAIL, user.email);
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.commit();
    }

    // 현재 로그인된 사용자 ID 가져오기
    public String getCurrentUserId() {
        return prefs.getString(KEY_USER_ID, null);
    }

    // 현재 로그인된 사용자 닉네임 가져오기
    public String getCurrentUserNickname() {
        return prefs.getString(KEY_NICKNAME, "사용자");
    }

    // 현재 로그인된 사용자 이메일 가져오기
    public String getCurrentUserEmail() {
        return prefs.getString(KEY_EMAIL, "");
    }

    // 로그인 상태 확인
    public boolean isLoggedIn() {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    // 로그아웃 - 완전한 데이터 초기화
    public void logoutUser() {
        Log.d("UserSession", "Logging out user: " + getCurrentUserId());

        // SharedPreferences 완전 초기화
        editor.clear();
        editor.commit();

        // 추가적으로 각 키를 명시적으로 제거
        editor.remove(KEY_USER_ID);
        editor.remove(KEY_NICKNAME);
        editor.remove(KEY_EMAIL);
        editor.remove(KEY_IS_LOGGED_IN);
        editor.commit();

        Log.d("UserSession", "User logged out completely");
    }

    // 사용자 정보 업데이트
    public void updateUserInfo(String nickname, String email) {
        editor.putString(KEY_NICKNAME, nickname);
        editor.putString(KEY_EMAIL, email);
        editor.commit();
    }

    // 세션 정보를 User 객체로 반환
    public User getCurrentUser() {
        if (!isLoggedIn()) {
            return null;
        }

        User user = new User();
        user.userId = getCurrentUserId();
        user.nickname = getCurrentUserNickname();
        user.email = getCurrentUserEmail();
        return user;
    }

    /**
     * 현재 로그인된 사용자 정보 로그 출력 (디버깅용)
     */
    public void debugCurrentUser() {
        Log.d("UserSession", "=== Current User Info ===");
        Log.d("UserSession", "Is Logged In: " + isLoggedIn());
        Log.d("UserSession", "User ID: " + getCurrentUserId());
        Log.d("UserSession", "Nickname: " + getCurrentUserNickname());
        Log.d("UserSession", "Email: " + getCurrentUserEmail());
        Log.d("UserSession", "========================");
    }
}
