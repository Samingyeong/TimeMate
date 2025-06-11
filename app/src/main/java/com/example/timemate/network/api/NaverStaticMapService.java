package com.example.timemate.network.api;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 네이버 Static Map API 서비스
 * 검색 결과를 지도에 마커로 표시
 */
public class NaverStaticMapService {

    private static final String TAG = "NaverStaticMap";
    private static final String CLIENT_ID = com.example.timemate.config.ApiConfig.NAVER_CLOUD_CLIENT_ID;
    private static final String CLIENT_SECRET = com.example.timemate.config.ApiConfig.NAVER_CLOUD_CLIENT_SECRET;
    private static final String STATIC_MAP_URL = com.example.timemate.config.ApiConfig.NAVER_STATIC_MAP_URL + "/raster";
    
    private ExecutorService executor;

    public NaverStaticMapService() {
        executor = Executors.newSingleThreadExecutor();
    }

    /**
     * 지도 이미지 로드 콜백
     */
    public interface MapImageCallback {
        void onSuccess(Bitmap bitmap);
        void onError(String error);
    }

    /**
     * 검색 결과를 지도에 표시
     */
    public void generateMapWithMarkers(List<NaverSearchApiService.SearchResult> results, MapImageCallback callback) {
        if (results == null || results.isEmpty()) {
            callback.onError("검색 결과가 없습니다");
            return;
        }

        executor.execute(() -> {
            try {
                // 마커 파라미터 생성 (개선된 형식)
                StringBuilder markersParam = new StringBuilder();

                for (int i = 0; i < Math.min(results.size(), 10); i++) { // 최대 10개 마커
                    NaverSearchApiService.SearchResult result = results.get(i);
                    if (i > 0) {
                        markersParam.append("|");
                    }
                    // 네이버 지도 마커 형식: type:t|size:mid|pos:경도 위도|label:숫자
                    markersParam.append("type:t|size:mid|pos:")
                              .append(result.getLongitude()).append(" ").append(result.getLatitude())
                              .append("|label:").append(i + 1)
                              .append("|color:0xFF0000"); // 빨간색
                }

                // 첫 번째 장소를 중심으로 지도 생성
                NaverSearchApiService.SearchResult firstResult = results.get(0);

                String urlString = STATIC_MAP_URL +
                    "?w=500" +
                    "&h=350" +
                    "&center=" + firstResult.getLongitude() + "," + firstResult.getLatitude() +
                    "&level=11" +
                    "&format=png" +
                    "&markers=" + URLEncoder.encode(markersParam.toString(), "UTF-8");

                Log.d(TAG, "Static Map URL: " + urlString);

                // HTTP 요청
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("X-NCP-APIGW-API-KEY-ID", CLIENT_ID);
                connection.setRequestProperty("X-NCP-APIGW-API-KEY", CLIENT_SECRET);
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);

                int responseCode = connection.getResponseCode();
                Log.d(TAG, "Static Map Response Code: " + responseCode);

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    InputStream inputStream = connection.getInputStream();
                    Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                    inputStream.close();
                    
                    if (bitmap != null) {
                        callback.onSuccess(bitmap);
                    } else {
                        callback.onError("지도 이미지 디코딩 실패");
                    }
                } else {
                    callback.onError("지도 API 오류: " + responseCode);
                }

                connection.disconnect();

            } catch (Exception e) {
                Log.e(TAG, "Static Map Exception", e);
                callback.onError("지도 생성 오류: " + e.getMessage());
            }
        });
    }

    /**
     * 기본 지도 생성 (마커 없이)
     */
    public void generateDefaultMap(double latitude, double longitude, MapImageCallback callback) {
        executor.execute(() -> {
            try {
                String urlString = STATIC_MAP_URL +
                    "?w=400" +
                    "&h=250" +
                    "&center=" + longitude + "," + latitude +
                    "&level=10";

                Log.d(TAG, "Default Map URL: " + urlString);

                // HTTP 요청
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("X-NCP-APIGW-API-KEY-ID", CLIENT_ID);
                connection.setRequestProperty("X-NCP-APIGW-API-KEY", CLIENT_SECRET);
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);

                int responseCode = connection.getResponseCode();
                Log.d(TAG, "Default Map Response Code: " + responseCode);

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    InputStream inputStream = connection.getInputStream();
                    Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                    inputStream.close();

                    if (bitmap != null) {
                        callback.onSuccess(bitmap);
                    } else {
                        callback.onError("기본 지도 이미지 디코딩 실패");
                    }
                } else {
                    callback.onError("기본 지도 API 오류: " + responseCode);
                }

                connection.disconnect();

            } catch (Exception e) {
                Log.e(TAG, "Default Map Exception", e);
                callback.onError("기본 지도 생성 오류: " + e.getMessage());
            }
        });
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
