package com.example.timemate.core.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * 사용자 세션 관리 클래스
 * 로그인 상태, 사용자 정보, 설정 등을 관리
 */
public class UserSession {

    private static final String TAG = "UserSession";
    private static final String PREF_NAME = "timemate_user_session";
    
    // SharedPreferences 키들
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_LOGIN_TIME = "login_time";
    private static final String KEY_LAST_ACTIVE_TIME = "last_active_time";
    private static final String KEY_AUTO_LOGIN = "auto_login";
    private static final String KEY_NOTIFICATION_ENABLED = "notification_enabled";
    private static final String KEY_WEATHER_LOCATION = "weather_location";
    private static final String KEY_ROUTE_PRIORITY = "route_priority"; // "time" or "cost"
    private static final String KEY_INCLUDE_REALTIME_DATA = "include_realtime_data";

    private static UserSession instance;
    private SharedPreferences preferences;
    private SharedPreferences.Editor editor;

    private UserSession(Context context) {
        preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = preferences.edit();
    }

    public static synchronized UserSession getInstance(Context context) {
        if (instance == null) {
            instance = new UserSession(context.getApplicationContext());
        }
        return instance;
    }

    /**
     * 사용자 로그인 처리
     */
    public void login(String userId, String userName, String userEmail, boolean autoLogin) {
        Log.d(TAG, "User login: " + userId + ", " + userName);
        
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putString(KEY_USER_ID, userId);
        editor.putString(KEY_USER_NAME, userName);
        editor.putString(KEY_USER_EMAIL, userEmail);
        editor.putLong(KEY_LOGIN_TIME, System.currentTimeMillis());
        editor.putLong(KEY_LAST_ACTIVE_TIME, System.currentTimeMillis());
        editor.putBoolean(KEY_AUTO_LOGIN, autoLogin);
        editor.apply();
    }

    /**
     * 사용자 로그아웃 처리
     */
    public void logout() {
        Log.d(TAG, "User logout");
        
        String userId = getCurrentUserId();
        
        // 로그인 관련 정보만 삭제 (설정은 유지)
        editor.remove(KEY_IS_LOGGED_IN);
        editor.remove(KEY_USER_ID);
        editor.remove(KEY_USER_NAME);
        editor.remove(KEY_USER_EMAIL);
        editor.remove(KEY_LOGIN_TIME);
        editor.remove(KEY_LAST_ACTIVE_TIME);
        editor.remove(KEY_AUTO_LOGIN);
        editor.apply();
        
        Log.d(TAG, "User " + userId + " logged out successfully");
    }

    /**
     * 로그인 상태 확인
     */
    public boolean isLoggedIn() {
        boolean isLoggedIn = preferences.getBoolean(KEY_IS_LOGGED_IN, false);
        
        if (isLoggedIn) {
            // 마지막 활동 시간 업데이트
            updateLastActiveTime();
        }
        
        return isLoggedIn;
    }

    /**
     * 자동 로그인 설정 확인
     */
    public boolean isAutoLoginEnabled() {
        return preferences.getBoolean(KEY_AUTO_LOGIN, false);
    }

    /**
     * 현재 사용자 ID 조회
     */
    public String getCurrentUserId() {
        return preferences.getString(KEY_USER_ID, null);
    }

    /**
     * 현재 사용자 이름 조회
     */
    public String getCurrentUserName() {
        return preferences.getString(KEY_USER_NAME, null);
    }

    /**
     * 현재 사용자 이메일 조회
     */
    public String getCurrentUserEmail() {
        return preferences.getString(KEY_USER_EMAIL, null);
    }

    /**
     * 로그인 시간 조회
     */
    public long getLoginTime() {
        return preferences.getLong(KEY_LOGIN_TIME, 0);
    }

    /**
     * 마지막 활동 시간 조회
     */
    public long getLastActiveTime() {
        return preferences.getLong(KEY_LAST_ACTIVE_TIME, 0);
    }

    /**
     * 마지막 활동 시간 업데이트
     */
    public void updateLastActiveTime() {
        editor.putLong(KEY_LAST_ACTIVE_TIME, System.currentTimeMillis());
        editor.apply();
    }

