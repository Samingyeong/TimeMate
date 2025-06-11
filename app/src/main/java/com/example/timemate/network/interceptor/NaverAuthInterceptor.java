package com.example.timemate.network.interceptor;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import java.io.IOException;

/**
 * 네이버 API 인증 인터셉터
 * 네이버 개발자센터와 클라우드 플랫폼 API의 인증 헤더 자동 추가
 */
public class NaverAuthInterceptor implements Interceptor {

    public enum AuthType {
        DEVELOPER_CENTER,   // 네이버 개발자센터 API
        CLOUD_PLATFORM     // 네이버 클라우드 플랫폼 API
    }

    private final String clientId;
    private final String clientSecret;
    private final AuthType authType;

    public NaverAuthInterceptor(String clientId, String clientSecret, AuthType authType) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.authType = authType;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request originalRequest = chain.request();
        
        Request.Builder requestBuilder = originalRequest.newBuilder();

        // 인증 타입에 따른 헤더 추가
        switch (authType) {
            case DEVELOPER_CENTER:
                // 네이버 개발자센터 API 인증
                requestBuilder
                    .addHeader("X-Naver-Client-Id", clientId)
                    .addHeader("X-Naver-Client-Secret", clientSecret)
                    .addHeader("Accept-Language", "ko");
                break;

            case CLOUD_PLATFORM:
                // 네이버 클라우드 플랫폼 API 인증
                requestBuilder
                    .addHeader("X-NCP-APIGW-API-KEY-ID", clientId)
                    .addHeader("X-NCP-APIGW-API-KEY", clientSecret)
                    .addHeader("Accept-Language", "ko");
                break;
        }

        // 공통 헤더 추가
        requestBuilder
            .addHeader("User-Agent", "TimeMate/1.0")
            .addHeader("Accept", "application/json")
            .addHeader("Content-Type", "application/json; charset=utf-8");

        Request newRequest = requestBuilder.build();
        
        // 요청 로깅
        android.util.Log.d("NaverAuthInterceptor", 
            "Request: " + newRequest.method() + " " + newRequest.url());
        android.util.Log.d("NaverAuthInterceptor", 
            "Auth Type: " + authType + ", Client ID: " + maskClientId(clientId));

        return chain.proceed(newRequest);
    }

    /**
     * 클라이언트 ID 마스킹 (보안을 위해 일부만 표시)
     */
    private String maskClientId(String clientId) {
        if (clientId == null || clientId.length() < 8) {
            return "****";
        }
        return clientId.substring(0, 4) + "****" + clientId.substring(clientId.length() - 4);
    }
}
