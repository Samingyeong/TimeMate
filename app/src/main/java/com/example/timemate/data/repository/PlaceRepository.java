package com.example.timemate.data.repository;

import android.content.Context;
import android.util.Log;

// import com.example.timemate.network.api.KakaoPlaceService;
// import com.example.timemate.network.api.NaverPlaceService;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

/**
 * iOS 감성 전국 장소 검색 Repository
 * 30km 반경 + 100km 폴백 시스템
 */
public class PlaceRepository {
    
    private static final String TAG = "PlaceRepository";
    
    // 검색 반경 설정
    private static final int PRIMARY_RADIUS = 30000;   // 30km
    private static final int FALLBACK_RADIUS = 100000; // 100km (전국)
    
    private Context context;
    // private KakaoPlaceService kakaoPlaceService;
    // private NaverPlaceService naverPlaceService;
    
    public PlaceRepository(Context context) {
        this.context = context;
        // TODO: 서비스 초기화
    }
    
    /**
     * 전국 장소 검색 (30km → 100km 폴백)
     */
    public void search(String keyword, String category, double latitude, double longitude, 
                      PlaceSearchCallback callback) {
        try {
            Log.d(TAG, "🔍 전국 장소 검색 시작: " + keyword + " (카테고리: " + category + ")");
            
            // UTF-8 인코딩
            String encodedKeyword = URLEncoder.encode(keyword, "UTF-8");
            
            // 1차 검색: 30km 반경
            searchWithRadius(encodedKeyword, category, latitude, longitude, PRIMARY_RADIUS, 
                new PlaceSearchCallback() {
                    @Override
                    public void onSuccess(List<PlaceResult> results) {
                        if (results != null && !results.isEmpty()) {
                            Log.d(TAG, "✅ 1차 검색 성공: " + results.size() + "개 결과");
                            callback.onSuccess(results);
                        } else {
                            Log.d(TAG, "⚠️ 1차 검색 결과 없음, 2차 검색 시작 (100km)");
                            // 2차 검색: 100km 반경 (전국)
                            searchWithRadius(encodedKeyword, category, latitude, longitude, 
                                           FALLBACK_RADIUS, callback);
                        }
                    }
                    
                    @Override
                    public void onError(String error) {
                        Log.w(TAG, "1차 검색 실패, 2차 검색 시도: " + error);
                        // 1차 실패 시에도 2차 검색 시도
                        searchWithRadius(encodedKeyword, category, latitude, longitude, 
                                       FALLBACK_RADIUS, callback);
                    }
                });
                
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "UTF-8 인코딩 오류", e);
            callback.onError("검색어 인코딩 오류: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "장소 검색 오류", e);
            callback.onError("장소 검색 오류: " + e.getMessage());
        }
    }
    