    /**
     * 사용자 정보 업데이트
     */
    public void updateUserInfo(String userName, String userEmail) {
        if (isLoggedIn()) {
            editor.putString(KEY_USER_NAME, userName);
            editor.putString(KEY_USER_EMAIL, userEmail);
            editor.apply();
            
            Log.d(TAG, "User info updated: " + userName + ", " + userEmail);
        }
    }

    /**
     * 알림 설정 관리
     */
    public boolean isNotificationEnabled() {
        return preferences.getBoolean(KEY_NOTIFICATION_ENABLED, true);
    }

    public void setNotificationEnabled(boolean enabled) {
        editor.putBoolean(KEY_NOTIFICATION_ENABLED, enabled);
        editor.apply();
        
        Log.d(TAG, "Notification setting changed: " + enabled);
    }

    /**
     * 날씨 위치 설정 관리
     */
    public String getWeatherLocation() {
        return preferences.getString(KEY_WEATHER_LOCATION, "Seoul");
    }

    public void setWeatherLocation(String location) {
        editor.putString(KEY_WEATHER_LOCATION, location);
        editor.apply();

        Log.d(TAG, "Weather location changed: " + location);
    }

    /**
     * 경로 우선순위 설정 관리
     */
    public String getRoutePriority() {
        return preferences.getString(KEY_ROUTE_PRIORITY, "time"); // 기본값: 시간 우선
    }

    public void setRoutePriority(String priority) {
        editor.putString(KEY_ROUTE_PRIORITY, priority);
        editor.apply();

        Log.d(TAG, "Route priority changed: " + priority);
    }

    /**
     * 실시간 데이터 포함 설정
     */
    public boolean isRealtimeDataEnabled() {
        return preferences.getBoolean(KEY_INCLUDE_REALTIME_DATA, true);
    }

    public void setRealtimeDataEnabled(boolean enabled) {
        editor.putBoolean(KEY_INCLUDE_REALTIME_DATA, enabled);
        editor.apply();

        Log.d(TAG, "Realtime data setting changed: " + enabled);
    }

    /**
     * 세션 유효성 검사
     */
    public boolean isSessionValid() {
        if (!isLoggedIn()) {
            return false;
        }

        // 7일 이상 비활성 상태면 세션 만료
        long lastActive = getLastActiveTime();
        long currentTime = System.currentTimeMillis();
        long inactiveDays = (currentTime - lastActive) / (1000 * 60 * 60 * 24);

        if (inactiveDays > 7) {
            Log.d(TAG, "Session expired due to inactivity: " + inactiveDays + " days");
            logout();
            return false;
        }

        return true;
    }

    /**
     * 사용자 세션 정보 조회
     */
    public SessionInfo getSessionInfo() {
        SessionInfo info = new SessionInfo();
        info.isLoggedIn = isLoggedIn();
        info.userId = getCurrentUserId();
        info.userName = getCurrentUserName();
        info.userEmail = getCurrentUserEmail();
        info.loginTime = getLoginTime();
        info.lastActiveTime = getLastActiveTime();
        info.autoLoginEnabled = isAutoLoginEnabled();
        info.notificationEnabled = isNotificationEnabled();
        info.weatherLocation = getWeatherLocation();
        
        return info;
    }

    /**
     * 세션 정보 데이터 클래스
     */
    public static class SessionInfo {
        public boolean isLoggedIn;
        public String userId;
        public String userName;
        public String userEmail;
        public long loginTime;
        public long lastActiveTime;
        public boolean autoLoginEnabled;
        public boolean notificationEnabled;
        public String weatherLocation;

        @Override
        public String toString() {
            return "SessionInfo{" +
                    "isLoggedIn=" + isLoggedIn +
                    ", userId='" + userId + '\'' +
                    ", userName='" + userName + '\'' +
                    ", userEmail='" + userEmail + '\'' +
                    ", autoLoginEnabled=" + autoLoginEnabled +
                    ", notificationEnabled=" + notificationEnabled +
                    ", weatherLocation='" + weatherLocation + '\'' +
                    '}';
        }
    }

    /**
     * 모든 사용자 데이터 삭제 (앱 삭제 시)
     */
    public void clearAllData() {
        Log.d(TAG, "Clearing all user data");
        editor.clear();
        editor.apply();
    }

    // 호환성을 위한 메서드들
    public void logoutUser() {
        logout();
    }
}
