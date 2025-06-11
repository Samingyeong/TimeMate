package com.example.timemate.network.api;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.example.timemate.config.ApiConfig;

/**
 * 실제 거리 계산 서비스
 * 카카오 및 네이버 API를 활용한 정확한 거리 및 소요시간 계산
 */
public class DistanceCalculationService {
    
    private static final String TAG = "DistanceCalculation";
    
    // 네이버 클라우드 플랫폼 API 설정
    private static final String NAVER_CLIENT_ID = ApiConfig.NAVER_CLOUD_CLIENT_ID;
    private static final String NAVER_CLIENT_SECRET = ApiConfig.NAVER_CLOUD_CLIENT_SECRET;
    private static final String NAVER_DIRECTIONS_URL = ApiConfig.NAVER_DIRECTIONS_URL;
    
    // 카카오 API 설정
    private static final String KAKAO_REST_API_KEY = ApiConfig.KAKAO_REST_API_KEY;
    private static final String KAKAO_DIRECTIONS_URL = "https://apis-navi.kakaomobility.com/v1/directions";
    
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    /**
     * 거리 및 소요시간 정보 모델
     */
    public static class DistanceInfo {
        public double distanceKm;           // 실제 경로 거리 (km)
        public int durationMinutes;         // 소요시간 (분)
        public double straightDistanceKm;   // 직선거리 (km)
        public String transportMode;        // 교통수단 (driving, walking, transit)
        public String description;          // 경로 설명
        public boolean isRealRoute;         // 실제 API 경로 여부
        
        public DistanceInfo() {
            this.distanceKm = 0.0;
            this.durationMinutes = 0;
            this.straightDistanceKm = 0.0;
            this.transportMode = "driving";
            this.description = "";
            this.isRealRoute = false;
        }
        
        public DistanceInfo(double distanceKm, int durationMinutes, String transportMode) {
            this.distanceKm = distanceKm;
            this.durationMinutes = durationMinutes;
            this.transportMode = transportMode;
            this.isRealRoute = true;
            this.description = "실제 경로";
        }
    }

    /**
     * 거리 계산 콜백 인터페이스
     */
    public interface DistanceCallback {
        void onSuccess(DistanceInfo distanceInfo);
        void onError(String error);
    }

    /**
     * 실제 거리 계산 (네이버 Directions API 우선 사용)
     */
    public void calculateRealDistance(double startLat, double startLng, 
                                    double goalLat, double goalLng,
                                    String transportMode, DistanceCallback callback) {
        executor.execute(() -> {
            try {
                Log.d(TAG, "실제 거리 계산 시작: " + transportMode + 
                          " (" + startLat + ", " + startLng + ") → (" + goalLat + ", " + goalLng + ")");
                
                // 1. 네이버 Directions API 시도
                DistanceInfo naverResult = getNaverDistance(startLat, startLng, goalLat, goalLng, transportMode);
                if (naverResult != null && naverResult.isRealRoute) {
                    Log.d(TAG, "네이버 API 성공: " + naverResult.distanceKm + "km, " + naverResult.durationMinutes + "분");
                    callback.onSuccess(naverResult);
                    return;
                }
                
                // 2. 네이버 실패 시 카카오 API 시도 (자동차만)
                if ("driving".equals(transportMode)) {
                    DistanceInfo kakaoResult = getKakaoDistance(startLat, startLng, goalLat, goalLng);
                    if (kakaoResult != null && kakaoResult.isRealRoute) {
                        Log.d(TAG, "카카오 API 성공: " + kakaoResult.distanceKm + "km, " + kakaoResult.durationMinutes + "분");
                        callback.onSuccess(kakaoResult);
                        return;
                    }
                }
                
                // 3. 모든 API 실패 시 직선거리 기반 추정
                DistanceInfo fallbackResult = calculateStraightLineDistance(startLat, startLng, goalLat, goalLng, transportMode);
                Log.d(TAG, "직선거리 기반 추정: " + fallbackResult.distanceKm + "km, " + fallbackResult.durationMinutes + "분");
                callback.onSuccess(fallbackResult);
                
            } catch (Exception e) {
                Log.e(TAG, "거리 계산 오류", e);
                callback.onError("거리 계산 중 오류가 발생했습니다: " + e.getMessage());
            }
        });
    }

