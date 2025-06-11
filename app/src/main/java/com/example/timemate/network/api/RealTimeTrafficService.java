package com.example.timemate.network.api;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.example.timemate.config.ApiConfig;

/**
 * 실시간 교통 정보 서비스
 * 네이버 교통 API와 시간대별 패턴 분석을 통한 실시간 교통 상황 제공
 */
public class RealTimeTrafficService {
    
    private static final String TAG = "RealTimeTraffic";
    
    // 네이버 클라우드 플랫폼 API 설정
    private static final String CLIENT_ID = ApiConfig.NAVER_CLOUD_CLIENT_ID;
    private static final String CLIENT_SECRET = ApiConfig.NAVER_CLOUD_CLIENT_SECRET;
    private static final String DIRECTIONS_URL = ApiConfig.NAVER_DIRECTIONS_URL;
    
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private WeatherService weatherService;

    public RealTimeTrafficService() {
        weatherService = new WeatherService();
    }

    /**
     * 교통 상황 데이터 모델
     */
    public static class TrafficCondition {
        public double trafficMultiplier;    // 교통 지연 배수 (1.0 = 정상, 1.5 = 50% 지연)
        public String trafficLevel;         // 교통 상황 레벨 (원활, 보통, 혼잡, 매우혼잡)
        public String description;          // 상황 설명
        public boolean isRushHour;          // 출퇴근 시간 여부
        public boolean isBadWeather;        // 악천후 여부
        public String weatherImpact;        // 날씨 영향 설명
        
        public TrafficCondition() {
            this.trafficMultiplier = 1.0;
            this.trafficLevel = "원활";
            this.description = "정상 교통 상황";
            this.isRushHour = false;
            this.isBadWeather = false;
            this.weatherImpact = "";
        }
    }

    /**
     * 교통 정보 콜백 인터페이스
     */
    public interface TrafficCallback {
        void onSuccess(TrafficCondition condition);
        void onError(String error);
    }

    /**
     * 실시간 교통 상황 분석
     */
    public void getRealTimeTrafficCondition(double startLat, double startLng, 
                                          double goalLat, double goalLng,
                                          String transportMode, TrafficCallback callback) {
        executor.execute(() -> {
            try {
                TrafficCondition condition = new TrafficCondition();
                
                // 1. 시간대별 교통 패턴 분석
                analyzeTimeBasedTraffic(condition);
                
                // 2. 날씨 영향 분석
                analyzeWeatherImpact(startLat, startLng, transportMode, condition, callback);
                
            } catch (Exception e) {
                Log.e(TAG, "Traffic analysis error", e);
                callback.onError("교통 정보 분석 오류: " + e.getMessage());
            }
        });
    }

    /**
     * 시간대별 교통 패턴 분석
     */
    private void analyzeTimeBasedTraffic(TrafficCondition condition) {
        Calendar cal = Calendar.getInstance();
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
        
        // 주말 vs 평일
        boolean isWeekend = (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY);
        
        if (isWeekend) {
            // 주말 패턴
            if (hour >= 10 && hour <= 14) {
                // 주말 오후 (쇼핑, 나들이)
                condition.trafficMultiplier = 1.2;
                condition.trafficLevel = "보통";
                condition.description = "주말 오후 • 쇼핑/나들이 교통량 증가";
            } else if (hour >= 18 && hour <= 21) {
                // 주말 저녁 (외식, 귀가)
                condition.trafficMultiplier = 1.3;
                condition.trafficLevel = "혼잡";
                condition.description = "주말 저녁 • 외식/귀가 교통량 증가";
            } else {
                condition.trafficMultiplier = 1.0;
                condition.trafficLevel = "원활";
                condition.description = "주말 • 원활한 교통 상황";
            }
        } else {
            // 평일 패턴
            if ((hour >= 7 && hour <= 9) || (hour >= 18 && hour <= 20)) {
                // 출퇴근 시간
                condition.isRushHour = true;
                condition.trafficMultiplier = 1.5;
                condition.trafficLevel = "매우혼잡";
                condition.description = "출퇴근 시간 • 심각한 교통체증 예상";
            } else if ((hour >= 6 && hour < 7) || (hour > 9 && hour < 11) || 
                      (hour > 16 && hour < 18) || (hour > 20 && hour <= 22)) {
                // 준 혼잡 시간
                condition.trafficMultiplier = 1.2;
                condition.trafficLevel = "혼잡";
                condition.description = "평일 혼잡 시간 • 교통량 증가";
            } else if (hour >= 12 && hour <= 14) {
                // 점심시간
                condition.trafficMultiplier = 1.1;
                condition.trafficLevel = "보통";
                condition.description = "점심시간 • 약간의 교통량 증가";
            } else {
                condition.trafficMultiplier = 1.0;
                condition.trafficLevel = "원활";
                condition.description = "평일 • 원활한 교통 상황";
            }
        }
        
        Log.d(TAG, String.format("시간대 분석: %s, 배수: %.1f", 
              condition.description, condition.trafficMultiplier));
    }

