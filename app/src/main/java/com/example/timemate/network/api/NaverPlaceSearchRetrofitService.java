package com.example.timemate.network.api;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.timemate.network.api.NaverPlaceKeywordService.PlaceItem;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Query;

/**
 * 네이버 개발자 센터 Local Search API 서비스
 * 실시간 자동완성을 위한 장소 + POI 검색
 */
public class NaverPlaceSearchRetrofitService {

    private static final String TAG = "NaverPlaceSearchRetrofit";
    private static final String BASE_URL = "https://openapi.naver.com/";
    private static final String CLIENT_ID = com.example.timemate.config.ApiConfig.NAVER_DEV_CLIENT_ID;
    private static final String CLIENT_SECRET = com.example.timemate.config.ApiConfig.NAVER_DEV_CLIENT_SECRET;

    private NaverPlaceSearchApi apiService;
    private ExecutorService executor;
    private Handler mainHandler;

    // Debounce를 위한 변수들
    private Handler debounceHandler;
    private Runnable lastSearchRunnable;

    /**
     * 네이버 개발자 센터 Local Search API 인터페이스
     */
    public interface NaverPlaceSearchApi {
        @GET("v1/search/local.json")
        Call<LocalSearchResponse> searchPlaces(
                @Header("X-Naver-Client-Id") String clientId,
                @Header("X-Naver-Client-Secret") String clientSecret,
                @Query(value = "query", encoded = true) String query,
                @Query("display") int display,
                @Query("start") int start,
                @Query("sort") String sort
        );
    }

    /**
     * 네이버 Local Search API 응답 데이터 클래스
     */
    public static class LocalSearchResponse {
        public String lastBuildDate;
        public int total;
        public int start;
        public int display;
        public LocalItem[] items;

        public static class LocalItem {
            public String title;
            public String link;
            public String category;
            public String description;
            public String telephone;
            public String address;
            public String roadAddress;
            public int mapx; // 경도 (KATEC X 좌표)
            public int mapy; // 위도 (KATEC Y 좌표)
        }
    }

    public interface PlaceSearchCallback {
        void onSuccess(List<PlaceItem> places);
        void onError(String error);
    }

    public NaverPlaceSearchRetrofitService() {
        // Retrofit 클라이언트 설정
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        apiService = retrofit.create(NaverPlaceSearchApi.class);
        executor = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());
        debounceHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * 키워드로 장소 검색 (Debounce 적용)
     * @param keyword 검색 키워드
     * @param callback 결과 콜백
     */
    public void searchPlacesWithDebounce(String keyword, PlaceSearchCallback callback) {
        // 이전 검색 요청 취소
        if (lastSearchRunnable != null) {
            debounceHandler.removeCallbacks(lastSearchRunnable);
        }

        // 300ms 지연 후 검색 실행
        lastSearchRunnable = () -> searchPlaces(keyword, callback);
        debounceHandler.postDelayed(lastSearchRunnable, 300);
    }

