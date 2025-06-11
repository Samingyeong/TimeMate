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

import com.example.timemate.config.ApiConfig;
import com.example.timemate.data.model.PlaceWithImage;
import com.example.timemate.features.recommendation.ImageCrawlingService;

/**
 * 카카오 로컬 API 서비스
 * 키워드 검색을 통해 실제 장소 정보를 가져옵니다.
 */
public class KakaoLocalSearchService {

    private static final String TAG = "KakaoLocalSearch";
    
    // 카카오 API 설정
    private static final String REST_API_KEY = ApiConfig.KAKAO_REST_API_KEY;
    private static final String KEYWORD_SEARCH_URL = ApiConfig.KAKAO_LOCAL_SEARCH_URL;
    private static final String ADDRESS_SEARCH_URL = ApiConfig.KAKAO_ADDRESS_SEARCH_URL;
    
    private ExecutorService executor;
    private ImageCrawlingService imageCrawlingService;

    // 동시 API 호출 제한을 위한 변수
    private volatile boolean isSearching = false;

    public KakaoLocalSearchService() {
        executor = Executors.newSingleThreadExecutor();
        imageCrawlingService = new ImageCrawlingService();

        // API 키 유효성 검사
        if (REST_API_KEY.equals("YOUR_KAKAO_REST_API_KEY") || REST_API_KEY.isEmpty()) {
            Log.e(TAG, "Kakao API key is not configured properly!");
        } else {
            Log.d(TAG, "Kakao API initialized with key: " + maskApiKey(REST_API_KEY));
        }
    }

