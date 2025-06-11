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
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Google Places API 서비스
 * 무료 사용량이 많고 안정적인 장소 검색 서비스
 */
public class GooglePlacesService {

    private static final String TAG = "GooglePlaces";
    
    // Google Places API 설정 (무료 API 키 필요)
    private static final String API_KEY = "YOUR_GOOGLE_API_KEY"; // 실제 API 키로 교체 필요
    private static final String PLACES_URL = "https://maps.googleapis.com/maps/api/place/textsearch/json";
    
    private ExecutorService executor;
    private volatile boolean isSearching = false;

    public GooglePlacesService() {
        executor = Executors.newSingleThreadExecutor();
        Log.d(TAG, "Google Places API initialized");
    }

    /**
     * 검색 결과 콜백 인터페이스
     */
    public interface SearchCallback {
        void onSuccess(List<PlaceItem> places);
        void onError(String error);
    }

    /**
     * 장소 정보 클래스
     */
    public static class PlaceItem {
        public String name;
        public String category;
        public double latitude;
        public double longitude;
        public String address;
        public String placeId;
        public double rating;
        public String distance;

        public PlaceItem(String name, String category, double latitude, double longitude, String address) {
            this.name = name;
            this.category = category;
            this.latitude = latitude;
            this.longitude = longitude;
            this.address = address;
            this.placeId = "";
            this.rating = 0.0;
            this.distance = "";
        }

        public String getDisplayAddress() {
            return address;
        }

        public String getCategoryIcon() {
            if (category.contains("restaurant") || category.contains("food")) {
                return "🍽️";
            } else if (category.contains("cafe") || category.contains("coffee")) {
                return "☕";
            } else if (category.contains("hospital") || category.contains("health")) {
                return "🏥";
            } else if (category.contains("school") || category.contains("university")) {
                return "🏫";
            } else if (category.contains("bank") || category.contains("finance")) {
                return "🏦";
            } else if (category.contains("shopping") || category.contains("store")) {
                return "🛒";
            } else if (category.contains("parking")) {
                return "🅿️";
            } else if (category.contains("transit") || category.contains("station")) {
                return "🚇";
            } else if (category.contains("tourist") || category.contains("attraction")) {
                return "🗺️";
            } else {
                return "📍";
            }
        }
    }

    /**
     * 키워드로 장소 검색
     */
    public void searchPlacesByKeyword(String keyword, SearchCallback callback) {
        if (keyword == null || keyword.trim().length() < 2) {
            callback.onSuccess(new ArrayList<>());
            return;
        }

        if (isSearching) {
            Log.d(TAG, "Search already in progress, skipping: " + keyword);
            return;
        }

        executor.execute(() -> {
            isSearching = true;
            try {
                searchWithGoogleAPI(keyword.trim(), callback);
            } catch (Exception e) {
                Log.e(TAG, "Google Places API Exception", e);
                callback.onError("네트워크 오류: " + e.getMessage());
            } finally {
                isSearching = false;
            }
        });
    }

    /**
     * Google Places API로 검색
     */
    private void searchWithGoogleAPI(String keyword, SearchCallback callback) {
        try {
            // UTF-8 인코딩으로 한글 키워드 처리
            String encodedKeyword = URLEncoder.encode(keyword + " 한국", "UTF-8");
            
            String urlString = PLACES_URL + 
                "?query=" + encodedKeyword +
                "&key=" + API_KEY +
                "&language=ko" +
                "&region=kr";

            Log.d(TAG, "Google Places API URL: " + urlString);

            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            int responseCode = connection.getResponseCode();
            Log.d(TAG, "Google Places API Response Code: " + responseCode);

            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), "UTF-8"));
                StringBuilder response = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                parseGoogleResponse(response.toString(), callback);
                
            } else {
                BufferedReader errorReader = new BufferedReader(
                    new InputStreamReader(connection.getErrorStream(), "UTF-8"));
                StringBuilder errorResponse = new StringBuilder();
                String errorLine;

                while ((errorLine = errorReader.readLine()) != null) {
                    errorResponse.append(errorLine);
                }
                errorReader.close();

                Log.e(TAG, "Google Places API Error: " + errorResponse.toString());
                callback.onError("Google API 오류: " + responseCode);
            }

            connection.disconnect();

        } catch (Exception e) {
            Log.e(TAG, "Google Places API Exception", e);
            callback.onError("네트워크 오류: " + e.getMessage());
        }
    }

    /**
     * Google Places API 응답 파싱
     */
    private void parseGoogleResponse(String response, SearchCallback callback) {
        try {
            JSONObject jsonObject = new JSONObject(response);
            String status = jsonObject.optString("status", "");
            
            if (!"OK".equals(status)) {
                Log.e(TAG, "Google Places API Status: " + status);
                callback.onError("검색 결과가 없습니다");
                return;
            }

            List<PlaceItem> placeList = new ArrayList<>();
            JSONArray results = jsonObject.optJSONArray("results");
            
            if (results != null) {
                Log.d(TAG, "Found " + results.length() + " places in Google response");

                for (int i = 0; i < results.length() && i < 8; i++) {
                    JSONObject place = results.getJSONObject(i);

                    String name = place.optString("name", "");
                    String address = place.optString("formatted_address", "");
                    
                    // 위치 정보
                    JSONObject geometry = place.optJSONObject("geometry");
                    JSONObject location = geometry != null ? geometry.optJSONObject("location") : null;
                    double lat = location != null ? location.optDouble("lat", 0.0) : 0.0;
                    double lng = location != null ? location.optDouble("lng", 0.0) : 0.0;
                    
                    // 카테고리 정보
                    JSONArray types = place.optJSONArray("types");
                    String category = "장소";
                    if (types != null && types.length() > 0) {
                        category = types.optString(0, "장소");
                    }
                    
                    // 평점 정보
                    double rating = place.optDouble("rating", 4.0);
                    
                    String placeId = place.optString("place_id", "");

                    if (!name.isEmpty() && lat != 0.0 && lng != 0.0) {
                        PlaceItem placeItem = new PlaceItem(name, category, lat, lng, address);
                        placeItem.placeId = placeId;
                        placeItem.rating = rating;
                        
                        placeList.add(placeItem);
                        Log.d(TAG, "Added Google place: " + name + " at (" + lat + ", " + lng + ")");
                    }
                }
            }

            if (placeList.isEmpty()) {
                Log.w(TAG, "No Google search results found");
                callback.onError("검색 결과가 없습니다");
                return;
            }

            Log.d(TAG, "Google Search Success: " + placeList.size() + " places found");
            callback.onSuccess(placeList);

        } catch (Exception e) {
            Log.e(TAG, "Google Response Parse Error", e);
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
