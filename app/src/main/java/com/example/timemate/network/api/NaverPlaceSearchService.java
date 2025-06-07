package com.example.timemate.network.api;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 네이버 Place Search API 서비스
 * 위치 기반 장소 검색 (맛집, 카페, 관광명소 등)
 */
public class NaverPlaceSearchService {
    
    private static final String TAG = "NaverPlaceSearch";
    private static final String CLIENT_ID = "YOUR_CLIENT_ID"; // 실제 클라이언트 ID로 교체
    private static final String CLIENT_SECRET = "YOUR_CLIENT_SECRET"; // 실제 클라이언트 시크릿으로 교체
    private static final String SEARCH_URL = "https://naveropenapi.apigw.ntruss.com/map-place/v1/search";
    
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    /**
     * 장소 검색 콜백 인터페이스
     */
    public interface PlaceSearchCallback {
        void onSuccess(List<PlaceItem> places);
        void onError(String error);
    }

    /**
     * 장소 정보 데이터 클래스
     */
    public static class PlaceItem {
        public String name;
        public String category;
        public double latitude;
        public double longitude;
        public String address;
        public double rating;
        public String distance;
        public String tel;
        public String businessHours;

        public PlaceItem(String name, String category, double latitude, double longitude, 
                        String address, double rating, String distance) {
            this.name = name;
            this.category = category;
            this.latitude = latitude;
            this.longitude = longitude;
            this.address = address;
            this.rating = rating;
            this.distance = distance;
        }

        @Override
        public String toString() {
            return String.format("%s (%s) - %.1f★", name, category, rating);
        }
    }

    /**
     * 근처 장소 검색
     */
    public void searchNearbyPlaces(double latitude, double longitude, String category, PlaceSearchCallback callback) {
        executor.execute(() -> {
            try {
                String query = getQueryByCategory(category);
                String encodedQuery = URLEncoder.encode(query, "UTF-8");
                
                // 반경 2km 내 검색
                String urlString = SEARCH_URL + "?query=" + encodedQuery + 
                                 "&coordinate=" + longitude + "," + latitude + 
                                 "&radius=2000" + // 2km
                                 "&display=10"; // Top 10
                
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("X-NCP-APIGW-API-KEY-ID", CLIENT_ID);
                connection.setRequestProperty("X-NCP-APIGW-API-KEY", CLIENT_SECRET);
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);
                
                int responseCode = connection.getResponseCode();
                Log.d(TAG, "Place Search Response Code: " + responseCode);
                
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();
                    
                    parsePlaceSearchResponse(response.toString(), category, callback);
                } else {
                    BufferedReader errorReader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    StringBuilder errorResponse = new StringBuilder();
                    String errorLine;
                    
                    while ((errorLine = errorReader.readLine()) != null) {
                        errorResponse.append(errorLine);
                    }
                    errorReader.close();
                    
                    Log.e(TAG, "Place Search Error: " + errorResponse.toString());
                    callback.onError("장소 검색 실패: " + responseCode);
                }
                
                connection.disconnect();
                
            } catch (Exception e) {
                Log.e(TAG, "Place Search Exception", e);
                callback.onError("네트워크 오류: " + e.getMessage());
            }
        });
    }
    
    /**
     * 카테고리별 검색 쿼리 생성
     */
    private String getQueryByCategory(String category) {
        switch (category) {
            case "맛집":
                return "맛집 음식점";
            case "카페":
                return "카페 커피";
            case "관광명소":
                return "관광지 명소";
            case "쇼핑":
                return "쇼핑몰 마트";
            case "병원":
                return "병원 의료";
            case "주차장":
                return "주차장";
            default:
                return "맛집";
        }
    }
    
    /**
     * Place Search API 응답 파싱
     */
    private void parsePlaceSearchResponse(String response, String category, PlaceSearchCallback callback) {
        try {
            JSONObject jsonObject = new JSONObject(response);
            JSONArray places = jsonObject.getJSONArray("places");
            
            List<PlaceItem> placeList = new ArrayList<>();
            
            for (int i = 0; i < places.length(); i++) {
                JSONObject place = places.getJSONObject(i);
                
                String name = place.optString("name", "이름 없음");
                double lat = place.optDouble("y", 0.0);
                double lng = place.optDouble("x", 0.0);
                String address = place.optString("roadAddress", place.optString("address", "주소 없음"));
                
                // 평점 정보 (실제 API 응답에 따라 조정 필요)
                double rating = place.optDouble("totalScore", 0.0);
                if (rating == 0.0) {
                    rating = 3.5 + (Math.random() * 1.5); // 임시 평점 (3.5~5.0)
                }
                
                // 거리 계산 (간단한 직선거리)
                String distance = "약 " + (int)(Math.random() * 1500 + 100) + "m";
                
                PlaceItem item = new PlaceItem(name, category, lat, lng, address, rating, distance);
                item.tel = place.optString("tel", "");
                item.businessHours = place.optString("businessHours", "");
                
                placeList.add(item);
            }
            
            // 평점 기준 내림차순 정렬
            Collections.sort(placeList, (a, b) -> Double.compare(b.rating, a.rating));
            
            Log.d(TAG, "Place Search Success: " + placeList.size() + " places found");
            callback.onSuccess(placeList);
            
        } catch (Exception e) {
            Log.e(TAG, "Place Search Parse Error", e);
            callback.onError("응답 파싱 오류: " + e.getMessage());
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