    /**
     * API 키 마스킹 (보안을 위해 일부만 표시)
     */
    private String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.length() < 8) {
            return "****";
        }
        return apiKey.substring(0, 4) + "****" + apiKey.substring(apiKey.length() - 4);
    }

    /**
     * 검색 결과 콜백 인터페이스
     */
    public interface SearchCallback {
        void onSuccess(List<PlaceItem> places);
        void onError(String error);
    }

    /**
     * 이미지 포함 검색 결과 콜백 인터페이스
     */
    public interface SearchWithImageCallback {
        void onSuccess(List<PlaceWithImage> places);
        void onError(String error);
        void onImageLoaded(String placeId, String imageUrl); // 개별 이미지 로딩 완료 시
    }

    /**
     * 장소 정보 클래스
     */
    public static class PlaceItem {
        public String name;              // 장소명
        public String category;          // 카테고리
        public double latitude;          // 위도
        public double longitude;         // 경도
        public String address;           // 지번 주소
        public String roadAddress;       // 도로명 주소
        public String phone;             // 전화번호
        public String placeUrl;          // 카카오맵 URL
        public String distance;          // 거리 (검색 기준점으로부터)
        public double rating;            // 평점 (기본값)

        public PlaceItem(String name, String category, double latitude, double longitude, 
                        String address, String roadAddress) {
            this.name = name;
            this.category = category;
            this.latitude = latitude;
            this.longitude = longitude;
            this.address = address;
            this.roadAddress = roadAddress;
            this.phone = "";
            this.placeUrl = "";
            this.distance = "";
            this.rating = 0.0;
        }

        /**
         * 표시할 주소 반환 (도로명 주소 우선)
         */
        public String getDisplayAddress() {
            return !roadAddress.isEmpty() ? roadAddress : address;
        }

        /**
         * 카테고리 아이콘 반환
         */
        public String getCategoryIcon() {
            if (category.contains("음식점") || category.contains("맛집")) {
                return "🍽️";
            } else if (category.contains("카페") || category.contains("커피")) {
                return "☕";
            } else if (category.contains("병원") || category.contains("의료")) {
                return "🏥";
            } else if (category.contains("학교") || category.contains("대학")) {
                return "🏫";
            } else if (category.contains("은행") || category.contains("금융")) {
                return "🏦";
            } else if (category.contains("쇼핑") || category.contains("마트")) {
                return "🛒";
            } else if (category.contains("주차")) {
                return "🅿️";
            } else if (category.contains("지하철") || category.contains("역")) {
                return "🚇";
            } else if (category.contains("관광") || category.contains("명소")) {
                return "🗺️";
            } else {
                return "📍";
            }
        }
    }

    /**
     * 키워드로 장소 검색 (자동완성용)
     */
    public void searchPlacesByKeyword(String keyword, SearchCallback callback) {
        if (keyword == null || keyword.trim().length() < 2) {
            callback.onSuccess(new ArrayList<>());
            return;
        }

        // 동시 검색 방지
        if (isSearching) {
            Log.d(TAG, "Search already in progress, skipping: " + keyword);
            return;
        }

        executor.execute(() -> {
            isSearching = true;
            try {
                searchWithKakaoAPI(keyword.trim(), callback);
                
            } catch (Exception e) {
                Log.e(TAG, "Kakao API Exception", e);
                callback.onError("네트워크 오류: " + e.getMessage());
            } finally {
                isSearching = false;
            }
        });
    }

    /**
     * 카카오 로컬 API로 키워드 검색
     */
    private void searchWithKakaoAPI(String keyword, SearchCallback callback) {
        try {
            // UTF-8 인코딩으로 한글 키워드 처리
            String encodedKeyword = URLEncoder.encode(keyword, "UTF-8");
            
            // 카카오 로컬 API 파라미터 설정
            String urlString = KEYWORD_SEARCH_URL + 
                "?query=" + encodedKeyword +
                "&size=10" +                          // 최대 10개 결과
                "&page=1";                            // 첫 번째 페이지

            Log.d(TAG, "Kakao Local Search API URL: " + urlString);
            Log.d(TAG, "Encoded keyword: " + encodedKeyword);

            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            
            // 카카오 API 인증 헤더
            connection.setRequestProperty("Authorization", "KakaoAK " + REST_API_KEY);
            connection.setRequestProperty("Accept-Language", "ko");
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            int responseCode = connection.getResponseCode();
            Log.d(TAG, "Kakao API Response Code: " + responseCode);

            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), "UTF-8"));
                StringBuilder response = new StringBuilder();
                String line;
                int maxResponseSize = 20000; // 20KB 제한
                int currentSize = 0;

                while ((line = reader.readLine()) != null && currentSize < maxResponseSize) {
                    response.append(line);
                    currentSize += line.length();
                }
                reader.close();

                String responseStr = response.toString();
                Log.d(TAG, "Kakao API Response size: " + responseStr.length() + " chars");
                parseKakaoResponse(responseStr, callback);
                
            } else {
                // 오류 응답 읽기
                BufferedReader errorReader = new BufferedReader(
                    new InputStreamReader(connection.getErrorStream(), "UTF-8"));
                StringBuilder errorResponse = new StringBuilder();
                String errorLine;

                while ((errorLine = errorReader.readLine()) != null) {
                    errorResponse.append(errorLine);
                }
                errorReader.close();

                Log.e(TAG, "Kakao API Error: " + errorResponse.toString());
                callback.onError("카카오 API 오류: " + responseCode + " - " + errorResponse.toString());
            }

            connection.disconnect();

        } catch (Exception e) {
            Log.e(TAG, "Kakao API Exception", e);
            callback.onError("네트워크 오류: " + e.getMessage());
        }
    }

    /**
     * 카카오 API 응답 파싱
     */
    private void parseKakaoResponse(String response, SearchCallback callback) {
        try {
            JSONObject jsonObject = new JSONObject(response);
            List<PlaceItem> placeList = new ArrayList<>();

            // documents 배열 파싱 (카카오 API 응답 구조)
            if (jsonObject.has("documents")) {
                JSONArray documents = jsonObject.getJSONArray("documents");
                Log.d(TAG, "Found " + documents.length() + " places in Kakao response");

                for (int i = 0; i < documents.length() && i < 8; i++) { // 최대 8개까지
                    JSONObject doc = documents.getJSONObject(i);

                    String placeName = doc.optString("place_name", "");
                    String categoryName = doc.optString("category_name", "장소");
                    String address = doc.optString("address_name", "");
                    String roadAddress = doc.optString("road_address_name", "");
                    String phone = doc.optString("phone", "");
                    String placeUrl = doc.optString("place_url", "");
                    String distance = doc.optString("distance", "");
                    
                    // 좌표 정보
                    double latitude = doc.optDouble("y", 0.0);
                    double longitude = doc.optDouble("x", 0.0);

                    // 유효한 데이터만 추가
                    if (!placeName.isEmpty() && latitude != 0.0 && longitude != 0.0) {
                        PlaceItem placeItem = new PlaceItem(placeName, categoryName, 
                            latitude, longitude, address, roadAddress);
                        placeItem.phone = phone;
                        placeItem.placeUrl = placeUrl;
                        placeItem.distance = distance.isEmpty() ? "" : distance + "m";
                        placeItem.rating = 4.0 + Math.random(); // 임시 평점 (4.0~5.0)
                        
                        placeList.add(placeItem);
                        
                        Log.d(TAG, "Added Kakao place: " + placeName + " at (" + latitude + ", " + longitude + ")");
                    }
                }
            }

            // 결과가 없으면 오류 반환
            if (placeList.isEmpty()) {
                Log.w(TAG, "No Kakao search results found");
                callback.onError("검색 결과가 없습니다");
                return;
            }

            Log.d(TAG, "Kakao Search Success: " + placeList.size() + " places found");
            callback.onSuccess(placeList);

        } catch (Exception e) {
            Log.e(TAG, "Kakao Response Parse Error", e);
            callback.onError("응답 파싱 오류: " + e.getMessage());
        }
    }

    /**
     * 이미지 크롤링을 포함한 장소 검색 (지역 + 카테고리)
     * @param location 지역명 (예: "강남", "해운대")
     * @param category 카테고리 (예: "맛집", "카페", "관광명소")
     * @param callback 결과 콜백
     */
    public void searchPlacesWithImages(String location, String category, SearchWithImageCallback callback) {
        if (location == null || location.trim().isEmpty() ||
            category == null || category.trim().isEmpty()) {
            callback.onError("지역과 카테고리를 모두 입력해주세요");
            return;
        }

        // 동시 검색 방지
        if (isSearching) {
            Log.d(TAG, "Search already in progress, skipping: " + location + " " + category);
            return;
        }

        executor.execute(() -> {
            isSearching = true;
            try {
                // 키워드 조합: "지역 + 카테고리"
                String keyword = location.trim() + " " + category.trim();
                Log.d(TAG, "🔍 이미지 포함 검색 시작: " + keyword);

                searchWithImagesInternal(keyword, callback);

            } catch (Exception e) {
                Log.e(TAG, "❌ 이미지 포함 검색 오류", e);
                callback.onError("검색 오류: " + e.getMessage());
            } finally {
                isSearching = false;
            }
        });
    }

    /**
     * 내부 이미지 포함 검색 로직
     */
    private void searchWithImagesInternal(String keyword, SearchWithImageCallback callback) {
        try {
            // UTF-8 인코딩으로 한글 키워드 처리
            String encodedKeyword = URLEncoder.encode(keyword, "UTF-8");

            // 카카오 로컬 API 파라미터 설정
            String urlString = KEYWORD_SEARCH_URL +
                "?query=" + encodedKeyword +
                "&size=15" +                          // 최대 15개 결과 (이미지 로딩 실패 고려)
                "&page=1";                            // 첫 번째 페이지

            Log.d(TAG, "🌐 카카오 API 호출: " + urlString);

            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", "KakaoAK " + REST_API_KEY);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);

            int responseCode = connection.getResponseCode();
            Log.d(TAG, "📡 API 응답 코드: " + responseCode);

            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                Log.d(TAG, "✅ API 응답 수신 완료");
                parseResponseWithImages(response.toString(), callback);

            } else {
                String errorMessage = "API 호출 실패: " + responseCode;
                Log.e(TAG, errorMessage);
                callback.onError(errorMessage);
            }

            connection.disconnect();

        } catch (Exception e) {
            Log.e(TAG, "❌ API 호출 오류", e);
            callback.onError("네트워크 오류: " + e.getMessage());
        }
    }

    /**
     * API 응답을 PlaceWithImage 리스트로 파싱하고 이미지 크롤링 시작
     */
    private void parseResponseWithImages(String jsonResponse, SearchWithImageCallback callback) {
        try {
            JSONObject jsonObject = new JSONObject(jsonResponse);
            JSONArray documents = jsonObject.getJSONArray("documents");

            List<PlaceWithImage> placeList = new ArrayList<>();

            Log.d(TAG, "📋 파싱할 장소 수: " + documents.length());

            for (int i = 0; i < documents.length(); i++) {
                JSONObject place = documents.getJSONObject(i);

                // 카카오 API 응답 파싱
                String id = place.optString("id", "");
                String placeName = place.optString("place_name", "");
                String categoryName = place.optString("category_name", "");
                String categoryGroupCode = place.optString("category_group_code", "");
                String categoryGroupName = place.optString("category_group_name", "");
                String phone = place.optString("phone", "");
                String addressName = place.optString("address_name", "");
                String roadAddressName = place.optString("road_address_name", "");
                String x = place.optString("x", "0");
                String y = place.optString("y", "0");
                String placeUrl = place.optString("place_url", "");
                String distance = place.optString("distance", "");

                // PlaceWithImage 객체 생성
                if (!placeName.isEmpty() && !placeUrl.isEmpty()) {
                    PlaceWithImage placeWithImage = new PlaceWithImage(
                        id, placeName, categoryName, categoryGroupCode, categoryGroupName,
                        phone, addressName, roadAddressName, x, y, placeUrl, distance
                    );

                    placeList.add(placeWithImage);
                    Log.d(TAG, "📍 장소 추가: " + placeName + " -> " + placeUrl);
                }
            }

            if (placeList.isEmpty()) {
                Log.w(TAG, "⚠️ 검색 결과가 없습니다");
                callback.onError("검색 결과가 없습니다");
                return;
            }

            Log.d(TAG, "✅ 장소 파싱 완료: " + placeList.size() + "개");

            // 먼저 기본 결과 반환 (이미지 없이)
            callback.onSuccess(placeList);

            // 백그라운드에서 이미지 크롤링 시작
            startImageCrawling(placeList, callback);

        } catch (Exception e) {
            Log.e(TAG, "❌ 응답 파싱 오류", e);
            callback.onError("응답 파싱 오류: " + e.getMessage());
        }
    }

    /**
     * 각 장소에 대해 이미지 크롤링 시작
     */
    private void startImageCrawling(List<PlaceWithImage> places, SearchWithImageCallback callback) {
        Log.d(TAG, "🖼️ 이미지 크롤링 시작: " + places.size() + "개 장소");

        for (PlaceWithImage place : places) {
            imageCrawlingService.crawlPlaceImage(
                place.getId(),
                place.getPlaceUrl(),
                new ImageCrawlingService.ImageCrawlingCallback() {
                    @Override
                    public void onImageFound(String placeId, String imageUrl) {
                        // 해당 장소의 이미지 URL 업데이트
                        for (PlaceWithImage p : places) {
                            if (p.getId().equals(placeId)) {
                                p.setImageUrl(imageUrl);
                                p.setImageLoaded(true);
                                Log.d(TAG, "✅ 이미지 로딩 완료: " + p.getPlaceName() + " -> " + imageUrl);

                                // 개별 이미지 로딩 완료 알림
                                callback.onImageLoaded(placeId, imageUrl);
                                break;
                            }
                        }
                    }

                    @Override
                    public void onImageNotFound(String placeId) {
                        for (PlaceWithImage p : places) {
                            if (p.getId().equals(placeId)) {
                                p.setImageLoadFailed(true);
                                Log.w(TAG, "⚠️ 이미지 없음: " + p.getPlaceName());
                                break;
                            }
                        }
                    }

                    @Override
                    public void onError(String placeId, Exception error) {
                        for (PlaceWithImage p : places) {
                            if (p.getId().equals(placeId)) {
                                p.setImageLoadFailed(true);
                                Log.e(TAG, "❌ 이미지 크롤링 오류: " + p.getPlaceName(), error);
                                break;
                            }
                        }
                    }
                }
            );
        }
    }

    /**
     * 서비스 종료
     */
    public void shutdown() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
        if (imageCrawlingService != null) {
            imageCrawlingService.shutdown();
        }
    }
}
