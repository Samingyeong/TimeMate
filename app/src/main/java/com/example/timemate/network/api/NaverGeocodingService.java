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

/**
 * 네이버 Geocoding API 서비스
 * 주소를 위도/경도 좌표로 변환
 */
public class NaverGeocodingService {
    
    private static final String TAG = "NaverGeocoding";
    private static final String CLIENT_ID = "YOUR_CLIENT_ID"; // 실제 클라이언트 ID로 교체
    private static final String CLIENT_SECRET = "YOUR_CLIENT_SECRET"; // 실제 클라이언트 시크릿으로 교체
    private static final String GEOCODING_URL = "https://naveropenapi.apigw.ntruss.com/map-geocode/v2/geocode";
    
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    /**
     * Geocoding 콜백 인터페이스
     */
    public interface GeocodingCallback {
        void onSuccess(double latitude, double longitude, String address);
        void onError(String error);
    }

    /**
     * 주소를 좌표로 변환
     */
    public void getCoordinates(String address, GeocodingCallback callback) {
        executor.execute(() -> {
            try {
                String encodedAddress = URLEncoder.encode(address, "UTF-8");
                String urlString = GEOCODING_URL + "?query=" + encodedAddress;
                
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("X-NCP-APIGW-API-KEY-ID", CLIENT_ID);
                connection.setRequestProperty("X-NCP-APIGW-API-KEY", CLIENT_SECRET);
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);
                
                int responseCode = connection.getResponseCode();
                Log.d(TAG, "Geocoding Response Code: " + responseCode);
                
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();
                    
                    parseGeocodingResponse(response.toString(), callback);
                } else {
                    BufferedReader errorReader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    StringBuilder errorResponse = new StringBuilder();
                    String errorLine;
                    
                    while ((errorLine = errorReader.readLine()) != null) {
                        errorResponse.append(errorLine);
                    }
                    errorReader.close();
                    
                    Log.e(TAG, "Geocoding Error: " + errorResponse.toString());
                    callback.onError("주소 검색 실패: " + responseCode);
                }
                
                connection.disconnect();
                
            } catch (Exception e) {
                Log.e(TAG, "Geocoding Exception", e);
                callback.onError("네트워크 오류: " + e.getMessage());
            }
        });
    }
    
    /**
     * Geocoding API 응답 파싱
     */
    private void parseGeocodingResponse(String response, GeocodingCallback callback) {
        try {
            JSONObject jsonObject = new JSONObject(response);
            String status = jsonObject.getString("status");
            
            if ("OK".equals(status)) {
                JSONArray addresses = jsonObject.getJSONArray("addresses");
                if (addresses.length() > 0) {
                    JSONObject firstAddress = addresses.getJSONObject(0);
                    double latitude = firstAddress.getDouble("y");
                    double longitude = firstAddress.getDouble("x");
                    String roadAddress = firstAddress.getString("roadAddress");
                    
                    Log.d(TAG, "Geocoding Success: " + latitude + ", " + longitude);
                    callback.onSuccess(latitude, longitude, roadAddress);
                } else {
                    callback.onError("주소를 찾을 수 없습니다");
                }
            } else {
                callback.onError("주소 검색 실패: " + status);
            }
        } catch (Exception e) {
            Log.e(TAG, "Geocoding Parse Error", e);
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
