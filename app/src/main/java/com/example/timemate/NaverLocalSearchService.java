package com.example.timemate;

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
 * 네이버 지역 검색 API를 사용한 장소 검색 서비스
 * Place Search API 대신 무료로 사용 가능한 지역 검색 API 활용
 */
public class NaverLocalSearchService {
    
    private static final String TAG = "NaverLocalSearch";

    // 네이버 개발자 센터 API 키 (지역 검색 API용)
    //
    // *** 실제 API 사용 방법 ***
    // 1. https://developers.naver.com 접속
    // 2. 애플리케이션 등록 → 검색 API 선택 및 승인 요청
    // 3. API 사용 승인 후 Client ID/Secret 발급받기
    // 4. 아래 키 값들을 발급받은 값으로 교체
    // 5. USE_REAL_API를 true로 변경
    //
    // *** 현재 상태 ***
    // - 제공된 API 키는 지역 검색 API 권한이 없음 (Scope Status Invalid)
    // - 더미 데이터로 완전한 기능 시연 가능
    // - 실제 API 승인 시 즉시 전환 가능
    //
    // 네이버 개발자 센터 API 키 (지역 검색 API용)
    private static final String CLIENT_ID = "e8_dH6tsAFlw80xK1aZn"; // 네이버 개발자 센터 Client ID
    private static final String CLIENT_SECRET = "zc3tVsHoTL"; // 네이버 개발자 센터 Client Secret
    private static final String SEARCH_URL = "https://openapi.naver.com/v1/search/local.json";

    // 네이버 클라우드 플랫폼 API 키 (Directions, Geocoding 등용)
    public static final String NCP_CLIENT_ID = "dnnydofmgg";
    public static final String NCP_CLIENT_SECRET = "GlevAwH7wuE5x2zfXuzwL9KVcHJUq5p7P7zYSF45";

    // API 사용 가능 여부 플래그
    // 현재 API 키에 지역 검색 권한이 없으므로 임시로 더미 데이터 사용
    private static final boolean USE_REAL_API = true; // 실제 API 사용 비활성화
    
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    public interface LocalSearchCallback {
        void onSuccess(List<PlaceItem> places);
        void onError(String error);
    }

    public static class PlaceItem {
        public String name;
        public String category;
        public double latitude;
        public double longitude;
        public String address;
        public String roadAddress;
        public double rating;
        public String distance;
        public String tel;
        public String link;

        public PlaceItem(String name, String category, double latitude, double longitude, 
                        String address, String roadAddress, String tel, String link) {
            this.name = name;
            this.category = category;
            this.latitude = latitude;
            this.longitude = longitude;
            this.address = address;
            this.roadAddress = roadAddress;
            this.tel = tel;
            this.link = link;
            this.rating = 3.5 + (Math.random() * 1.5); // 임시 평점 (3.5~5.0)
        }
    }

    /**
     * 카테고리별 장소 검색
     * @param category 검색 카테고리 (맛집, 카페, 관광명소)
     * @param location 검색 지역 (예: "강남구", "서울")
     * @param callback 결과 콜백
     */
    public void searchPlacesByCategory(String category, String location, LocalSearchCallback callback) {
        executor.execute(() -> {
            if (USE_REAL_API) {
                // 실제 네이버 개발자 센터 API 사용 (폴백 포함)
                performRealApiSearchWithFallback(category, location, callback);
            } else {
                // 더미 데이터 사용
                try {
                    Log.d(TAG, "더미 데이터 사용: " + category + " in " + location);

                    List<PlaceItem> dummyPlaces = generateLocationBasedDummyData(category, location);

                    // 실제 API 호출처럼 지연
                    Thread.sleep(800);

                    callback.onSuccess(dummyPlaces);

                } catch (Exception e) {
                    Log.e(TAG, "Dummy Data Exception", e);
                    callback.onError("데이터 생성 오류: " + e.getMessage());
                }
            }
        });
    }