    /**
     * 네이버 Directions API를 사용한 거리 계산
     */
    private DistanceInfo getNaverDistance(double startLat, double startLng, 
                                        double goalLat, double goalLng, String transportMode) {
        try {
            String apiPath = getNaverApiPath(transportMode);
            if (apiPath == null) {
                return null;
            }
            
            String startCoords = startLng + "," + startLat;
            String goalCoords = goalLng + "," + goalLat;
            
            String urlString = NAVER_DIRECTIONS_URL + apiPath +
                "?start=" + startCoords +
                "&goal=" + goalCoords +
                "&option=trafast";
            
            Log.d(TAG, "네이버 API 호출: " + urlString);
            
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            
            connection.setRequestProperty("X-NCP-APIGW-API-KEY-ID", NAVER_CLIENT_ID);
            connection.setRequestProperty("X-NCP-APIGW-API-KEY", NAVER_CLIENT_SECRET);
            connection.setRequestProperty("Accept-Language", "ko");
            
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            
            int responseCode = connection.getResponseCode();
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), "UTF-8"));
                StringBuilder response = new StringBuilder();
                String line;
                
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                
                return parseNaverResponse(response.toString(), transportMode);
            } else {
                Log.w(TAG, "네이버 API 오류: " + responseCode);
                return null;
            }
            
        } catch (Exception e) {
            Log.e(TAG, "네이버 API 호출 오류", e);
            return null;
        }
    }

    /**
     * 카카오 Directions API를 사용한 거리 계산 (자동차만)
     */
    private DistanceInfo getKakaoDistance(double startLat, double startLng, 
                                        double goalLat, double goalLng) {
        try {
            String urlString = KAKAO_DIRECTIONS_URL +
                "?origin=" + startLng + "," + startLat +
                "&destination=" + goalLng + "," + goalLat +
                "&priority=RECOMMEND";
            
            Log.d(TAG, "카카오 API 호출: " + urlString);
            
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            
            connection.setRequestProperty("Authorization", "KakaoAK " + KAKAO_REST_API_KEY);
            connection.setRequestProperty("Content-Type", "application/json");
            
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            
            int responseCode = connection.getResponseCode();
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), "UTF-8"));
                StringBuilder response = new StringBuilder();
                String line;
                
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                
                return parseKakaoResponse(response.toString());
            } else {
                Log.w(TAG, "카카오 API 오류: " + responseCode);
                return null;
            }
            
        } catch (Exception e) {
            Log.e(TAG, "카카오 API 호출 오류", e);
            return null;
        }
    }

    /**
     * 네이버 API 경로 결정
     */
    private String getNaverApiPath(String transportMode) {
        switch (transportMode) {
            case "driving":
                return "/driving";
            case "walking":
                return "/walking";
            case "transit":
                return "/transit";
            default:
                return "/driving";
        }
    }

    /**
     * 네이버 API 응답 파싱
     */
    private DistanceInfo parseNaverResponse(String response, String transportMode) {
        try {
            JSONObject jsonObject = new JSONObject(response);
            
            if (jsonObject.optInt("code", -1) != 0) {
                return null;
            }
            
            JSONObject route = jsonObject.optJSONObject("route");
            if (route == null) {
                return null;
            }
            
            JSONArray routeArray = route.optJSONArray("trafast");
            if (routeArray == null || routeArray.length() == 0) {
                return null;
            }
            
            JSONObject routeInfo = routeArray.getJSONObject(0);
            JSONObject summary = routeInfo.getJSONObject("summary");
            
            // 거리, 시간 정보 추출
            int distanceInMeters = summary.getInt("distance");
            double distanceKm = distanceInMeters / 1000.0;
            
            int durationInMs = summary.getInt("duration");
            int durationMinutes = durationInMs / (1000 * 60);
            
            DistanceInfo info = new DistanceInfo(distanceKm, durationMinutes, transportMode);
            info.description = "네이버 실제 경로";
            
            return info;
            
        } catch (Exception e) {
            Log.e(TAG, "네이버 응답 파싱 오류", e);
            return null;
        }
    }

    /**
     * 카카오 API 응답 파싱
     */
    private DistanceInfo parseKakaoResponse(String response) {
        try {
            JSONObject jsonObject = new JSONObject(response);
            
            JSONArray routes = jsonObject.optJSONArray("routes");
            if (routes == null || routes.length() == 0) {
                return null;
            }
            
            JSONObject route = routes.getJSONObject(0);
            JSONObject summary = route.getJSONObject("summary");
            
            // 거리, 시간 정보 추출
            int distanceInMeters = summary.getInt("distance");
            double distanceKm = distanceInMeters / 1000.0;
            
            int durationInSeconds = summary.getInt("duration");
            int durationMinutes = durationInSeconds / 60;
            
            DistanceInfo info = new DistanceInfo(distanceKm, durationMinutes, "driving");
            info.description = "카카오 실제 경로";
            
            return info;
            
        } catch (Exception e) {
            Log.e(TAG, "카카오 응답 파싱 오류", e);
            return null;
        }
    }

    /**
     * 직선거리 기반 추정 계산
     */
    private DistanceInfo calculateStraightLineDistance(double startLat, double startLng, 
                                                     double goalLat, double goalLng, String transportMode) {
        // Haversine 공식으로 직선거리 계산
        final int R = 6371; // 지구 반지름 (km)
        
        double latDistance = Math.toRadians(goalLat - startLat);
        double lngDistance = Math.toRadians(goalLng - startLng);
        
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(startLat)) * Math.cos(Math.toRadians(goalLat))
                * Math.sin(lngDistance / 2) * Math.sin(lngDistance / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double straightDistance = R * c;
        
        // 교통수단별 실제 거리 추정 (직선거리 대비 비율)
        double realDistanceMultiplier = getRealDistanceMultiplier(transportMode);
        double estimatedDistance = straightDistance * realDistanceMultiplier;
        
        // 교통수단별 속도로 소요시간 계산
        double averageSpeed = getAverageSpeed(transportMode);
        int estimatedDuration = (int) Math.ceil((estimatedDistance / averageSpeed) * 60);
        
        DistanceInfo info = new DistanceInfo();
        info.distanceKm = estimatedDistance;
        info.durationMinutes = estimatedDuration;
        info.straightDistanceKm = straightDistance;
        info.transportMode = transportMode;
        info.description = "직선거리 기반 추정";
        info.isRealRoute = false;
        
        return info;
    }

    /**
     * 교통수단별 실제 거리 배수 (직선거리 대비)
     */
    private double getRealDistanceMultiplier(String transportMode) {
        switch (transportMode) {
            case "walking":
                return 1.3; // 도보는 직선거리의 1.3배
            case "driving":
                return 1.4; // 자동차는 직선거리의 1.4배
            case "transit":
                return 1.5; // 대중교통은 직선거리의 1.5배
            default:
                return 1.4;
        }
    }

    /**
     * 교통수단별 평균 속도 (km/h)
     */
    private double getAverageSpeed(String transportMode) {
        switch (transportMode) {
            case "walking":
                return 4.0; // 도보 4km/h
            case "driving":
                return 30.0; // 자동차 30km/h (도심 기준)
            case "transit":
                return 25.0; // 대중교통 25km/h
            default:
                return 30.0;
        }
    }

    /**
     * 서비스 종료
     */
    public void shutdown() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
}
