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
import java.net.URLEncoder;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NaverDirectionsService {
    
    private static final String TAG = "NaverDirections";
    private static final String BASE_URL = "https://naveropenapi.apigw.ntruss.com/map-direction/v1/driving";
    
    // 네이버 클라우드 플랫폼에서 발급받은 API 키
    private static final String CLIENT_ID = "dnnydofmgg";
    private static final String CLIENT_SECRET = "GlevAwH7wuE5x2zfXuzwL9KVcHJUq5p7P7zYSF45";
    
    private ExecutorService executor;
    
    public NaverDirectionsService() {
        executor = Executors.newSingleThreadExecutor();
    }
    
    public interface DirectionsCallback {
        void onSuccess(DirectionsResult result);
        void onError(String error);
    }
    
    public static class DirectionsResult {
        public String distance;      // 거리 (km)
        public String duration;      // 소요시간 (분)
        public String tollFare;      // 통행료
        public String fuelPrice;     // 연료비
        public String summary;       // 경로 요약
        
        public DirectionsResult(String distance, String duration, String tollFare, String fuelPrice, String summary) {
            this.distance = distance;
            this.duration = duration;
            this.tollFare = tollFare;
            this.fuelPrice = fuelPrice;
            this.summary = summary;
        }
    }
    
    /**
     * 두 지점 간의 경로 정보를 조회합니다.
     * @param start 출발지 (예: "서울특별시 강남구 테헤란로 152")
     * @param goal 도착지 (예: "부산광역시 해운대구 해운대해변로 264")
     * @param callback 결과 콜백
     */
    public void getDirections(String start, String goal, DirectionsCallback callback) {
        executor.execute(() -> {
            try {
                // 주소를 좌표로 변환 (실제로는 Geocoding API를 사용해야 함)
                // 여기서는 간단히 더미 좌표 사용
                String startCoords = getCoordinatesFromAddress(start);
                String goalCoords = getCoordinatesFromAddress(goal);
                
                if (startCoords == null || goalCoords == null) {
                    callback.onError("주소를 좌표로 변환할 수 없습니다.");
                    return;
                }
                
                String urlString = BASE_URL + 
                    "?start=" + startCoords + 
                    "&goal=" + goalCoords +
                    "&option=trafast"; // 실시간 빠른길
                
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                
                // 헤더 설정
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
                    
                    DirectionsResult result = parseDirectionsResponse(response.toString(), start, goal);
                    callback.onSuccess(result);
                    
                } else {
                    BufferedReader errorReader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    StringBuilder errorResponse = new StringBuilder();
                    String line;
                    
                    while ((line = errorReader.readLine()) != null) {
                        errorResponse.append(line);
                    }
                    errorReader.close();
                    
                    Log.e(TAG, "API Error: " + errorResponse.toString());
                    callback.onError("경로 조회 실패: " + responseCode);
                }
                
                connection.disconnect();
                
            } catch (Exception e) {
                Log.e(TAG, "Exception in getDirections", e);
                callback.onError("네트워크 오류: " + e.getMessage());
            }
        });
    }
    
    /**
     * 주소를 좌표로 변환 (더미 구현)
     * 실제로는 네이버 Geocoding API를 사용해야 합니다.
     */
    private String getCoordinatesFromAddress(String address) {
        // 더미 좌표 반환 (실제 구현에서는 Geocoding API 사용)
        if (address.contains("서울") || address.contains("강남")) {
            return "127.0276,37.4979"; // 강남역 좌표
        } else if (address.contains("부산") || address.contains("해운대")) {
            return "129.1603,35.1595"; // 해운대 좌표
        } else if (address.contains("대전")) {
            return "127.3845,36.3504"; // 대전역 좌표
        } else {
            // 기본값: 서울시청
            return "126.9780,37.5665";
        }
    }
    
    /**
     * API 응답을 파싱하여 DirectionsResult 객체로 변환
     */
    private DirectionsResult parseDirectionsResponse(String jsonResponse, String startAddress, String goalAddress) throws JSONException {
        JSONObject json = new JSONObject(jsonResponse);

        if (json.has("route")) {
            JSONObject route = json.getJSONObject("route");
            JSONArray trafast = route.getJSONArray("trafast");

            if (trafast.length() > 0) {
                JSONObject routeInfo = trafast.getJSONObject(0);
                JSONObject summary = routeInfo.getJSONObject("summary");

                // 거리 (미터 -> 킬로미터)
                int distanceInMeters = summary.getInt("distance");
                String distance = String.format("%.1f km", distanceInMeters / 1000.0);

                // 소요시간 (밀리초 -> 분)
                int durationInMs = summary.getInt("duration");
                String duration = String.format("%d분", durationInMs / (1000 * 60));

                // 통행료
                String tollFare = summary.optString("tollFare", "0") + "원";

                // 연료비
                String fuelPrice = summary.optString("fuelPrice", "0") + "원";

                // 경로 요약
                String routeSummary = startAddress + " → " + goalAddress;

                return new DirectionsResult(distance, duration, tollFare, fuelPrice, routeSummary);
            }
        }

        // 기본값 반환
        return new DirectionsResult("정보 없음", "정보 없음", "0원", "0원", "경로 정보를 가져올 수 없습니다.");
    }
    
    /**
     * 서비스 종료 시 호출
     */
    public void shutdown() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
}
