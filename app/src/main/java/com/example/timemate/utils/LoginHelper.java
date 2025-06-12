package com.example.timemate.utils;

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
                final String userId = String.valueOf(kakaoUser.getId());
                final String nickname = kakaoUser.getKakaoAccount().getProfile().getNickname();
                final String profileUrl = kakaoUser.getKakaoAccount().getProfile().getProfileImageUrl();

                // 이메일 정보 가져오기 (있는 경우에만)
                final String email;
                if (kakaoUser.getKakaoAccount() != null &&
                    kakaoUser.getKakaoAccount().getEmail() != null) {
                    email = kakaoUser.getKakaoAccount().getEmail();
                    Log.d("KAKAO_USER", "카카오 이메일: " + email);
                } else {
                    email = null;
                    Log.d("KAKAO_USER", "카카오 이메일 정보 없음");
                }

                com.example.timemate.data.model.User newUser = new com.example.timemate.data.model.User();
                newUser.userId = userId;
                newUser.nickname = nickname;
                newUser.email = email;
                newUser.profileImage = profileUrl;

                // UserSession에 로그인 정보 저장
                com.example.timemate.util.UserSession userSession =
                    com.example.timemate.util.UserSession.getInstance(context);
                userSession.login(userId, nickname, email);

                Log.d("KAKAO_USER", "로그인 정보 저장 완료 - ID: " + userId + ", 닉네임: " + nickname + ", 이메일: " + email);

                Executors.newSingleThreadExecutor().execute(() -> {
                    try {
                        // 기존 사용자가 있는지 확인
                        com.example.timemate.data.model.User existingUser = db.userDao().getUserById(userId);
                        if (existingUser != null) {
                            // 기존 사용자 정보 업데이트
                            existingUser.nickname = nickname;
                            existingUser.email = email;
                            existingUser.profileImage = profileUrl;
                            existingUser.updateLastLogin();
                            db.userDao().update(existingUser);
                            Log.d("KAKAO_USER", "기존 사용자 정보 업데이트 완료");
                        } else {
                            // 새 사용자 생성
                            db.userDao().insert(newUser);
                            Log.d("KAKAO_USER", "새 사용자 생성 완료");
                        }
                    } catch (Exception e) {
                        Log.e("KAKAO_USER", "사용자 정보 저장 오류", e);
                    }
                });

                // ✅ 로그인 성공 시 홈 화면으로 이동 (수정된 경로)
                Intent intent = new Intent(context, HomeActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                context.startActivity(intent);
            }

            return null;
        });
    }
}
