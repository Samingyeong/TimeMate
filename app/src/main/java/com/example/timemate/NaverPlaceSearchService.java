package com.example.timemate;

import android.util.Log;

// import org.json.JSONArray;
// import org.json.JSONObject;
// import java.io.BufferedReader;
// import java.io.InputStreamReader;
// import java.net.HttpURLConnection;
// import java.net.URL;
// import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NaverPlaceSearchService {
    
    private static final String TAG = "NaverPlaceSearch";

    // 네이버 Place Search API는 유료 구독이 필요합니다.
    // 현재는 더미 데이터를 사용하여 기능을 시연합니다.
    // 실제 사용 시 아래 API 키를 설정하고 searchNearbyPlaces 메서드를 수정하세요.
    private static final String CLIENT_ID = "YOUR_CLIENT_ID"; // 실제 클라이언트 ID로 교체
    private static final String CLIENT_SECRET = "YOUR_CLIENT_SECRET"; // 실제 클라이언트 시크릿으로 교체
    private static final String SEARCH_URL = "https://naveropenapi.apigw.ntruss.com/map-place/v1/search";
    
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    public interface PlaceSearchCallback {
        void onSuccess(List<PlaceItem> places);
        void onError(String error);
    }

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
    }

    public void searchNearbyPlaces(double latitude, double longitude, String category, PlaceSearchCallback callback) {
        executor.execute(() -> {
            try {
                // API 구독이 필요하므로 더미 데이터로 대체
                Log.d(TAG, "Using dummy data for category: " + category + " at location: " + latitude + ", " + longitude);

                // 더미 데이터 생성
                List<PlaceItem> dummyPlaces = generateDummyPlaces(category, latitude, longitude);

                Log.d(TAG, "Generated " + dummyPlaces.size() + " dummy places for category: " + category);

                // 약간의 지연으로 실제 API 호출처럼 보이게 함
                Thread.sleep(500);

                callback.onSuccess(dummyPlaces);

            } catch (Exception e) {
                Log.e(TAG, "Place Search Exception", e);
                callback.onError("네트워크 오류: " + e.getMessage());
            }
        });
    }
    
    private String getQueryByCategory(String category) {
        switch (category) {
            case "맛집":
                return "맛집 음식점";
            case "카페":
                return "카페 커피";
            case "관광명소":
                return "관광지 명소";
            default:
                return "맛집";
        }
    }

    private List<PlaceItem> generateDummyPlaces(String category, double baseLat, double baseLng) {
        List<PlaceItem> places = new ArrayList<>();

        switch (category) {
            case "맛집":
                places.add(new PlaceItem("맛있는 한식당", "맛집", baseLat + 0.001, baseLng + 0.001,
                    "서울시 강남구 테헤란로 123", 4.5, "약 150m"));
                places.add(new PlaceItem("이탈리안 레스토랑", "맛집", baseLat + 0.002, baseLng - 0.001,
                    "서울시 강남구 역삼동 456", 4.3, "약 280m"));
                places.add(new PlaceItem("일본식 라멘집", "맛집", baseLat - 0.001, baseLng + 0.002,
                    "서울시 강남구 삼성동 789", 4.7, "약 320m"));
                places.add(new PlaceItem("중국집 맛집", "맛집", baseLat + 0.003, baseLng + 0.001,
                    "서울시 강남구 논현동 321", 4.2, "약 450m"));
                places.add(new PlaceItem("분식집", "맛집", baseLat - 0.002, baseLng - 0.001,
                    "서울시 강남구 신사동 654", 4.4, "약 380m"));
                break;

            case "카페":
                places.add(new PlaceItem("스타벅스 강남점", "카페", baseLat + 0.001, baseLng - 0.002,
                    "서울시 강남구 테헤란로 111", 4.1, "약 200m"));
                places.add(new PlaceItem("투썸플레이스", "카페", baseLat - 0.001, baseLng + 0.001,
                    "서울시 강남구 역삼동 222", 4.3, "약 180m"));
                places.add(new PlaceItem("블루보틀 커피", "카페", baseLat + 0.002, baseLng + 0.002,
                    "서울시 강남구 삼성동 333", 4.6, "약 350m"));
                places.add(new PlaceItem("로컬 카페", "카페", baseLat - 0.002, baseLng + 0.003,
                    "서울시 강남구 논현동 444", 4.4, "약 420m"));
                places.add(new PlaceItem("디저트 카페", "카페", baseLat + 0.003, baseLng - 0.001,
                    "서울시 강남구 신사동 555", 4.5, "약 480m"));
                break;

            case "관광명소":
                places.add(new PlaceItem("코엑스 아쿠아리움", "관광명소", baseLat + 0.005, baseLng + 0.003,
                    "서울시 강남구 영동대로 513", 4.2, "약 800m"));
                places.add(new PlaceItem("봉은사", "관광명소", baseLat - 0.003, baseLng + 0.004,
                    "서울시 강남구 봉은사로 531", 4.4, "약 650m"));
                places.add(new PlaceItem("선릉", "관광명소", baseLat + 0.004, baseLng - 0.002,
                    "서울시 강남구 선릉로 100길", 4.1, "약 720m"));
                places.add(new PlaceItem("강남역 지하상가", "관광명소", baseLat - 0.001, baseLng - 0.003,
                    "서울시 강남구 강남대로 지하", 3.9, "약 300m"));
                places.add(new PlaceItem("가로수길", "관광명소", baseLat + 0.002, baseLng + 0.005,
                    "서울시 강남구 신사동 가로수길", 4.3, "약 900m"));
                break;
        }

        // 평점 기준 내림차순 정렬
        Collections.sort(places, (a, b) -> Double.compare(b.rating, a.rating));

        return places;
    }
    
    // 더미 데이터 사용으로 인해 파싱 메서드는 더 이상 필요하지 않음
    // 실제 API 사용 시 이 메서드를 다시 활성화하면 됨
    
    public void shutdown() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
}