    /**
     * 지정된 반경으로 장소 검색 (30km → 100km 폴백 포함)
     */
    private void searchWithRadius(String encodedKeyword, String category,
                                 double latitude, double longitude, int radius,
                                 PlaceSearchCallback callback) {
        try {
            Log.d(TAG, "📍 반경 " + (radius/1000) + "km 검색: " + encodedKeyword);

            // 1차 검색: 지정된 반경 내
            performRadiusSearch(encodedKeyword, category, latitude, longitude, radius, new PlaceSearchCallback() {
                @Override
                public void onSuccess(List<PlaceResult> results) {
                    if (!results.isEmpty()) {
                        Log.d(TAG, "✅ 1차 검색 성공: " + results.size() + "개 결과");
                        callback.onSuccess(results);
                    } else if (radius < 100000) {
                        // 2차 검색: 100km 전국 검색
                        Log.d(TAG, "🔄 1차 검색 결과 없음, 전국 검색 시작 (100km)");
                        performRadiusSearch(encodedKeyword, category, latitude, longitude, 100000, new PlaceSearchCallback() {
                            @Override
                            public void onSuccess(List<PlaceResult> fallbackResults) {
                                Log.d(TAG, "✅ 전국 검색 성공: " + fallbackResults.size() + "개 결과");
                                callback.onSuccess(fallbackResults);
                            }

                            @Override
                            public void onError(String error) {
                                Log.w(TAG, "전국 검색 실패, 샘플 데이터 제공: " + error);
                                createSampleResults(encodedKeyword, callback);
                            }
                        });
                    } else {
                        Log.d(TAG, "⚠️ 전국 검색도 결과 없음, 샘플 데이터 제공");
                        createSampleResults(encodedKeyword, callback);
                    }
                }

                @Override
                public void onError(String error) {
                    Log.w(TAG, "1차 검색 실패, 전국 검색 시도: " + error);
                    if (radius < 100000) {
                        // 1차 검색 실패 시에도 전국 검색 시도
                        performRadiusSearch(encodedKeyword, category, latitude, longitude, 100000, new PlaceSearchCallback() {
                            @Override
                            public void onSuccess(List<PlaceResult> fallbackResults) {
                                Log.d(TAG, "✅ 전국 검색 성공: " + fallbackResults.size() + "개 결과");
                                callback.onSuccess(fallbackResults);
                            }

                            @Override
                            public void onError(String fallbackError) {
                                Log.w(TAG, "전국 검색도 실패, 샘플 데이터 제공: " + fallbackError);
                                createSampleResults(encodedKeyword, callback);
                            }
                        });
                    } else {
                        createSampleResults(encodedKeyword, callback);
                    }
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "반경 검색 오류", e);
            callback.onError("반경 검색 오류: " + e.getMessage());
        }
    }

    /**
     * 실제 반경 검색 수행
     */
    private void performRadiusSearch(String encodedKeyword, String category,
                                   double latitude, double longitude, int radius,
                                   PlaceSearchCallback callback) {
        // API 서비스 구현 예정 - 현재는 샘플 데이터 사용
        // if (kakaoPlaceService != null) {
        //     searchWithKakao(encodedKeyword, category, latitude, longitude, radius, callback);
        // } else if (naverPlaceService != null) {
        //     searchWithNaver(encodedKeyword, category, latitude, longitude, radius, callback);
        // } else {
            // 샘플 데이터 반환
            createSampleResults(encodedKeyword, callback);
        // }
    }
    
    /**
     * 카카오 API 검색
     */
    private void searchWithKakao(String keyword, String category, 
                                double latitude, double longitude, int radius,
                                PlaceSearchCallback callback) {
        // TODO: 카카오 API 구현
        Log.d(TAG, "🟡 카카오 API 검색 (구현 예정)");
        createSampleResults(keyword, callback);
    }
    
    /**
     * 네이버 API 검색
     */
    private void searchWithNaver(String keyword, String category, 
                                double latitude, double longitude, int radius,
                                PlaceSearchCallback callback) {
        // TODO: 네이버 API 구현
        Log.d(TAG, "🟢 네이버 API 검색 (구현 예정)");
        createSampleResults(keyword, callback);
    }
    
    /**
     * 샘플 결과 생성 (API 구현 전 테스트용)
     */
    private void createSampleResults(String keyword, PlaceSearchCallback callback) {
        try {
            List<PlaceResult> sampleResults = new ArrayList<>();
            
            // 전국 주요 도시 샘플 데이터
            sampleResults.add(new PlaceResult("서울역", "서울특별시 중구 세종대로 2", 37.5547, 126.9706));
            sampleResults.add(new PlaceResult("부산역", "부산광역시 동구 중앙대로 206", 35.1158, 129.0403));
            sampleResults.add(new PlaceResult("대전역", "대전광역시 동구 중앙로 215", 36.3315, 127.4345));
            sampleResults.add(new PlaceResult("대구역", "대구광역시 북구 태평로 161", 35.8797, 128.6292));
            sampleResults.add(new PlaceResult("광주역", "광주광역시 북구 무등로 235", 35.1595, 126.9025));
            
            Log.d(TAG, "📋 샘플 결과 생성: " + sampleResults.size() + "개");
            callback.onSuccess(sampleResults);
            
        } catch (Exception e) {
            Log.e(TAG, "샘플 결과 생성 오류", e);
            callback.onError("샘플 데이터 생성 오류: " + e.getMessage());
        }
    }
    
    /**
     * 장소 검색 결과 모델
     */
    public static class PlaceResult {
        public String name;
        public String address;
        public double latitude;
        public double longitude;
        public String category;
        public String phone;
        public String url;
        
        public PlaceResult(String name, String address, double latitude, double longitude) {
            this.name = name;
            this.address = address;
            this.latitude = latitude;
            this.longitude = longitude;
        }
        
        @Override
        public String toString() {
            return name + " (" + address + ")";
        }
    }
    
    /**
     * 장소 검색 콜백 인터페이스
     */
    public interface PlaceSearchCallback {
        void onSuccess(List<PlaceResult> results);
        void onError(String error);
    }
}