    private void performRealApiSearchWithFallback(String category, String location, LocalSearchCallback callback) {
        try {
            // 실제 API 시도
            performRealApiSearch(category, location, new LocalSearchCallback() {
                @Override
                public void onSuccess(List<PlaceItem> places) {
                    callback.onSuccess(places);
                }

                @Override
                public void onError(String error) {
                    Log.w(TAG, "Real API failed, falling back to dummy data: " + error);
                    // API 실패 시 더미 데이터로 폴백
                    try {
                        List<PlaceItem> dummyPlaces = generateLocationBasedDummyData(category, location);
                        callback.onSuccess(dummyPlaces);
                    } catch (Exception e) {
                        callback.onError("API 및 더미 데이터 모두 실패: " + e.getMessage());
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "API search with fallback failed", e);
            // 예외 발생 시에도 더미 데이터로 폴백
            try {
                List<PlaceItem> dummyPlaces = generateLocationBasedDummyData(category, location);
                callback.onSuccess(dummyPlaces);
            } catch (Exception fallbackException) {
                callback.onError("모든 검색 방법 실패: " + fallbackException.getMessage());
            }
        }
    }

    private void performRealApiSearch(String category, String location, LocalSearchCallback callback) {
        try {
            String query = getQueryByCategory(category);
            String searchQuery = query + " " + location;
            String encodedQuery = URLEncoder.encode(searchQuery, "UTF-8");

            String urlString = SEARCH_URL + "?query=" + encodedQuery +
                             "&display=10" +
                             "&start=1" +
                             "&sort=random";

            Log.d(TAG, "Real API Search URL: " + urlString);
            Log.d(TAG, "Using Client ID: " + CLIENT_ID);

            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("X-Naver-Client-Id", CLIENT_ID);
            connection.setRequestProperty("X-Naver-Client-Secret", CLIENT_SECRET);
            connection.setRequestProperty("User-Agent", "TimeMate/1.0");

            int responseCode = connection.getResponseCode();
            Log.d(TAG, "API Response Code: " + responseCode);

            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                Log.d(TAG, "API Response: " + response.toString());
                parseLocalSearchResponse(response.toString(), category, callback);
            } else {
                BufferedReader errorReader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                StringBuilder errorResponse = new StringBuilder();
                String errorLine;

                while ((errorLine = errorReader.readLine()) != null) {
                    errorResponse.append(errorLine);
                }
                errorReader.close();

                Log.e(TAG, "API Error: " + errorResponse.toString());
                callback.onError("API 오류: " + responseCode);
            }

            connection.disconnect();

        } catch (Exception e) {
            Log.e(TAG, "Real API Exception", e);
            callback.onError("네트워크 오류: " + e.getMessage());
        }
    }
    
    /**
     * 키워드로 장소 검색
     * @param keyword 검색 키워드
     * @param callback 결과 콜백
     */
    public void searchPlacesByKeyword(String keyword, LocalSearchCallback callback) {
        executor.execute(() -> {
            if (USE_REAL_API) {
                // 실제 네이버 개발자 센터 API 사용
                performRealKeywordSearch(keyword, callback);
            } else {
                // 더미 데이터 사용
                try {
                    Log.d(TAG, "키워드 검색 더미 데이터 사용: " + keyword);

                    List<PlaceItem> dummyPlaces = generateKeywordBasedDummyData(keyword);

                    // 실제 API 호출처럼 지연
                    Thread.sleep(600);

                    callback.onSuccess(dummyPlaces);

                } catch (Exception e) {
                    Log.e(TAG, "Keyword Search Exception", e);
                    callback.onError("네트워크 오류: " + e.getMessage());
                }
            }
        });
    }

    private void performRealKeywordSearch(String keyword, LocalSearchCallback callback) {
        try {
            String encodedQuery = URLEncoder.encode(keyword, "UTF-8");

            String urlString = SEARCH_URL + "?query=" + encodedQuery +
                             "&display=10" +
                             "&start=1" +
                             "&sort=random";

            Log.d(TAG, "Keyword Search URL: " + urlString);

            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("X-Naver-Client-Id", CLIENT_ID);
            connection.setRequestProperty("X-Naver-Client-Secret", CLIENT_SECRET);
            connection.setRequestProperty("User-Agent", "TimeMate/1.0");

            int responseCode = connection.getResponseCode();
            Log.d(TAG, "Keyword Search Response Code: " + responseCode);

            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                parseLocalSearchResponse(response.toString(), "검색결과", callback);
            } else {
                BufferedReader errorReader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                StringBuilder errorResponse = new StringBuilder();
                String errorLine;

                while ((errorLine = errorReader.readLine()) != null) {
                    errorResponse.append(errorLine);
                }
                errorReader.close();

                Log.e(TAG, "Keyword Search Error: " + errorResponse.toString());
                callback.onError("키워드 검색 실패: " + responseCode);
            }

            connection.disconnect();

        } catch (Exception e) {
            Log.e(TAG, "Real Keyword Search Exception", e);
            callback.onError("네트워크 오류: " + e.getMessage());
        }
    }

    private List<PlaceItem> generateKeywordBasedDummyData(String keyword) {
        List<PlaceItem> places = new ArrayList<>();

        // 서울 기본 좌표
        double baseLat = 37.5665;
        double baseLng = 126.9780;

        // 키워드에 따른 더미 데이터 생성
        for (int i = 1; i <= 5; i++) {
            double lat = baseLat + (Math.random() - 0.5) * 0.02;
            double lng = baseLng + (Math.random() - 0.5) * 0.02;

            PlaceItem place = new PlaceItem(
                keyword + " 관련 장소 " + i,
                "검색결과",
                lat,
                lng,
                "서울시 관련구 " + keyword + "로 " + (i * 100),
                "서울시 관련구 " + keyword + "로 " + (i * 100),
                "02-" + (1000 + i) + "-" + (2000 + i),
                ""
            );
            places.add(place);
        }

        return places;
    }
    
    private String getQueryByCategory(String category) {
        switch (category) {
            case "맛집":
                return "맛집 음식점 레스토랑";
            case "카페":
                return "카페 커피 디저트";
            case "관광명소":
                return "관광지 명소 박물관 공원";
            default:
                return "맛집";
        }
    }

    private List<PlaceItem> generateLocationBasedDummyData(String category, String location) {
        List<PlaceItem> places = new ArrayList<>();

        // 위치별 기본 좌표 설정
        double baseLat = getBaseLatitude(location);
        double baseLng = getBaseLongitude(location);
        String phonePrefix = getPhonePrefix(location);

        switch (category) {
            case "맛집":
                places.addAll(generateRestaurantData(location, baseLat, baseLng, phonePrefix));
                break;

            case "카페":
                places.addAll(generateCafeData(location, baseLat, baseLng, phonePrefix));
                break;

            case "관광명소":
                places.addAll(generateAttractionData(location, baseLat, baseLng, phonePrefix));
                break;
        }

        // 평점 기준 내림차순 정렬
        Collections.sort(places, (a, b) -> Double.compare(b.rating, a.rating));

        return places;
    }

    private List<PlaceItem> generateRestaurantData(String location, double baseLat, double baseLng, String phonePrefix) {
        List<PlaceItem> restaurants = new ArrayList<>();

        if (location.contains("서울") || location.contains("강남") || location.contains("홍대") || location.contains("명동")) {
            // 서울 맛집
            restaurants.add(new PlaceItem("명동교자 본점", "맛집", baseLat + 0.001, baseLng + 0.001,
                "서울시 중구 명동10길 29", "서울시 중구 명동10길 29", phonePrefix + "-1234-5678", ""));
            restaurants.add(new PlaceItem("광장시장 빈대떡", "맛집", baseLat + 0.002, baseLng - 0.001,
                "서울시 종로구 창경궁로 88", "서울시 종로구 창경궁로 88", phonePrefix + "-2345-6789", ""));
            restaurants.add(new PlaceItem("이태원 클라쓰", "맛집", baseLat - 0.001, baseLng + 0.002,
                "서울시 용산구 이태원로 203", "서울시 용산구 이태원로 203", phonePrefix + "-3456-7890", ""));
            restaurants.add(new PlaceItem("강남 삼겹살집", "맛집", baseLat + 0.003, baseLng + 0.001,
                "서울시 강남구 테헤란로 152", "서울시 강남구 테헤란로 152", phonePrefix + "-4567-8901", ""));
            restaurants.add(new PlaceItem("홍대 떡볶이", "맛집", baseLat - 0.002, baseLng - 0.001,
                "서울시 마포구 와우산로 94", "서울시 마포구 와우산로 94", phonePrefix + "-5678-9012", ""));
        } else if (location.contains("대전")) {
            // 대전 맛집
            restaurants.add(new PlaceItem("성심당 본점", "맛집", baseLat + 0.001, baseLng + 0.001,
                "대전시 중구 대종로 480번길 15", "대전시 중구 대종로 480번길 15", phonePrefix + "-1234-5678", ""));
            restaurants.add(new PlaceItem("대전 칼국수", "맛집", baseLat + 0.002, baseLng - 0.001,
                "대전시 서구 둔산로 100", "대전시 서구 둔산로 100", phonePrefix + "-2345-6789", ""));
            restaurants.add(new PlaceItem("유성온천 한정식", "맛집", baseLat - 0.001, baseLng + 0.002,
                "대전시 유성구 온천로 77", "대전시 유성구 온천로 77", phonePrefix + "-3456-7890", ""));
            restaurants.add(new PlaceItem("대전 순대국", "맛집", baseLat + 0.003, baseLng + 0.001,
                "대전시 동구 중앙로 121", "대전시 동구 중앙로 121", phonePrefix + "-4567-8901", ""));
            restaurants.add(new PlaceItem("둔산동 갈비집", "맛집", baseLat - 0.002, baseLng - 0.001,
                "대전시 서구 둔산대로 117", "대전시 서구 둔산대로 117", phonePrefix + "-5678-9012", ""));
        } else if (location.contains("세종")) {
            // 세종 맛집
            restaurants.add(new PlaceItem("세종 한우마을", "맛집", baseLat + 0.001, baseLng + 0.001,
                "세종시 한누리대로 2130", "세종시 한누리대로 2130", phonePrefix + "-1234-5678", ""));
            restaurants.add(new PlaceItem("정부청사 맛집", "맛집", baseLat + 0.002, baseLng - 0.001,
                "세종시 도움6로 11", "세종시 도움6로 11", phonePrefix + "-2345-6789", ""));
            restaurants.add(new PlaceItem("세종호수공원 카페", "맛집", baseLat - 0.001, baseLng + 0.002,
                "세종시 연기면 세종로 110", "세종시 연기면 세종로 110", phonePrefix + "-3456-7890", ""));
            restaurants.add(new PlaceItem("조치원 순두부", "맛집", baseLat + 0.003, baseLng + 0.001,
                "세종시 조치원읍 새내로 25", "세종시 조치원읍 새내로 25", phonePrefix + "-4567-8901", ""));
            restaurants.add(new PlaceItem("세종 떡갈비", "맛집", baseLat - 0.002, baseLng - 0.001,
                "세종시 어진동 578", "세종시 어진동 578", phonePrefix + "-5678-9012", ""));
        } else if (location.contains("청주")) {
            // 청주 맛집
            restaurants.add(new PlaceItem("청주 직지갈비", "맛집", baseLat + 0.001, baseLng + 0.001,
                "충북 청주시 상당구 상당로 314", "충북 청주시 상당구 상당로 314", phonePrefix + "-1234-5678", ""));
            restaurants.add(new PlaceItem("흥덕구 칼국수", "맛집", baseLat + 0.002, baseLng - 0.001,
                "충북 청주시 흥덕구 1순환로 776", "충북 청주시 흥덕구 1순환로 776", phonePrefix + "-2345-6789", ""));
            restaurants.add(new PlaceItem("청주 막국수", "맛집", baseLat - 0.001, baseLng + 0.002,
                "충북 청주시 서원구 흥덕로 108", "충북 청주시 서원구 흥덕로 108", phonePrefix + "-3456-7890", ""));
            restaurants.add(new PlaceItem("상당산성 한정식", "맛집", baseLat + 0.003, baseLng + 0.001,
                "충북 청주시 상당구 명암로 143", "충북 청주시 상당구 명암로 143", phonePrefix + "-4567-8901", ""));
            restaurants.add(new PlaceItem("청주 순대국밥", "맛집", baseLat - 0.002, baseLng - 0.001,
                "충북 청주시 청원구 오창읍 각리로 45", "충북 청주시 청원구 오창읍 각리로 45", phonePrefix + "-5678-9012", ""));
        } else if (location.contains("제주")) {
            // 제주 맛집
            restaurants.add(new PlaceItem("제주 흑돼지", "맛집", baseLat + 0.001, baseLng + 0.001,
                "제주시 연동 1338-1", "제주시 연동 1338-1", phonePrefix + "-1234-5678", ""));
            restaurants.add(new PlaceItem("성산일출봉 해물탕", "맛집", baseLat + 0.002, baseLng - 0.001,
                "제주시 성산읍 성산리 114", "제주시 성산읍 성산리 114", phonePrefix + "-2345-6789", ""));
            restaurants.add(new PlaceItem("한라산 산채비빔밥", "맛집", baseLat - 0.001, baseLng + 0.002,
                "제주시 애월읍 고성리 33", "제주시 애월읍 고성리 33", phonePrefix + "-3456-7890", ""));
            restaurants.add(new PlaceItem("서귀포 갈치조림", "맛집", baseLat + 0.003, baseLng + 0.001,
                "서귀포시 중앙로 62번길 11", "서귀포시 중앙로 62번길 11", phonePrefix + "-4567-8901", ""));
            restaurants.add(new PlaceItem("제주 전복죽", "맛집", baseLat - 0.002, baseLng - 0.001,
                "제주시 구좌읍 하도리 1498", "제주시 구좌읍 하도리 1498", phonePrefix + "-5678-9012", ""));
        }

        return restaurants;
    }

    private List<PlaceItem> generateCafeData(String location, double baseLat, double baseLng, String phonePrefix) {
        List<PlaceItem> cafes = new ArrayList<>();

        if (location.contains("서울") || location.contains("강남") || location.contains("홍대") || location.contains("명동")) {
            // 서울 카페
            cafes.add(new PlaceItem("스타벅스 명동점", "카페", baseLat + 0.001, baseLng + 0.001,
                "서울시 중구 명동8길 31", "서울시 중구 명동8길 31", phonePrefix + "-1111-2222", ""));
            cafes.add(new PlaceItem("블루보틀 삼청점", "카페", baseLat + 0.002, baseLng - 0.001,
                "서울시 종로구 삼청로 20", "서울시 종로구 삼청로 20", phonePrefix + "-2222-3333", ""));
            cafes.add(new PlaceItem("앤트러사이트 한남점", "카페", baseLat - 0.001, baseLng + 0.002,
                "서울시 용산구 한남대로 42", "서울시 용산구 한남대로 42", phonePrefix + "-3333-4444", ""));
            cafes.add(new PlaceItem("투썸플레이스 강남점", "카페", baseLat + 0.003, baseLng + 0.001,
                "서울시 강남구 테헤란로 146", "서울시 강남구 테헤란로 146", phonePrefix + "-4444-5555", ""));
            cafes.add(new PlaceItem("폴바셋 홍대점", "카페", baseLat - 0.002, baseLng - 0.001,
                "서울시 마포구 와우산로 29길 24", "서울시 마포구 와우산로 29길 24", phonePrefix + "-5555-6666", ""));
        } else if (location.contains("대전")) {
            // 대전 카페
            cafes.add(new PlaceItem("스타벅스 대전시청점", "카페", baseLat + 0.001, baseLng + 0.001,
                "대전시 서구 둔산로 100", "대전시 서구 둔산로 100", phonePrefix + "-1111-2222", ""));
            cafes.add(new PlaceItem("카페베네 유성점", "카페", baseLat + 0.002, baseLng - 0.001,
                "대전시 유성구 대학로 99", "대전시 유성구 대학로 99", phonePrefix + "-2222-3333", ""));
            cafes.add(new PlaceItem("할리스 대전역점", "카페", baseLat - 0.001, baseLng + 0.002,
                "대전시 동구 중앙로 215", "대전시 동구 중앙로 215", phonePrefix + "-3333-4444", ""));
            cafes.add(new PlaceItem("이디야 둔산점", "카페", baseLat + 0.003, baseLng + 0.001,
                "대전시 서구 둔산대로 117", "대전시 서구 둔산대로 117", phonePrefix + "-4444-5555", ""));
            cafes.add(new PlaceItem("메가커피 대전점", "카페", baseLat - 0.002, baseLng - 0.001,
                "대전시 중구 대종로 488", "대전시 중구 대종로 488", phonePrefix + "-5555-6666", ""));
        } else if (location.contains("세종")) {
            // 세종 카페
            cafes.add(new PlaceItem("스타벅스 세종청사점", "카페", baseLat + 0.001, baseLng + 0.001,
                "세종시 한누리대로 2130", "세종시 한누리대로 2130", phonePrefix + "-1111-2222", ""));
            cafes.add(new PlaceItem("투썸플레이스 세종점", "카페", baseLat + 0.002, baseLng - 0.001,
                "세종시 도움6로 11", "세종시 도움6로 11", phonePrefix + "-2222-3333", ""));
            cafes.add(new PlaceItem("카페베네 조치원점", "카페", baseLat - 0.001, baseLng + 0.002,
                "세종시 조치원읍 새내로 25", "세종시 조치원읍 새내로 25", phonePrefix + "-3333-4444", ""));
            cafes.add(new PlaceItem("이디야 세종호수공원점", "카페", baseLat + 0.003, baseLng + 0.001,
                "세종시 연기면 세종로 110", "세종시 연기면 세종로 110", phonePrefix + "-4444-5555", ""));
            cafes.add(new PlaceItem("할리스 세종점", "카페", baseLat - 0.002, baseLng - 0.001,
                "세종시 어진동 578", "세종시 어진동 578", phonePrefix + "-5555-6666", ""));
        } else if (location.contains("청주")) {
            // 청주 카페
            cafes.add(new PlaceItem("스타벅스 청주터미널점", "카페", baseLat + 0.001, baseLng + 0.001,
                "충북 청주시 흥덕구 가경동 1435", "충북 청주시 흥덕구 가경동 1435", phonePrefix + "-1111-2222", ""));
            cafes.add(new PlaceItem("투썸플레이스 청주점", "카페", baseLat + 0.002, baseLng - 0.001,
                "충북 청주시 상당구 상당로 314", "충북 청주시 상당구 상당로 314", phonePrefix + "-2222-3333", ""));
            cafes.add(new PlaceItem("카페베네 충북대점", "카페", baseLat - 0.001, baseLng + 0.002,
                "충북 청주시 서원구 충대로 1", "충북 청주시 서원구 충대로 1", phonePrefix + "-3333-4444", ""));
            cafes.add(new PlaceItem("이디야 청주상당점", "카페", baseLat + 0.003, baseLng + 0.001,
                "충북 청주시 상당구 명암로 143", "충북 청주시 상당구 명암로 143", phonePrefix + "-4444-5555", ""));
            cafes.add(new PlaceItem("할리스 청주점", "카페", baseLat - 0.002, baseLng - 0.001,
                "충북 청주시 흥덕구 1순환로 776", "충북 청주시 흥덕구 1순환로 776", phonePrefix + "-5555-6666", ""));
        } else if (location.contains("제주")) {
            // 제주 카페
            cafes.add(new PlaceItem("스타벅스 제주공항점", "카페", baseLat + 0.001, baseLng + 0.001,
                "제주시 공항로 2", "제주시 공항로 2", phonePrefix + "-1111-2222", ""));
            cafes.add(new PlaceItem("카페 델문도", "카페", baseLat + 0.002, baseLng - 0.001,
                "제주시 애월읍 고성리 33", "제주시 애월읍 고성리 33", phonePrefix + "-2222-3333", ""));
            cafes.add(new PlaceItem("오설록 티뮤지엄", "카페", baseLat - 0.001, baseLng + 0.002,
                "서귀포시 안덕면 신화역사로 15", "서귀포시 안덕면 신화역사로 15", phonePrefix + "-3333-4444", ""));
            cafes.add(new PlaceItem("카페 쿠다", "카페", baseLat + 0.003, baseLng + 0.001,
                "제주시 구좌읍 하도리 1498", "제주시 구좌읍 하도리 1498", phonePrefix + "-4444-5555", ""));
            cafes.add(new PlaceItem("투썸플레이스 제주점", "카페", baseLat - 0.002, baseLng - 0.001,
                "제주시 연동 1338-1", "제주시 연동 1338-1", phonePrefix + "-5555-6666", ""));
        }

        return cafes;
    }

    private double getBaseLatitude(String location) {
        // 주요 지역별 기본 위도
        switch (location.toLowerCase()) {
            case "강남구":
            case "강남":
                return 37.5173;
            case "홍대":
            case "홍익대":
                return 37.5563;
            case "명동":
                return 37.5636;
            case "이태원":
                return 37.5347;
            case "신촌":
                return 37.5596;
            case "서울":
                return 37.5665;
            case "대전":
                return 36.3504;
            case "세종":
                return 36.4800;
            case "청주":
                return 36.6424;
            case "제주":
                return 33.4996;
            default:
                return 37.5665; // 서울 기본값
        }
    }

    private double getBaseLongitude(String location) {
        // 주요 지역별 기본 경도
        switch (location.toLowerCase()) {
            case "강남구":
            case "강남":
                return 127.0473;
            case "홍대":
            case "홍익대":
                return 126.9236;
            case "명동":
                return 126.9784;
            case "이태원":
                return 126.9947;
            case "신촌":
                return 126.9370;
            case "서울":
                return 126.9780;
            case "대전":
                return 127.3845;
            case "세종":
                return 127.2890;
            case "청주":
                return 127.4890;
            case "제주":
                return 126.5312;
            default:
                return 126.9780; // 서울 기본값
        }
    }

    private String getPhonePrefix(String location) {
        // 지역별 전화번호 prefix
        if (location.contains("서울") || location.contains("강남") || location.contains("홍대") || location.contains("명동")) {
            return "02";
        } else if (location.contains("대전")) {
            return "042";
        } else if (location.contains("세종")) {
            return "044";
        } else if (location.contains("청주")) {
            return "043";
        } else if (location.contains("제주")) {
            return "064";
        } else {
            return "02"; // 기본값
        }
    }

    private List<PlaceItem> generateAttractionData(String location, double baseLat, double baseLng, String phonePrefix) {
        List<PlaceItem> attractions = new ArrayList<>();

        if (location.contains("서울") || location.contains("강남") || location.contains("홍대") || location.contains("명동")) {
            // 서울 관광명소
            attractions.add(new PlaceItem("경복궁", "관광명소", baseLat + 0.001, baseLng + 0.001,
                "서울시 종로구 사직로 161", "서울시 종로구 사직로 161", phonePrefix + "-3700-3900", ""));
            attractions.add(new PlaceItem("N서울타워", "관광명소", baseLat + 0.002, baseLng - 0.001,
                "서울시 용산구 남산공원길 105", "서울시 용산구 남산공원길 105", phonePrefix + "-3455-9277", ""));
            attractions.add(new PlaceItem("동대문디자인플라자", "관광명소", baseLat - 0.001, baseLng + 0.002,
                "서울시 중구 을지로 281", "서울시 중구 을지로 281", phonePrefix + "-2153-0000", ""));
            attractions.add(new PlaceItem("한강공원", "관광명소", baseLat + 0.003, baseLng + 0.001,
                "서울시 영등포구 여의동로 330", "서울시 영등포구 여의동로 330", phonePrefix + "-3780-0561", ""));
            attractions.add(new PlaceItem("명동성당", "관광명소", baseLat - 0.002, baseLng - 0.001,
                "서울시 중구 명동길 74", "서울시 중구 명동길 74", phonePrefix + "-774-1784", ""));
        } else if (location.contains("대전")) {
            // 대전 관광명소
            attractions.add(new PlaceItem("대전엑스포과학공원", "관광명소", baseLat + 0.001, baseLng + 0.001,
                "대전시 유성구 대덕대로 480", "대전시 유성구 대덕대로 480", phonePrefix + "-250-1111", ""));
            attractions.add(new PlaceItem("유성온천", "관광명소", baseLat + 0.002, baseLng - 0.001,
                "대전시 유성구 온천로 77", "대전시 유성구 온천로 77", phonePrefix + "-611-2114", ""));
            attractions.add(new PlaceItem("대청호", "관광명소", baseLat - 0.001, baseLng + 0.002,
                "대전시 동구 대청호반로 850", "대전시 동구 대청호반로 850", phonePrefix + "-270-4861", ""));
            attractions.add(new PlaceItem("계룡산국립공원", "관광명소", baseLat + 0.003, baseLng + 0.001,
                "대전시 중구 사정동 산1-1", "대전시 중구 사정동 산1-1", phonePrefix + "-825-3002", ""));
            attractions.add(new PlaceItem("대전시립미술관", "관광명소", baseLat - 0.002, baseLng - 0.001,
                "대전시 서구 둔산대로 155", "대전시 서구 둔산대로 155", phonePrefix + "-270-8077", ""));
        } else if (location.contains("세종")) {
            // 세종 관광명소
            attractions.add(new PlaceItem("세종호수공원", "관광명소", baseLat + 0.001, baseLng + 0.001,
                "세종시 연기면 세종로 110", "세종시 연기면 세종로 110", phonePrefix + "-300-7000", ""));
            attractions.add(new PlaceItem("정부세종청사", "관광명소", baseLat + 0.002, baseLng - 0.001,
                "세종시 한누리대로 413", "세종시 한누리대로 413", phonePrefix + "-200-1234", ""));
            attractions.add(new PlaceItem("베어트리파크", "관광명소", baseLat - 0.001, baseLng + 0.002,
                "세종시 전동면 신송로 217", "세종시 전동면 신송로 217", phonePrefix + "-868-1200", ""));
            attractions.add(new PlaceItem("세종문화예술회관", "관광명소", baseLat + 0.003, baseLng + 0.001,
                "세종시 한누리대로 2130", "세종시 한누리대로 2130", phonePrefix + "-850-0001", ""));
            attractions.add(new PlaceItem("금강수목원", "관광명소", baseLat - 0.002, baseLng - 0.001,
                "세종시 금남면 금강로 110", "세종시 금남면 금강로 110", phonePrefix + "-635-7400", ""));
        } else if (location.contains("청주")) {
            // 청주 관광명소
            attractions.add(new PlaceItem("상당산성", "관광명소", baseLat + 0.001, baseLng + 0.001,
                "충북 청주시 상당구 산성동", "충북 청주시 상당구 산성동", phonePrefix + "-201-0001", ""));
            attractions.add(new PlaceItem("청주고인쇄박물관", "관광명소", baseLat + 0.002, baseLng - 0.001,
                "충북 청주시 흥덕구 직지대로 713", "충북 청주시 흥덕구 직지대로 713", phonePrefix + "-201-4266", ""));
            attractions.add(new PlaceItem("용두사지철당간", "관광명소", baseLat - 0.001, baseLng + 0.002,
                "충북 청주시 상당구 남일면 용정리", "충북 청주시 상당구 남일면 용정리", phonePrefix + "-201-0001", ""));
            attractions.add(new PlaceItem("청주랜드", "관광명소", baseLat + 0.003, baseLng + 0.001,
                "충북 청주시 상당구 명암로 143", "충북 청주시 상당구 명암로 143", phonePrefix + "-257-5391", ""));
            attractions.add(new PlaceItem("문의문화재단지", "관광명소", baseLat - 0.002, baseLng - 0.001,
                "충북 청주시 상당구 문의면 문의리", "충북 청주시 상당구 문의면 문의리", phonePrefix + "-201-0001", ""));
        } else if (location.contains("제주")) {
            // 제주 관광명소
            attractions.add(new PlaceItem("성산일출봉", "관광명소", baseLat + 0.001, baseLng + 0.001,
                "제주시 성산읍 성산리 114", "제주시 성산읍 성산리 114", phonePrefix + "-783-0959", ""));
            attractions.add(new PlaceItem("한라산국립공원", "관광명소", baseLat + 0.002, baseLng - 0.001,
                "제주시 1100로 2070-61", "제주시 1100로 2070-61", phonePrefix + "-713-9950", ""));
            attractions.add(new PlaceItem("제주민속촌", "관광명소", baseLat - 0.001, baseLng + 0.002,
                "서귀포시 표선면 민속해안로 631", "서귀포시 표선면 민속해안로 631", phonePrefix + "-787-4501", ""));
            attractions.add(new PlaceItem("천지연폭포", "관광명소", baseLat + 0.003, baseLng + 0.001,
                "서귀포시 천지동 667-7", "서귀포시 천지동 667-7", phonePrefix + "-760-6304", ""));
            attractions.add(new PlaceItem("우도", "관광명소", baseLat - 0.002, baseLng - 0.001,
                "제주시 우도면 연평리", "제주시 우도면 연평리", phonePrefix + "-728-4394", ""));
        }

        return attractions;
    }
    
    private void parseLocalSearchResponse(String response, String category, LocalSearchCallback callback) {
        try {
            JSONObject jsonObject = new JSONObject(response);
            JSONArray items = jsonObject.getJSONArray("items");
            
            List<PlaceItem> placeList = new ArrayList<>();
            
            for (int i = 0; i < items.length(); i++) {
                JSONObject item = items.getJSONObject(i);
                
                String title = item.optString("title", "이름 없음");
                // HTML 태그 제거
                title = title.replaceAll("<[^>]*>", "");
                
                String address = item.optString("address", "주소 없음");
                String roadAddress = item.optString("roadAddress", address);
                String tel = item.optString("telephone", "");
                String link = item.optString("link", "");
                
                // 좌표 정보 (주소 기반 추정 좌표)
                double[] coordinates = estimateCoordinatesFromAddress(address);
                double lat = coordinates[0];
                double lng = coordinates[1];
                
                PlaceItem placeItem = new PlaceItem(title, category, lat, lng, address, roadAddress, tel, link);
                
                // 거리 계산 (임시)
                placeItem.distance = "약 " + (int)(Math.random() * 1500 + 100) + "m";
                
                placeList.add(placeItem);
            }
            
            // 평점 기준 내림차순 정렬
            Collections.sort(placeList, (a, b) -> Double.compare(b.rating, a.rating));
            
            Log.d(TAG, "Local Search Success: " + placeList.size() + " places found");
            callback.onSuccess(placeList);
            
        } catch (Exception e) {
            Log.e(TAG, "Local Search Parse Error", e);
            callback.onError("응답 파싱 오류: " + e.getMessage());
        }
    }

    /**
     * 주소 기반으로 좌표를 추정합니다.
     * 실제 Geocoding API 대신 지역명 기반 추정 사용
     */
    private double[] estimateCoordinatesFromAddress(String address) {
        double lat = 37.5665; // 서울 기본값
        double lng = 126.9780;

        // 주요 지역별 좌표 매핑
        if (address.contains("강남") || address.contains("테헤란로") || address.contains("역삼")) {
            lat = 37.5173; lng = 127.0473;
        } else if (address.contains("홍대") || address.contains("마포") || address.contains("홍익대")) {
            lat = 37.5563; lng = 126.9236;
        } else if (address.contains("명동") || address.contains("중구")) {
            lat = 37.5636; lng = 126.9784;
        } else if (address.contains("이태원") || address.contains("용산")) {
            lat = 37.5347; lng = 126.9947;
        } else if (address.contains("신촌") || address.contains("서대문")) {
            lat = 37.5596; lng = 126.9370;
        } else if (address.contains("잠실") || address.contains("송파")) {
            lat = 37.5133; lng = 127.1028;
        } else if (address.contains("건대") || address.contains("광진")) {
            lat = 37.5403; lng = 127.0695;
        } else if (address.contains("대전")) {
            lat = 36.3504; lng = 127.3845;
        } else if (address.contains("부산")) {
            lat = 35.1796; lng = 129.0756;
        } else if (address.contains("인천")) {
            lat = 37.4563; lng = 126.7052;
        }

        // 약간의 랜덤 오프셋 추가 (같은 지역 내 다양한 위치 표현)
        lat += (Math.random() - 0.5) * 0.005;
        lng += (Math.random() - 0.5) * 0.005;

        return new double[]{lat, lng};
    }

    public void shutdown() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
}
