package com.example.timemate.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * 사용자 세션 관리 유틸리티
 * 로그인 상태, 현재 사용자 정보 관리
 */
public class UserSession {
    
    private static final String TAG = "UserSession";
    private static final String PREF_NAME = "timemate_session";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_CURRENT_USER_ID = "current_user_id";
    private static final String KEY_CURRENT_NICKNAME = "current_nickname";
    private static final String KEY_CURRENT_EMAIL = "current_email";
    private static final String KEY_LOGIN_TIME = "login_time";

    // 경로 설정 관련 키들
    private static final String KEY_ROUTE_PRIORITY = "route_priority";
    private static final String KEY_REALTIME_DATA_ENABLED = "realtime_data_enabled";
    
    private static UserSession instance;
    private SharedPreferences preferences;
    private Context context;

    private UserSession(Context context) {
        this.context = context.getApplicationContext();
        this.preferences = this.context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    /**
     * 싱글톤 인스턴스 가져오기
     */
    public static synchronized UserSession getInstance(Context context) {
        if (instance == null) {
            instance = new UserSession(context);
        }
        return instance;
    }

    /**
     * 사용자 로그인 처리
     */
    public void login(String userId, String nickname) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putString(KEY_CURRENT_USER_ID, userId);
        editor.putString(KEY_CURRENT_NICKNAME, nickname);
        editor.putLong(KEY_LOGIN_TIME, System.currentTimeMillis());
        editor.apply();

        Log.d(TAG, "User logged in: " + userId + " (" + nickname + ")");
    }

    /**
     * 사용자 로그인 처리 (이메일 포함)
     */
    public void login(String userId, String nickname, String email) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putString(KEY_CURRENT_USER_ID, userId);
        editor.putString(KEY_CURRENT_NICKNAME, nickname);
        editor.putString(KEY_CURRENT_EMAIL, email);
        editor.putLong(KEY_LOGIN_TIME, System.currentTimeMillis());
        editor.apply();

        Log.d(TAG, "User logged in: " + userId + " (" + nickname + ")");
    }

    /**
     * 사용자 로그아웃 처리
     */
    public void logout() {
        String currentUserId = getCurrentUserId();

        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(KEY_IS_LOGGED_IN, false);
        editor.remove(KEY_CURRENT_USER_ID);
        editor.remove(KEY_CURRENT_NICKNAME);
        editor.remove(KEY_CURRENT_EMAIL);
        editor.remove(KEY_LOGIN_TIME);
        // 경로 설정은 유지 (사용자 설정이므로)
        editor.apply();

        Log.d(TAG, "User logged out: " + currentUserId);
    }

    /**
     * 로그인 상태 확인
     */
    public boolean isLoggedIn() {
        boolean loggedIn = preferences.getBoolean(KEY_IS_LOGGED_IN, false);
        String userId = preferences.getString(KEY_CURRENT_USER_ID, null);
        
        // 사용자 ID가 없으면 로그인 상태가 아님
        return loggedIn && userId != null && !userId.isEmpty();
    }

    /**
     * 현재 사용자 ID 가져오기
     */
    public String getCurrentUserId() {
        return preferences.getString(KEY_CURRENT_USER_ID, null);
    }

    /**
     * 현재 사용자 닉네임 가져오기
     */
    public String getCurrentNickname() {
        return preferences.getString(KEY_CURRENT_NICKNAME, null);
    }

    /**
     * 현재 사용자 이름 가져오기 (닉네임과 동일)
     */
    public String getCurrentUserName() {
        return getCurrentNickname();
    }

    /**
     * 현재 사용자 이메일 가져오기
     */
    public String getCurrentUserEmail() {
        return preferences.getString(KEY_CURRENT_EMAIL, null);
    }

    /**
     * 로그인 시간 가져오기
     */
    public long getLoginTime() {
        return preferences.getLong(KEY_LOGIN_TIME, 0);
    }

    /**
     * 닉네임 업데이트
     */
    public void updateNickname(String newNickname) {
        if (isLoggedIn()) {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString(KEY_CURRENT_NICKNAME, newNickname);
            editor.apply();
            
            Log.d(TAG, "Nickname updated: " + newNickname);
        }
    }

    /**
     * 세션 유효성 검사
     */
    public boolean isSessionValid() {
        if (!isLoggedIn()) {
            return false;
        }
        
        long loginTime = getLoginTime();
        long currentTime = System.currentTimeMillis();
        long sessionDuration = currentTime - loginTime;
        
        // 30일 세션 유지 (밀리초)
        long maxSessionDuration = 30L * 24 * 60 * 60 * 1000;
        
        if (sessionDuration > maxSessionDuration) {
            Log.d(TAG, "Session expired, logging out");
            logout();
            return false;
        }
        
        return true;
    }

    /**
     * 세션 갱신 (로그인 시간 업데이트)
     */
    public void refreshSession() {
        if (isLoggedIn()) {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putLong(KEY_LOGIN_TIME, System.currentTimeMillis());
            editor.apply();
            
            Log.d(TAG, "Session refreshed");
        }
    }

    /**
     * 모든 세션 데이터 삭제
     */
    public void clearAllData() {
        SharedPreferences.Editor editor = preferences.edit();
        editor.clear();
        editor.apply();
        
        Log.d(TAG, "All session data cleared");
    }

    // ========== 경로 설정 관련 메서드들 ==========

    /**
     * 경로 우선순위 설정 가져오기
     */
    public String getRoutePriority() {
        return preferences.getString(KEY_ROUTE_PRIORITY, "time"); // 기본값: 시간 우선
    }

    /**
     * 경로 우선순위 설정
     */
    public void setRoutePriority(String priority) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(KEY_ROUTE_PRIORITY, priority);
        editor.apply();

        Log.d(TAG, "Route priority set: " + priority);
    }

    /**
     * 실시간 데이터 사용 여부 가져오기
     */
    public boolean isRealtimeDataEnabled() {
        return preferences.getBoolean(KEY_REALTIME_DATA_ENABLED, true); // 기본값: 활성화
    }

    /**
     * 실시간 데이터 사용 여부 설정
     */
    public void setRealtimeDataEnabled(boolean enabled) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(KEY_REALTIME_DATA_ENABLED, enabled);
        editor.apply();

        Log.d(TAG, "Realtime data enabled: " + enabled);
    }

    // ========== 호환성을 위한 메서드들 ==========

    /**
     * 사용자 로그아웃 (ProfileActivity 호환성)
     */
    public void logoutUser() {
        logout();
    }

    /**
     * 세션 정보 로깅 (디버그용)
     */
    public void logSessionInfo() {
        Log.d(TAG, "=== Session Info ===");
        Log.d(TAG, "Logged in: " + isLoggedIn());
        Log.d(TAG, "User ID: " + getCurrentUserId());
        Log.d(TAG, "Nickname: " + getCurrentNickname());
        Log.d(TAG, "Email: " + getCurrentUserEmail());
        Log.d(TAG, "Login time: " + getLoginTime());
        Log.d(TAG, "Session valid: " + isSessionValid());
        Log.d(TAG, "Route priority: " + getRoutePriority());
        Log.d(TAG, "Realtime data: " + isRealtimeDataEnabled());
        Log.d(TAG, "==================");
    }
}
