package com.example.timemate;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EnhancedDirectionsService {
    
    private static final String TAG = "EnhancedDirections";
    private static final String DRIVING_URL = "https://naveropenapi.apigw.ntruss.com/map-direction/v1/driving";
    private static final String TRANSIT_URL = "https://naveropenapi.apigw.ntruss.com/map-direction-15/v1/transit";
    private static final String WALKING_URL = "https://naveropenapi.apigw.ntruss.com/map-direction-15/v1/walking";
    
    private static final String CLIENT_ID = "dnnydofmgg";
    private static final String CLIENT_SECRET = "GlevAwH7wuE5x2zfXuzwL9KVcHJUq5p7P7zYSF45";
    
    private ExecutorService executor;
    
    public EnhancedDirectionsService() {
        executor = Executors.newSingleThreadExecutor();
    }
    
    public interface OptimalRouteCallback {
        void onSuccess(OptimalRoute route);
        void onError(String error);
    }
    
    public static class OptimalRoute {
        public String transportType;    // "driving", "transit", "walking"
        public int durationMinutes;     // 소요시간 (분)
        public String distance;         // 거리
        public String summary;          // 경로 요약
        public String tollFare;         // 통행료 (자동차만)
        public String fuelPrice;        // 연료비 (자동차만)
        public List<RouteStep> steps;   // 상세 경로
        
        public OptimalRoute(String transportType, int durationMinutes, String distance, String summary) {
            this.transportType = transportType;
            this.durationMinutes = durationMinutes;
            this.distance = distance;
            this.summary = summary;
            this.steps = new ArrayList<>();
        }
    }
    
    public static class RouteStep {
        public String instruction;      // 경로 안내
        public String distance;         // 구간 거리
        public int duration;           // 구간 소요시간
        public double lat, lng;        // 좌표
        
        public RouteStep(String instruction, String distance, int duration, double lat, double lng) {
            this.instruction = instruction;
            this.distance = distance;
            this.duration = duration;
            this.lat = lat;
            this.lng = lng;
        }
    }
    
    /**
     * 최적 경로 찾기 (자동차, 대중교통, 도보 중 가장 빠른 경로)
     */
    public void findOptimalRoute(String start, String goal, OptimalRouteCallback callback) {
        executor.execute(() -> {
            try {
                String startCoords = getCoordinatesFromAddress(start);
                String goalCoords = getCoordinatesFromAddress(goal);
                
                if (startCoords == null || goalCoords == null) {
                    callback.onError("주소를 좌표로 변환할 수 없습니다.");
                    return;
                }
                
                List<OptimalRoute> routes = new ArrayList<>();
                
                // 1. 자동차 경로 조회
                OptimalRoute drivingRoute = getDrivingRoute(startCoords, goalCoords);
                if (drivingRoute != null) routes.add(drivingRoute);
                
                // 2. 대중교통 경로 조회
                OptimalRoute transitRoute = getTransitRoute(startCoords, goalCoords);
                if (transitRoute != null) routes.add(transitRoute);
                
                // 3. 도보 경로 조회
                OptimalRoute walkingRoute = getWalkingRoute(startCoords, goalCoords);
                if (walkingRoute != null) routes.add(walkingRoute);
                
                // 가장 빠른 경로 선택
                OptimalRoute fastest = null;
                for (OptimalRoute route : routes) {
                    if (fastest == null || route.durationMinutes < fastest.durationMinutes) {
                        fastest = route;
                    }
                }
                
                if (fastest != null) {
                    callback.onSuccess(fastest);
                } else {
                    callback.onError("경로를 찾을 수 없습니다.");
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Exception in findOptimalRoute", e);
                callback.onError("경로 검색 중 오류 발생: " + e.getMessage());
            }
        });
    }
    
    private OptimalRoute getDrivingRoute(String start, String goal) {
        try {
            String urlString = DRIVING_URL + "?start=" + start + "&goal=" + goal + "&option=trafast";
            JSONObject response = makeApiCall(urlString);
            
            if (response != null && response.has("route")) {
                JSONObject route = response.getJSONObject("route");
                JSONArray trafast = route.getJSONArray("trafast");
                
                if (trafast.length() > 0) {
                    JSONObject routeInfo = trafast.getJSONObject(0);
                    JSONObject summary = routeInfo.getJSONObject("summary");
                    
                    int durationMs = summary.getInt("duration");
                    int durationMinutes = durationMs / (1000 * 60);
                    
                    int distanceM = summary.getInt("distance");
                    String distance = String.format("%.1f km", distanceM / 1000.0);
                    
                    String tollFare = summary.optString("tollFare", "0") + "원";
                    String fuelPrice = summary.optString("fuelPrice", "0") + "원";
                    
                    OptimalRoute optimalRoute = new OptimalRoute("driving", durationMinutes, distance, "자동차 경로");
                    optimalRoute.tollFare = tollFare;
                    optimalRoute.fuelPrice = fuelPrice;
                    
                    return optimalRoute;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting driving route", e);
        }
        return null;
    }
    
    private OptimalRoute getTransitRoute(String start, String goal) {
        try {
            String urlString = TRANSIT_URL + "?start=" + start + "&goal=" + goal;
            JSONObject response = makeApiCall(urlString);
            
            if (response != null && response.has("route")) {
                // 대중교통 응답 파싱 (실제 API 응답 구조에 따라 조정 필요)
                JSONObject route = response.getJSONObject("route");
                // 대중교통 경로 파싱 로직 구현
                
                return new OptimalRoute("transit", 45, "15.2 km", "대중교통 경로");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting transit route", e);
        }
        return null;
    }
    
    private OptimalRoute getWalkingRoute(String start, String goal) {
        try {
            String urlString = WALKING_URL + "?start=" + start + "&goal=" + goal;
            JSONObject response = makeApiCall(urlString);
            
            if (response != null && response.has("route")) {
                // 도보 경로 파싱 로직 구현
                return new OptimalRoute("walking", 120, "8.5 km", "도보 경로");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting walking route", e);
        }
        return null;
    }
    
    private JSONObject makeApiCall(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        
        connection.setRequestMethod("GET");
        connection.setRequestProperty("X-NCP-APIGW-API-KEY-ID", CLIENT_ID);
        connection.setRequestProperty("X-NCP-APIGW-API-KEY", CLIENT_SECRET);
        connection.setRequestProperty("Content-Type", "application/json");
        
        int responseCode = connection.getResponseCode();
        
        if (responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            connection.disconnect();
            
            return new JSONObject(response.toString());
        } else {
            Log.e(TAG, "API Error: " + responseCode);
            connection.disconnect();
            return null;
        }
    }
    
    private String getCoordinatesFromAddress(String address) {
        // 실제로는 Geocoding API를 사용해야 하지만, 여기서는 더미 좌표 사용
        if (address.contains("서울") || address.contains("강남")) {
            return "127.0276,37.4979"; // 강남역
        } else if (address.contains("부산") || address.contains("해운대")) {
            return "129.1603,35.1595"; // 해운대
        } else if (address.contains("대전")) {
            return "127.3845,36.3504"; // 대전역
        } else {
            return "126.9780,37.5665"; // 서울시청
        }
    }
    
    public void shutdown() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
}
