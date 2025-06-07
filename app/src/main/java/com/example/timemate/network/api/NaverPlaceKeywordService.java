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
 * 네이버 Place Keyword API 서비스
 * 키워드 기반 장소 검색 및 자동완성 기능
 */
public class NaverPlaceKeywordService {
    
    private static final String TAG = "NaverPlaceKeyword";
    private static final String CLIENT_ID = "dnnydofmgg";
    private static final String CLIENT_SECRET = "GlevAwH7wuE5x2zfXuzwL9KVcHJUq5p7P7zYSF45";
    private static final String SEARCH_URL = "https://naveropenapi.apigw.ntruss.com/map-place/v1/search";
    
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    /**
     * 장소 검색 콜백 인터페이스
     */
    public interface PlaceKeywordCallback {
        void onSuccess(List<PlaceItem> places);
        void onError(String error);
    }

    /**
     * 장소 정보 데이터 클래스
     */
    public static class PlaceItem {
        public String name;          // 장소명
        public String address;       // 주소
        public String roadAddress;   // 도로명 주소
        public double latitude;      // 위도
        public double longitude;     // 경도
        public String category;      // 카테고리
        public String tel;           // 전화번호
        public String distance;      // 거리 (검색 기준점에서)

        public PlaceItem() {}

        public PlaceItem(String name, String address, String roadAddress, 
                        double latitude, double longitude, String category) {
            this.name = name;
            this.address = address;
            this.roadAddress = roadAddress;
            this.latitude = latitude;
            this.longitude = longitude;
            this.category = category;
        }

        /**
         * 표시용 주소 반환 (도로명 주소 우선)
         */
        public String getDisplayAddress() {
            if (roadAddress != null && !roadAddress.isEmpty()) {
                return roadAddress;
            }
            return address != null ? address : "";
        }

        /**
         * 전체 정보 문자열
         */
        public String getFullInfo() {
            return name + " (" + getDisplayAddress() + ")";
        }

        @Override
        public String toString() {
            return name + " - " + getDisplayAddress();
        }
    }

    /**
     * 키워드로 장소 검색 (자동완성용)
     */
    public void searchPlacesByKeyword(String keyword, PlaceKeywordCallback callback) {
        if (keyword == null || keyword.trim().length() < 2) {
            callback.onSuccess(new ArrayList<>());
            return;
        }

        executor.execute(() -> {
            try {
                String encodedKeyword = URLEncoder.encode(keyword.trim(), "UTF-8");
                String urlString = SEARCH_URL + "?query=" + encodedKeyword + "&display=10&start=1&sort=random";
                
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("X-NCP-APIGW-API-KEY-ID", CLIENT_ID);
                connection.setRequestProperty("X-NCP-APIGW-API-KEY", CLIENT_SECRET);
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                
                int responseCode = connection.getResponseCode();
                Log.d(TAG, "Place Keyword Search Response Code: " + responseCode);
                
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();
                    
                    parsePlaceKeywordResponse(response.toString(), callback);
                } else {
                    BufferedReader errorReader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    StringBuilder errorResponse = new StringBuilder();
                    String errorLine;
                    
                    while ((errorLine = errorReader.readLine()) != null) {
                        errorResponse.append(errorLine);
                    }
                    errorReader.close();
                    
                    Log.e(TAG, "Place Keyword Search Error: " + errorResponse.toString());
                    callback.onError("장소 검색 실패: " + responseCode);
                }
                
                connection.disconnect();
                
            } catch (Exception e) {
                Log.e(TAG, "Place Keyword Search Exception", e);
                callback.onError("네트워크 오류: " + e.getMessage());
            }
        });
    }

    /**
     * API 응답 파싱
     */
    private void parsePlaceKeywordResponse(String response, PlaceKeywordCallback callback) {
        try {
            JSONObject jsonObject = new JSONObject(response);
            
            List<PlaceItem> placeList = new ArrayList<>();
            
            // places 배열 파싱
            if (jsonObject.has("places")) {
                JSONArray places = jsonObject.getJSONArray("places");
                
                for (int i = 0; i < places.length(); i++) {
                    JSONObject place = places.getJSONObject(i);
                    
                    String name = place.optString("name", "");
                    String address = place.optString("address", "");
                    String roadAddress = place.optString("roadAddress", "");
                    double lat = place.optDouble("y", 0.0);
                    double lng = place.optDouble("x", 0.0);
                    String category = place.optString("category", "");
                    String tel = place.optString("tel", "");
                    
                    if (!name.isEmpty() && (lat != 0.0 || lng != 0.0)) {
                        PlaceItem item = new PlaceItem(name, address, roadAddress, lat, lng, category);
                        item.tel = tel;
                        placeList.add(item);
                    }
                }
            }
            
            Log.d(TAG, "Place Keyword Search Success: " + placeList.size() + " places found");
            callback.onSuccess(placeList);
            
        } catch (Exception e) {
            Log.e(TAG, "Place Keyword Parse Error", e);
            callback.onError("응답 파싱 오류: " + e.getMessage());
        }
    }

    /**
     * 좌표로 주소 검색 (Reverse Geocoding)
     */
    public void getAddressByCoordinates(double latitude, double longitude, PlaceKeywordCallback callback) {
        executor.execute(() -> {
            try {
                // 네이버 Reverse Geocoding API 사용
                String urlString = "https://naveropenapi.apigw.ntruss.com/map-reversegeocode/v2/gc" +
                    "?coords=" + longitude + "," + latitude + "&sourcecrs=epsg:4326&targetcrs=epsg:4326&orders=roadaddr,addr";
                
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("X-NCP-APIGW-API-KEY-ID", CLIENT_ID);
                connection.setRequestProperty("X-NCP-APIGW-API-KEY", CLIENT_SECRET);
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                
                int responseCode = connection.getResponseCode();
                
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();
                    
                    parseReverseGeocodingResponse(response.toString(), latitude, longitude, callback);
                } else {
                    callback.onError("주소 검색 실패: " + responseCode);
                }
                
                connection.disconnect();
                
            } catch (Exception e) {
                Log.e(TAG, "Reverse Geocoding Exception", e);
                callback.onError("주소 검색 오류: " + e.getMessage());
            }
        });
    }

    /**
     * Reverse Geocoding 응답 파싱
     */
    private void parseReverseGeocodingResponse(String response, double lat, double lng, PlaceKeywordCallback callback) {
        try {
            JSONObject jsonObject = new JSONObject(response);
            
            if (jsonObject.has("results") && jsonObject.getJSONArray("results").length() > 0) {
                JSONObject result = jsonObject.getJSONArray("results").getJSONObject(0);
                JSONObject region = result.getJSONObject("region");
                
                // 주소 조합
                String address = "";
                if (region.has("area1")) {
                    address += region.getJSONObject("area1").optString("name", "") + " ";
                }
                if (region.has("area2")) {
                    address += region.getJSONObject("area2").optString("name", "") + " ";
                }
                if (region.has("area3")) {
                    address += region.getJSONObject("area3").optString("name", "");
                }
                
                PlaceItem item = new PlaceItem("현재 위치", address.trim(), "", lat, lng, "위치");
                List<PlaceItem> resultList = new ArrayList<>();
                resultList.add(item);
                
                callback.onSuccess(resultList);
            } else {
                callback.onError("주소를 찾을 수 없습니다");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Reverse Geocoding Parse Error", e);
            callback.onError("주소 파싱 오류: " + e.getMessage());
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
