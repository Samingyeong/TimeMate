package com.example.timemate;

import android.app.Application;
import android.util.Log;

import com.example.timemate.network.api.NaverApiService;

/**
 * TimeMate 애플리케이션 클래스
 * 앱 전체 생명주기 관리 및 리소스 정리
 */
public class TimeMateApplication extends Application {
    
    private static final String TAG = "TimeMateApplication";
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "TimeMate Application 시작");

        // 앱 데이터 디렉토리 안전성 확인
        try {
            java.io.File filesDir = getFilesDir();
            java.io.File cacheDir = getCacheDir();

            Log.d(TAG, "Files Dir: " + (filesDir != null ? filesDir.getAbsolutePath() : "null"));
            Log.d(TAG, "Cache Dir: " + (cacheDir != null ? cacheDir.getAbsolutePath() : "null"));

            // 필요한 디렉토리 생성
            if (filesDir != null && !filesDir.exists()) {
                boolean created = filesDir.mkdirs();
                Log.d(TAG, "Files 디렉토리 생성: " + created);
            }

        } catch (Exception e) {
            Log.e(TAG, "디렉토리 확인 중 오류", e);
        }

        // 앱 전역 예외 처리기 설정
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable throwable) {
                Log.e(TAG, "앱 전역 예외 발생", throwable);

                // 크래시 로그 기록
                android.util.Log.e(TAG, "Uncaught exception in thread " + thread.getName(), throwable);

                // 시스템 기본 처리기 호출
                System.exit(1);
            }
        });
    }
    
    @Override
    public void onTerminate() {
        super.onTerminate();
        Log.d(TAG, "TimeMate Application 종료 - 리소스 정리 시작");
        
        // 네트워크 서비스 정리
        try {
            NaverApiService.destroyInstance();
            Log.d(TAG, "네트워크 서비스 정리 완료");
        } catch (Exception e) {
            Log.e(TAG, "네트워크 서비스 정리 중 오류", e);
        }
    }
    
    @Override
    public void onLowMemory() {
        super.onLowMemory();
        Log.w(TAG, "메모리 부족 - 가비지 컬렉션 실행");
        System.gc();
    }
    
    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        Log.w(TAG, "메모리 정리 요청 - 레벨: " + level);
        
        if (level >= TRIM_MEMORY_MODERATE) {
            // 메모리 부족 시 네트워크 연결 정리
            try {
                NaverApiService naverService = NaverApiService.getInstance();
                if (naverService != null) {
                    naverService.shutdown();
                }
            } catch (Exception e) {
                Log.e(TAG, "메모리 정리 중 네트워크 서비스 종료 오류", e);
            }
        }
    }
}
