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
 * 네이버 개발자 센터 검색 API 서비스
 * 지역 검색을 통해 실제 맛집, 카페, 관광명소 정보를 가져옵니다.
 */
public class NaverSearchApiService {

    private static final String TAG = "NaverSearchApi";
    
    // 네이버 개발자 센터 API 키
    private static final String CLIENT_ID = com.example.timemate.config.ApiConfig.NAVER_DEV_CLIENT_ID;
    private static final String CLIENT_SECRET = com.example.timemate.config.ApiConfig.NAVER_DEV_CLIENT_SECRET;
    private static final String SEARCH_URL = com.example.timemate.config.ApiConfig.NAVER_LOCAL_SEARCH_URL;
    
    private ExecutorService executor;

    public NaverSearchApiService() {
        executor = Executors.newSingleThreadExecutor();
    }

    /**
     * 검색 결과 콜백 인터페이스
     */
    public interface SearchCallback {
        void onSuccess(List<SearchResult> results);
        void onError(String error);
    }

    /**
     * 검색 결과 데이터 클래스
     */
    public static class SearchResult {
        public String title;           // 업체명
        public String category;        // 카테고리
        public String description;     // 설명
        public String telephone;       // 전화번호
        public String address;         // 주소
        public String roadAddress;     // 도로명 주소
        public int mapx;              // x 좌표 (경도)
        public int mapy;              // y 좌표 (위도)
        public String link;           // 네이버 상세 링크

        public SearchResult(String title, String category, String description, 
                          String telephone, String address, String roadAddress,
                          int mapx, int mapy, String link) {
            this.title = cleanHtmlTags(title);
            this.category = category;
            this.description = cleanHtmlTags(description);
            this.telephone = telephone;
            this.address = address;
            this.roadAddress = roadAddress;
            this.mapx = mapx;
            this.mapy = mapy;
            this.link = link;
        }

        /**
         * HTML 태그 제거
         */
        private String cleanHtmlTags(String text) {
            if (text == null) return "";
            return text.replaceAll("<[^>]*>", "");
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
         * 위도 반환 (네이버 좌표계를 WGS84로 변환)
         */
        public double getLatitude() {
            return mapy / 10000000.0;
        }

        /**
         * 경도 반환 (네이버 좌표계를 WGS84로 변환)
         */
        public double getLongitude() {
            return mapx / 10000000.0;
        }
    }

    /**
     * 지역별 카테고리 검색
     * @param category 카테고리 (맛집, 카페, 관광명소)
     * @param location 지역명
     * @param callback 결과 콜백
     */
    public void searchByCategory(String category, String location, SearchCallback callback) {
        executor.execute(() -> {
            try {
                String query = buildSearchQuery(category, location);
                performSearch(query, callback);
            } catch (Exception e) {
                Log.e(TAG, "Search exception", e);
                callback.onError("검색 중 오류가 발생했습니다: " + e.getMessage());
            }
        });
    }

    /**
     * 검색 쿼리 생성
     */
    private String buildSearchQuery(String category, String location) {
        String query;
        switch (category) {
            case "맛집":
                query = location + " 맛집 음식점 레스토랑";
                break;
            case "카페":
                query = location + " 카페 커피 디저트";
                break;
            case "관광명소":
                query = location + " 관광지 명소 박물관 공원";
                break;
            default:
                query = location + " " + category;
                break;
        }
        return query;
    }

    /**
     * 실제 검색 수행
     */
    private void performSearch(String query, SearchCallback callback) {
        try {
            String encodedQuery = URLEncoder.encode(query, "UTF-8");
            String urlString = SEARCH_URL + "?query=" + encodedQuery + 
                             "&display=5" +  // 상위 5개만
                             "&start=1" + 
                             "&sort=random";

            Log.d(TAG, "=== Naver Search API Call ===");
            Log.d(TAG, "Query: " + query);
            Log.d(TAG, "URL: " + urlString);

            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("X-Naver-Client-Id", CLIENT_ID);
            connection.setRequestProperty("X-Naver-Client-Secret", CLIENT_SECRET);
            connection.setRequestProperty("User-Agent", "TimeMate/1.0");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);

            int responseCode = connection.getResponseCode();
            Log.d(TAG, "Response Code: " + responseCode);

            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), "UTF-8"));
                StringBuilder response = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                Log.d(TAG, "Response: " + response.toString());
                parseSearchResponse(response.toString(), callback);

            } else {
                BufferedReader errorReader = new BufferedReader(
                    new InputStreamReader(connection.getErrorStream(), "UTF-8"));
                StringBuilder errorResponse = new StringBuilder();
                String errorLine;

                while ((errorLine = errorReader.readLine()) != null) {
                    errorResponse.append(errorLine);
                }
                errorReader.close();

                Log.e(TAG, "API Error: " + errorResponse.toString());
                callback.onError("검색 API 오류: " + responseCode);
            }

            connection.disconnect();

        } catch (Exception e) {
            Log.e(TAG, "Network exception", e);
            callback.onError("네트워크 오류: " + e.getMessage());
        }
    }

    /**
     * 검색 응답 파싱
     */
    private void parseSearchResponse(String response, SearchCallback callback) {
        try {
            JSONObject jsonObject = new JSONObject(response);
            JSONArray items = jsonObject.getJSONArray("items");
            
            List<SearchResult> results = new ArrayList<>();
            
            for (int i = 0; i < items.length(); i++) {
                JSONObject item = items.getJSONObject(i);
                
                SearchResult result = new SearchResult(
                    item.optString("title", ""),
                    item.optString("category", ""),
                    item.optString("description", ""),
                    item.optString("telephone", ""),
                    item.optString("address", ""),
                    item.optString("roadAddress", ""),
                    item.optInt("mapx", 0),
                    item.optInt("mapy", 0),
                    item.optString("link", "")
                );
                
                results.add(result);
            }
            
            Log.d(TAG, "Parsed " + results.size() + " search results");
            callback.onSuccess(results);
            
        } catch (Exception e) {
            Log.e(TAG, "Parse exception", e);
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
