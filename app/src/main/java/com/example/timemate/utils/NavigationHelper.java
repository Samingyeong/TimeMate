package com.example.timemate.utils;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import com.example.timemate.R;
import com.example.timemate.features.friend.FriendListActivity;
import com.example.timemate.features.schedule.ScheduleListActivity;
import com.example.timemate.ui.home.HomeActivity;
import com.example.timemate.ui.recommendation.RecommendationActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * 바텀 네비게이션 공통 헬퍼 클래스
 * 모든 액티비티에서 일관된 네비게이션 동작 보장
 */
public class NavigationHelper {
    
    private static final String TAG = "NavigationHelper";
    
    /**
     * 바텀 네비게이션 설정
     * @param activity 현재 액티비티
     * @param currentMenuId 현재 선택된 메뉴 ID
     */
    public static void setupBottomNavigation(Activity activity, int currentMenuId) {
        try {
            BottomNavigationView bottomNav = activity.findViewById(R.id.bottomNavigationView);
            if (bottomNav == null) {
                Log.e(TAG, "BottomNavigationView를 찾을 수 없습니다");
                return;
            }

            // 현재 메뉴 선택
            bottomNav.setSelectedItemId(currentMenuId);
            
            // 네비게이션 리스너 설정
            bottomNav.setOnItemSelectedListener(new BottomNavigationView.OnItemSelectedListener() {
                @Override
                public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                    return handleNavigationItemSelected(activity, item, currentMenuId);
                }
            });
            
            Log.d(TAG, "바텀 네비게이션 설정 완료: " + activity.getClass().getSimpleName());
            
        } catch (Exception e) {
            Log.e(TAG, "바텀 네비게이션 설정 오류", e);
        }
    }
    
    /**
     * 네비게이션 아이템 선택 처리
     */
    private static boolean handleNavigationItemSelected(Activity activity, MenuItem item, int currentMenuId) {
        try {
            // 액티비티 상태 확인
            if (activity.isFinishing() || activity.isDestroyed()) {
                Log.w(TAG, "액티비티가 종료 중이므로 네비게이션 무시");
                return false;
            }
            
            int itemId = item.getItemId();
            Log.d(TAG, "네비게이션 클릭: " + item.getTitle() + " (현재: " + getCurrentActivityName(currentMenuId) + ")");
            
            // 현재 화면과 같은 메뉴 클릭 시 무시
            if (itemId == currentMenuId) {
                Log.d(TAG, "현재 화면과 동일한 메뉴 클릭, 무시");
                return true;
            }
            
            // 목적지 액티비티 결정
            Class<?> targetActivity = getTargetActivity(itemId);
            if (targetActivity == null) {
                Log.w(TAG, "알 수 없는 메뉴 ID: " + itemId);
                return false;
            }
            
            // 안전한 네비게이션 실행
            navigateToActivity(activity, targetActivity);
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "네비게이션 처리 오류", e);
            return false;
        }
    }
    
    /**
     * 메뉴 ID에 따른 목적지 액티비티 반환
     */
    private static Class<?> getTargetActivity(int menuId) {
        if (menuId == R.id.nav_home) {
            return HomeActivity.class;
        } else if (menuId == R.id.nav_schedule) {
            return ScheduleListActivity.class;
        } else if (menuId == R.id.nav_friends) {
            return FriendListActivity.class;
        } else if (menuId == R.id.nav_recommendation) {
            return RecommendationActivity.class;
        } else if (menuId == R.id.nav_profile) {
            return com.example.timemate.features.profile.ProfileActivity.class;
        } else {
            return null;
        }
    }
    
    /**
     * 현재 액티비티 이름 반환 (디버깅용)
     */
    private static String getCurrentActivityName(int currentMenuId) {
        if (currentMenuId == R.id.nav_home) {
            return "Home";
        } else if (currentMenuId == R.id.nav_schedule) {
            return "Schedule";
        } else if (currentMenuId == R.id.nav_friends) {
            return "Friends";
        } else if (currentMenuId == R.id.nav_recommendation) {
            return "Recommendation";
        } else if (currentMenuId == R.id.nav_profile) {
            return "Profile";
        } else {
            return "Unknown";
        }
    }
    
    /**
     * 안전한 액티비티 네비게이션
     */
    private static void navigateToActivity(Activity currentActivity, Class<?> targetActivity) {
        try {
            Log.d(TAG, "🚀 네비게이션 시작: " + currentActivity.getClass().getSimpleName() + " → " + targetActivity.getSimpleName());

            // 액티비티 상태 재확인
            if (currentActivity.isFinishing() || currentActivity.isDestroyed()) {
                Log.w(TAG, "❌ 액티비티가 이미 종료되어 네비게이션 취소");
                return;
            }

            // RecommendationActivity 특별 처리
            if (targetActivity == RecommendationActivity.class) {
                Log.d(TAG, "🎯 RecommendationActivity로 네비게이션 시작");
            }

            Intent intent = new Intent(currentActivity, targetActivity);
            Log.d(TAG, "✅ Intent 생성 완료: " + intent.toString());

            // 액티비티 스택 관리 - 새로운 태스크로 시작하지 않고 기존 태스크 내에서 관리
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            Log.d(TAG, "✅ Intent 플래그 설정 완료");

            Log.d(TAG, "🔄 startActivity 호출 중...");
            currentActivity.startActivity(intent);
            Log.d(TAG, "✅ startActivity 호출 완료");

            Log.d(TAG, "🔄 현재 액티비티 종료 중...");
            // 현재 액티비티 종료 (메모리 절약)
            currentActivity.finish();
            Log.d(TAG, "✅ 현재 액티비티 종료 완료");

            // 부드러운 전환 애니메이션 (안전하게 적용)
            try {
                currentActivity.overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                Log.d(TAG, "✅ 전환 애니메이션 적용 완료");
            } catch (Exception animException) {
                Log.w(TAG, "⚠️ 전환 애니메이션 적용 실패 (무시): " + animException.getMessage());
            }

            Log.d(TAG, "🎉 네비게이션 완료");

        } catch (Exception e) {
            Log.e(TAG, "❌ 액티비티 네비게이션 오류", e);
            e.printStackTrace();

            // 오류 발생 시 기본 방식으로 재시도
            try {
                Log.d(TAG, "🔄 기본 방식으로 네비게이션 재시도");
                Intent fallbackIntent = new Intent(currentActivity, targetActivity);
                currentActivity.startActivity(fallbackIntent);
                currentActivity.finish();
                Log.d(TAG, "✅ 기본 방식 네비게이션 성공");
            } catch (Exception fallbackException) {
                Log.e(TAG, "❌ 기본 방식 네비게이션도 실패", fallbackException);

                // 최종 폴백: 토스트 메시지 표시
                try {
                    android.widget.Toast.makeText(currentActivity,
                        "추천 페이지를 열 수 없습니다. 앱을 재시작해주세요.",
                        android.widget.Toast.LENGTH_LONG).show();
                } catch (Exception toastException) {
                    Log.e(TAG, "토스트 메시지도 실패", toastException);
                }
            }
        }
    }
    
    /**
     * 홈 화면으로 직접 이동 (특별한 경우)
     */
    public static void navigateToHome(Activity currentActivity) {
        try {
            Log.d(TAG, "홈 화면으로 직접 이동");
            navigateToActivity(currentActivity, HomeActivity.class);
        } catch (Exception e) {
            Log.e(TAG, "홈 네비게이션 오류", e);
        }
    }
    
    /**
     * 현재 액티비티가 홈 액티비티인지 확인
     */
    public static boolean isHomeActivity(Activity activity) {
        return activity instanceof HomeActivity;
    }
    
    /**
     * 바텀 네비게이션 메뉴 ID 가져오기
     */
    public static int getMenuIdForActivity(Activity activity) {
        if (activity instanceof HomeActivity) {
            return R.id.nav_home;
        } else if (activity instanceof ScheduleListActivity) {
            return R.id.nav_schedule;
        } else if (activity instanceof FriendListActivity) {
            return R.id.nav_friends;
        } else if (activity instanceof RecommendationActivity) {
            return R.id.nav_recommendation;
        } else if (activity instanceof com.example.timemate.features.profile.ProfileActivity) {
            return R.id.nav_profile;
        } else {
            return R.id.nav_home; // 기본값
        }
    }
}