    /**
     * 즉시 장소 검색
     * @param keyword 검색 키워드
     * @param callback 결과 콜백
     */
    public void searchPlaces(String keyword, PlaceSearchCallback callback) {
        if (keyword == null || keyword.trim().length() < 2) {
            callback.onSuccess(new ArrayList<>());
            return;
        }

        executor.execute(() -> {
            try {
                // UTF-8 인코딩
                String encodedQuery = URLEncoder.encode(keyword.trim(), "UTF-8");
                
                Log.d(TAG, "=== Naver Place Search API Call ===");
                Log.d(TAG, "Original keyword: " + keyword);
                Log.d(TAG, "Encoded query: " + encodedQuery);
                Log.d(TAG, "Base URL: " + BASE_URL);
                Log.d(TAG, "Client ID: " + CLIENT_ID);

                // UTF-8 인코딩 검증
                if (encodedQuery.contains("%")) {
                    Log.d(TAG, "✓ UTF-8 encoding verified (contains %)");
                } else {
                    Log.w(TAG, "⚠ UTF-8 encoding may be missing");
                }

                // 네이버 Local Search API 호출
                Call<LocalSearchResponse> call = apiService.searchPlaces(
                        CLIENT_ID,
                        CLIENT_SECRET,
                        encodedQuery,
                        10, // display: 최대 10개 결과
                        1,  // start: 첫 번째 페이지
                        "random" // sort: 정확도순
                );

                call.enqueue(new Callback<LocalSearchResponse>() {
                    @Override
                    public void onResponse(Call<LocalSearchResponse> call, Response<LocalSearchResponse> response) {
                        Log.d(TAG, "=== API Response Received ===");
                        Log.d(TAG, "Response Code: " + response.code());

                        if (response.isSuccessful() && response.body() != null) {
                            LocalSearchResponse localResponse = response.body();

                            Log.d(TAG, "Total Count: " + localResponse.total);
                            Log.d(TAG, "Display Count: " + localResponse.display);
                            Log.d(TAG, "✓ Search successful with " + localResponse.total + " results");

                            List<PlaceItem> places = parseLocalSearchResponse(localResponse, keyword);

                            mainHandler.post(() -> callback.onSuccess(places));
                        } else {
                            Log.e(TAG, "❌ API Response Error: " + response.code() + " - " + response.message());

                            // 오류 응답 본문 로깅
                            try {
                                if (response.errorBody() != null) {
                                    String errorBody = response.errorBody().string();
                                    Log.e(TAG, "Error Body: " + errorBody);
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error reading error body", e);
                            }

                            mainHandler.post(() -> callback.onError("검색 실패: " + response.code()));
                        }
                    }

                    @Override
                    public void onFailure(Call<LocalSearchResponse> call, Throwable t) {
                        Log.e(TAG, "API Call Failed", t);
                        mainHandler.post(() -> callback.onError("네트워크 오류: " + t.getMessage()));
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "Search Exception", e);
                mainHandler.post(() -> callback.onError("검색 오류: " + e.getMessage()));
            }
        });
    }

    /**
     * Local Search API 응답을 PlaceItem 리스트로 변환
     */
    private List<PlaceItem> parseLocalSearchResponse(LocalSearchResponse response, String keyword) {
        List<PlaceItem> places = new ArrayList<>();

        if (response.items != null && response.items.length > 0) {
            for (int i = 0; i < Math.min(response.items.length, 10); i++) {
                LocalSearchResponse.LocalItem item = response.items[i];

                try {
                    // HTML 태그 제거
                    String name = item.title != null ? item.title.replaceAll("<[^>]*>", "") : "";
                    String address = item.roadAddress != null && !item.roadAddress.isEmpty()
                                   ? item.roadAddress : item.address;

                    // KATEC 좌표를 WGS84로 변환 (간단한 근사 변환)
                    double latitude = convertKatecToWgs84Lat(item.mapy);
                    double longitude = convertKatecToWgs84Lng(item.mapx);

                    PlaceItem placeItem = new PlaceItem(
                            name,
                            address,
                            item.roadAddress != null ? item.roadAddress : "",
                            latitude,
                            longitude,
                            item.category != null ? item.category : "장소"
                    );

                    placeItem.tel = item.telephone;
                    places.add(placeItem);
                } catch (Exception e) {
                    Log.w(TAG, "Error parsing place: " + item.title, e);
                }
            }
        }

        Log.d(TAG, "Parsed " + places.size() + " places from response");
        return places;
    }

    /**
     * KATEC Y 좌표를 WGS84 위도로 변환 (근사)
     */
    private double convertKatecToWgs84Lat(int katecY) {
        return katecY / 1000000.0;
    }

    /**
     * KATEC X 좌표를 WGS84 경도로 변환 (근사)
     */
    private double convertKatecToWgs84Lng(int katecX) {
        return katecX / 1000000.0;
    }

    /**
     * 서비스 종료
     */
    public void shutdown() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
        if (debounceHandler != null) {
            debounceHandler.removeCallbacksAndMessages(null);
        }
    }
}
