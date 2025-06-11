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

    // 네이버 클라우드 플랫폼 Geocoding API 사용
    private static final String GEOCODING_URL = "https://maps.apigw.ntruss.com/map-geocode/v2/geocode";

    // API 사용 가능 여부 (네이버 클라우드 플랫폼 API 사용)
    private static final boolean USE_REAL_API = true;

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
        public String title;         // 장소명 (호환성)
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
            this.title = name; // 호환성을 위해 title도 설정
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
            if (USE_REAL_API) {
                // 실제 API 호출
                performRealApiSearch(keyword, callback);
            } else {
                // 더미 데이터 사용
                try {
                    Log.d(TAG, "키워드 검색 더미 데이터 사용: " + keyword);

                    List<PlaceItem> dummyPlaces = generateKeywordDummyData(keyword.trim());

                    // 실제 API 호출처럼 지연
                    Thread.sleep(300);

                    callback.onSuccess(dummyPlaces);

                } catch (Exception e) {
                    Log.e(TAG, "Dummy Data Exception", e);
                    callback.onError("검색 오류: " + e.getMessage());
                }
            }
        });
    }

    private void performRealApiSearch(String keyword, PlaceKeywordCallback callback) {
        try {
            // 네이버 클라우드 플랫폼 Geocoding API 사용
            String encodedKeyword = URLEncoder.encode(keyword.trim(), "UTF-8");
            String urlString = GEOCODING_URL + "?query=" + encodedKeyword;

            Log.d(TAG, "Geocoding API URL: " + urlString);

            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("X-NCP-APIGW-API-KEY-ID", CLIENT_ID);
            connection.setRequestProperty("X-NCP-APIGW-API-KEY", CLIENT_SECRET);
            connection.setRequestProperty("Accept-Language", "ko");
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);

            int responseCode = connection.getResponseCode();
            Log.d(TAG, "Geocoding API Response Code: " + responseCode);

            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
                StringBuilder response = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                Log.d(TAG, "Geocoding API Response: " + response.toString());
                parseGeocodingResponse(response.toString(), keyword, callback);
            } else {
                BufferedReader errorReader = new BufferedReader(new InputStreamReader(connection.getErrorStream(), "UTF-8"));
                StringBuilder errorResponse = new StringBuilder();
                String errorLine;

                while ((errorLine = errorReader.readLine()) != null) {
                    errorResponse.append(errorLine);
                }
                errorReader.close();

                Log.e(TAG, "Geocoding API Error: " + errorResponse.toString());

                // API 실패 시 더미 데이터로 폴백
                Log.w(TAG, "Geocoding API failed, falling back to dummy data");
                List<PlaceItem> dummyPlaces = generateKeywordDummyData(keyword);
                callback.onSuccess(dummyPlaces);
            }

            connection.disconnect();

        } catch (Exception e) {
            Log.e(TAG, "Geocoding API Exception", e);
            // 예외 발생 시 더미 데이터로 폴백
            Log.w(TAG, "Exception occurred, falling back to dummy data");
            List<PlaceItem> dummyPlaces = generateKeywordDummyData(keyword);
            callback.onSuccess(dummyPlaces);
        }
    }

    /**
     * Geocoding API 응답 파싱
     */
    private void parseGeocodingResponse(String response, String keyword, PlaceKeywordCallback callback) {
        try {
            JSONObject jsonObject = new JSONObject(response);
            List<PlaceItem> placeList = new ArrayList<>();

            // addresses 배열 파싱
            if (jsonObject.has("addresses")) {
                JSONArray addresses = jsonObject.getJSONArray("addresses");

                for (int i = 0; i < addresses.length() && i < 5; i++) { // 최대 5개까지
                    JSONObject address = addresses.getJSONObject(i);

                    String jibunAddress = address.optString("jibunAddress", "");
                    String roadAddress = address.optString("roadAddress", "");
                    double lat = address.optDouble("y", 0.0);
                    double lng = address.optDouble("x", 0.0);

                    // 주소가 있고 좌표가 유효한 경우만 추가
                    if ((!jibunAddress.isEmpty() || !roadAddress.isEmpty()) && lat != 0.0 && lng != 0.0) {
                        String displayName = !roadAddress.isEmpty() ? roadAddress : jibunAddress;
                        String displayAddress = !roadAddress.isEmpty() ? roadAddress : jibunAddress;

                        // 키워드가 포함된 이름 생성
                        if (displayName.length() > 50) {
                            displayName = displayName.substring(0, 47) + "...";
                        }

                        PlaceItem item = new PlaceItem(displayName, jibunAddress, roadAddress, lat, lng, "주소");
                        placeList.add(item);
                    }
                }
            }

            // 결과가 없으면 더미 데이터 사용
            if (placeList.isEmpty()) {
                Log.w(TAG, "No geocoding results found, using dummy data");
                placeList = generateKeywordDummyData(keyword);
            }

            Log.d(TAG, "Geocoding Success: " + placeList.size() + " places found");
            callback.onSuccess(placeList);

        } catch (Exception e) {
            Log.e(TAG, "Geocoding Parse Error", e);
            // 파싱 오류 시 더미 데이터로 폴백
            List<PlaceItem> dummyPlaces = generateKeywordDummyData(keyword);
            callback.onSuccess(dummyPlaces);
        }
    }

    /**
     * 기존 Place API 응답 파싱 (사용하지 않음)
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
     * 키워드 기반 더미 데이터 생성
     */
    private List<PlaceItem> generateKeywordDummyData(String keyword) {
        List<PlaceItem> places = new ArrayList<>();
        String lowerKeyword = keyword.toLowerCase();

        // 지하철역 검색
        if (lowerKeyword.contains("역") || lowerKeyword.contains("지하철")) {
            places.add(new PlaceItem("서울역", "서울시 중구 세종대로 2", "서울시 중구 세종대로 2", 37.5547, 126.9706, "지하철역"));
            places.add(new PlaceItem("강남역", "서울시 강남구 강남대로 396", "서울시 강남구 강남대로 396", 37.4979, 127.0276, "지하철역"));
            places.add(new PlaceItem("홍대입구역", "서울시 마포구 양화로 188", "서울시 마포구 양화로 188", 37.5563, 126.9236, "지하철역"));
            places.add(new PlaceItem("명동역", "서울시 중구 명동길 26", "서울시 중구 명동길 26", 37.5636, 126.9784, "지하철역"));
            places.add(new PlaceItem("대전역", "대전시 동구 중앙로 215", "대전시 동구 중앙로 215", 36.3504, 127.3845, "기차역"));
        }
        // 대학교 검색
        else if (lowerKeyword.contains("대학") || lowerKeyword.contains("대")) {
            places.add(new PlaceItem("서울대학교", "서울시 관악구 관악로 1", "서울시 관악구 관악로 1", 37.4601, 126.9520, "대학교"));
            places.add(new PlaceItem("연세대학교", "서울시 서대문구 연세로 50", "서울시 서대문구 연세로 50", 37.5596, 126.9370, "대학교"));
            places.add(new PlaceItem("고려대학교", "서울시 성북구 안암로 145", "서울시 성북구 안암로 145", 37.5896, 127.0324, "대학교"));
            places.add(new PlaceItem("홍익대학교", "서울시 마포구 와우산로 94", "서울시 마포구 와우산로 94", 37.5563, 126.9236, "대학교"));
            places.add(new PlaceItem("충남대학교", "대전시 유성구 대학로 99", "대전시 유성구 대학로 99", 36.3504, 127.3845, "대학교"));
        }
        // 병원 검색
        else if (lowerKeyword.contains("병원") || lowerKeyword.contains("의료")) {
            places.add(new PlaceItem("서울대학교병원", "서울시 종로구 대학로 101", "서울시 종로구 대학로 101", 37.5796, 126.9997, "병원"));
            places.add(new PlaceItem("삼성서울병원", "서울시 강남구 일원로 81", "서울시 강남구 일원로 81", 37.4881, 127.0857, "병원"));
            places.add(new PlaceItem("세브란스병원", "서울시 서대문구 연세로 50-1", "서울시 서대문구 연세로 50-1", 37.5596, 126.9370, "병원"));
            places.add(new PlaceItem("아산병원", "서울시 송파구 올림픽로 43길 88", "서울시 송파구 올림픽로 43길 88", 37.5260, 127.1086, "병원"));
            places.add(new PlaceItem("충남대학교병원", "대전시 중구 문화로 282", "대전시 중구 문화로 282", 36.3504, 127.3845, "병원"));
        }
        // 공항 검색
        else if (lowerKeyword.contains("공항")) {
            places.add(new PlaceItem("인천국제공항", "인천시 중구 공항로 272", "인천시 중구 공항로 272", 37.4602, 126.4407, "공항"));
            places.add(new PlaceItem("김포공항", "서울시 강서구 하늘길 112", "서울시 강서구 하늘길 112", 37.5583, 126.7906, "공항"));
            places.add(new PlaceItem("제주국제공항", "제주시 공항로 2", "제주시 공항로 2", 33.5066, 126.4927, "공항"));
        }
        // 쇼핑몰 검색
        else if (lowerKeyword.contains("쇼핑") || lowerKeyword.contains("몰") || lowerKeyword.contains("백화점")) {
            places.add(new PlaceItem("롯데월드몰", "서울시 송파구 올림픽로 300", "서울시 송파구 올림픽로 300", 37.5133, 127.1028, "쇼핑몰"));
            places.add(new PlaceItem("코엑스몰", "서울시 강남구 영동대로 513", "서울시 강남구 영동대로 513", 37.5115, 127.0590, "쇼핑몰"));
            places.add(new PlaceItem("명동 롯데백화점", "서울시 중구 남대문로 81", "서울시 중구 남대문로 81", 37.5636, 126.9784, "백화점"));
            places.add(new PlaceItem("현대백화점 무역센터점", "서울시 강남구 테헤란로 517", "서울시 강남구 테헤란로 517", 37.5115, 127.0590, "백화점"));
            places.add(new PlaceItem("갤러리아백화점", "서울시 강남구 압구정로 343", "서울시 강남구 압구정로 343", 37.5273, 127.0286, "백화점"));
        }
        // 일반 키워드 검색 (지역명 포함)
        else {
            // 서울 지역
            if (lowerKeyword.contains("서울") || lowerKeyword.contains("강남") || lowerKeyword.contains("홍대")) {
                places.add(new PlaceItem("서울시청", "서울시 중구 세종대로 110", "서울시 중구 세종대로 110", 37.5665, 126.9780, "관공서"));
                places.add(new PlaceItem("강남구청", "서울시 강남구 학동로 426", "서울시 강남구 학동로 426", 37.5173, 127.0473, "관공서"));
                places.add(new PlaceItem("홍대 걷고싶은거리", "서울시 마포구 와우산로 29길", "서울시 마포구 와우산로 29길", 37.5563, 126.9236, "상업지구"));
            }
            // 대전 지역
            else if (lowerKeyword.contains("대전")) {
                places.add(new PlaceItem("대전시청", "대전시 서구 둔산로 100", "대전시 서구 둔산로 100", 36.3504, 127.3845, "관공서"));
                places.add(new PlaceItem("대전역", "대전시 동구 중앙로 215", "대전시 동구 중앙로 215", 36.3504, 127.3845, "기차역"));
                places.add(new PlaceItem("유성온천", "대전시 유성구 온천로 77", "대전시 유성구 온천로 77", 36.3504, 127.3845, "관광지"));
            }
            // 세종 지역
            else if (lowerKeyword.contains("세종")) {
                places.add(new PlaceItem("정부세종청사", "세종시 한누리대로 413", "세종시 한누리대로 413", 36.4800, 127.2890, "관공서"));
                places.add(new PlaceItem("세종호수공원", "세종시 연기면 세종로 110", "세종시 연기면 세종로 110", 36.4800, 127.2890, "공원"));
            }
            // 청주 지역
            else if (lowerKeyword.contains("청주")) {
                places.add(new PlaceItem("청주시청", "충북 청주시 상당구 상당로 155", "충북 청주시 상당구 상당로 155", 36.6424, 127.4890, "관공서"));
                places.add(new PlaceItem("상당산성", "충북 청주시 상당구 산성동", "충북 청주시 상당구 산성동", 36.6424, 127.4890, "관광지"));
            }
            // 제주 지역
            else if (lowerKeyword.contains("제주")) {
                places.add(new PlaceItem("제주시청", "제주시 삼도2동 1153-3", "제주시 삼도2동 1153-3", 33.4996, 126.5312, "관공서"));
                places.add(new PlaceItem("성산일출봉", "제주시 성산읍 성산리 114", "제주시 성산읍 성산리 114", 33.4996, 126.5312, "관광지"));
                places.add(new PlaceItem("한라산", "제주시 1100로 2070-61", "제주시 1100로 2070-61", 33.4996, 126.5312, "산"));
            }
            // 기본 검색 결과
            else {
                places.add(new PlaceItem(keyword + " 관련 장소 1", "서울시 중구 세종대로 110", "서울시 중구 세종대로 110", 37.5665, 126.9780, "기타"));
                places.add(new PlaceItem(keyword + " 관련 장소 2", "서울시 강남구 테헤란로 152", "서울시 강남구 테헤란로 152", 37.5173, 127.0473, "기타"));
                places.add(new PlaceItem(keyword + " 관련 장소 3", "대전시 서구 둔산로 100", "대전시 서구 둔산로 100", 36.3504, 127.3845, "기타"));
            }
        }

        return places;
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
