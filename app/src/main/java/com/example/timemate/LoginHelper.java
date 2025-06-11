package com.example.timemate;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.timemate.data.database.AppDatabase;
import com.example.timemate.features.home.HomeActivity;
import com.kakao.sdk.user.UserApiClient;

import java.util.concurrent.Executors;

public class LoginHelper {
    public static void handleKakaoLogin(AppDatabase db, Context context) {
        UserApiClient.getInstance().loginWithKakaoTalk(context, (token, error) -> {
            if (error != null) {
                Log.e("KAKAO_LOGIN", "카카오톡 로그인 실패: " + error.toString());
                loginWithAccount(db, context);
            } else if (token != null) {
                fetchAndStoreUserInfo(db, context);
            }
            return null; // ✅ 반드시 추가
        });
    }

    private static void loginWithAccount(AppDatabase db, Context context) {
        UserApiClient.getInstance().loginWithKakaoAccount(context, (token, error) -> {
            if (error != null) {
                Log.e("KAKAO_LOGIN", "카카오계정 로그인 실패: " + error.toString());
            }

            if (token != null) {
                fetchAndStoreUserInfo(db, context);
            }
            return null;});
    }

    private static void fetchAndStoreUserInfo(AppDatabase db, Context context) {
        UserApiClient.getInstance().me((com.kakao.sdk.user.model.User kakaoUser, Throwable meError) -> {
            if (meError != null) {
                Log.e("KAKAO_USER", "사용자 정보 가져오기 실패: " + meError.toString());
                return null;
            }

            if (kakaoUser != null) {
                String userId = String.valueOf(kakaoUser.getId());
                String nickname = kakaoUser.getKakaoAccount().getProfile().getNickname();
                String profileUrl = kakaoUser.getKakaoAccount().getProfile().getProfileImageUrl();

                com.example.timemate.data.model.User newUser = new com.example.timemate.data.model.User();
                newUser.userId = userId;
                newUser.nickname = nickname;
                newUser.profileImage = profileUrl;

                Executors.newSingleThreadExecutor().execute(() -> {
                    db.userDao().insert(newUser);
                });

                // ✅ 로그인 성공 시 홈 화면으로 이동
                Intent intent = new Intent(context, com.example.timemate.ui.home.HomeActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                context.startActivity(intent);
            }

            return null;
        });
    }
}