    /**
     * 날씨 영향 분석
     */
    private void analyzeWeatherImpact(double lat, double lng, String transportMode, 
                                    TrafficCondition condition, TrafficCallback callback) {
        
        weatherService.getCurrentWeather(lat, lng, new WeatherService.WeatherCallback() {
            @Override
            public void onSuccess(WeatherService.WeatherData weather) {
                try {
                    // 날씨 조건별 영향 분석
                    String weatherMain = weather.main.toLowerCase();
                    double windSpeed = weather.windSpeed;
                    
                    if (weatherMain.contains("rain") || weatherMain.contains("drizzle")) {
                        // 비/이슬비
                        condition.isBadWeather = true;
                        condition.trafficMultiplier *= 1.3;
                        condition.weatherImpact = "비로 인한 교통 지연";
                        
                        if ("walking".equals(transportMode) || "bicycle".equals(transportMode)) {
                            condition.trafficMultiplier *= 1.2; // 도보/자전거 추가 지연
                            condition.weatherImpact += " • 도보/자전거 이용 주의";
                        }
                        
                    } else if (weatherMain.contains("snow")) {
                        // 눈
                        condition.isBadWeather = true;
                        condition.trafficMultiplier *= 1.6;
                        condition.weatherImpact = "눈으로 인한 심각한 교통 지연";
                        
                        if ("walking".equals(transportMode) || "bicycle".equals(transportMode)) {
                            condition.trafficMultiplier *= 1.5;
                            condition.weatherImpact += " • 도보/자전거 매우 위험";
                        }
                        
                    } else if (weatherMain.contains("fog") || weatherMain.contains("mist")) {
                        // 안개/박무
                        condition.isBadWeather = true;
                        condition.trafficMultiplier *= 1.2;
                        condition.weatherImpact = "안개로 인한 시야 제한";
                        
                    } else if (windSpeed > 10.0) {
                        // 강풍 (10m/s 이상)
                        condition.isBadWeather = true;
                        condition.trafficMultiplier *= 1.1;
                        condition.weatherImpact = "강풍 주의";
                        
                        if ("bicycle".equals(transportMode)) {
                            condition.trafficMultiplier *= 1.3;
                            condition.weatherImpact += " • 자전거 이용 위험";
                        }
                        
                    } else if (weather.temperature < -5.0) {
                        // 혹한 (-5도 이하)
                        condition.trafficMultiplier *= 1.1;
                        condition.weatherImpact = "혹한으로 인한 교통 영향";
                        
                        if ("walking".equals(transportMode) || "bicycle".equals(transportMode)) {
                            condition.trafficMultiplier *= 1.2;
                            condition.weatherImpact += " • 도보/자전거 한파 주의";
                        }
                        
                    } else if (weather.temperature > 35.0) {
                        // 폭염 (35도 이상)
                        condition.weatherImpact = "폭염 주의";
                        
                        if ("walking".equals(transportMode) || "bicycle".equals(transportMode)) {
                            condition.trafficMultiplier *= 1.1;
                            condition.weatherImpact += " • 도보/자전거 폭염 주의";
                        }
                    }
                    
                    // 최종 교통 레벨 재계산
                    updateTrafficLevel(condition);
                    
                    Log.d(TAG, String.format("최종 교통 분석: %s (배수: %.1f, 날씨: %s)", 
                          condition.trafficLevel, condition.trafficMultiplier, condition.weatherImpact));
                    
                    callback.onSuccess(condition);
                    
                } catch (Exception e) {
                    Log.e(TAG, "Weather impact analysis error", e);
                    // 날씨 분석 실패해도 기본 교통 정보는 제공
                    callback.onSuccess(condition);
                }
            }

            @Override
            public void onError(String error) {
                Log.w(TAG, "Weather API failed, using time-based traffic only: " + error);
                // 날씨 API 실패해도 시간대 기반 교통 정보는 제공
                callback.onSuccess(condition);
            }
        });
    }

    /**
     * 교통 배수에 따른 레벨 업데이트
     */
    private void updateTrafficLevel(TrafficCondition condition) {
        if (condition.trafficMultiplier >= 1.8) {
            condition.trafficLevel = "매우혼잡";
        } else if (condition.trafficMultiplier >= 1.4) {
            condition.trafficLevel = "혼잡";
        } else if (condition.trafficMultiplier >= 1.2) {
            condition.trafficLevel = "보통";
        } else {
            condition.trafficLevel = "원활";
        }
        
        // 설명 업데이트
        if (condition.isBadWeather && !condition.weatherImpact.isEmpty()) {
            condition.description += " • " + condition.weatherImpact;
        }
    }

    /**
     * 서비스 종료
     */
    public void shutdown() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
        if (weatherService != null) {
            weatherService.shutdown();
        }
    }
}
