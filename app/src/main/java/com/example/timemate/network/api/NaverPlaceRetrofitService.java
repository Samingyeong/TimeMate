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

/**
 * Retrofit을 사용한 네이버 Place API 서비스
 * 실시간 자동완성을 위한 최적화된 API 클라이언트
 */
public class NaverPlaceRetrofitService {

    private static final String TAG = "NaverPlaceRetrofit";
    private static final String BASE_URL = "https://maps.apigw.ntruss.com/";
    private static final String CLIENT_ID = "dnnydofmgg";
    private static final String CLIENT_SECRET = "GlevAwH7wuE5x2zfXuzwL9KVcHJUq5p7P7zYSF45";

    private NaverPlaceApiService apiService;
    private ExecutorService executor;
    private Handler mainHandler;

    // Debounce를 위한 변수들
    private Handler debounceHandler;
    private Runnable lastSearchRunnable;

    public interface PlaceSearchCallback {
        void onSuccess(List<PlaceItem> places);
        void onError(String error);
    }

    public NaverPlaceRetrofitService() {
        // Retrofit 클라이언트 설정
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        apiService = retrofit.create(NaverPlaceApiService.class);
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

                Log.d(TAG, "=== Naver Place API Call ===");
                Log.d(TAG, "Original keyword: " + keyword);
                Log.d(TAG, "Encoded query: " + encodedQuery);
                Log.d(TAG, "Base URL: " + BASE_URL);
                Log.d(TAG, "Client ID: " + CLIENT_ID);

                // Geocoding API 호출 (파라미터 보강)
                Call<NaverPlaceApiService.GeocodingResponse> call = apiService.geocode(
                        CLIENT_ID,
                        CLIENT_SECRET,
                        "ko", // Accept-Language 헤더
                        encodedQuery
                );

                call.enqueue(new Callback<NaverPlaceApiService.GeocodingResponse>() {
                    @Override
                    public void onResponse(Call<NaverPlaceApiService.GeocodingResponse> call, 
                                         Response<NaverPlaceApiService.GeocodingResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            NaverPlaceApiService.GeocodingResponse geocodingResponse = response.body();
                            
                            Log.d(TAG, "API Response Status: " + geocodingResponse.status);
                            if (geocodingResponse.meta != null) {
                                Log.d(TAG, "Total Count: " + geocodingResponse.meta.totalCount);
                            }

                            List<PlaceItem> places = parseGeocodingResponse(geocodingResponse, keyword);
                            
                            mainHandler.post(() -> callback.onSuccess(places));
                        } else {
                            Log.e(TAG, "API Response Error: " + response.code() + " - " + response.message());
                            mainHandler.post(() -> callback.onError("검색 실패: " + response.code()));
                        }
                    }

                    @Override
                    public void onFailure(Call<NaverPlaceApiService.GeocodingResponse> call, Throwable t) {
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
     * Geocoding API 응답을 PlaceItem 리스트로 변환
     */
    private List<PlaceItem> parseGeocodingResponse(NaverPlaceApiService.GeocodingResponse response, String keyword) {
        List<PlaceItem> places = new ArrayList<>();

        if (response.addresses != null && response.addresses.length > 0) {
            for (int i = 0; i < Math.min(response.addresses.length, 10); i++) {
                NaverPlaceApiService.GeocodingResponse.Address address = response.addresses[i];

                try {
                    double lat = Double.parseDouble(address.y);
                    double lng = Double.parseDouble(address.x);

                    String name = !address.roadAddress.isEmpty() ? address.roadAddress : address.jibunAddress;
                    if (name.length() > 50) {
                        name = name.substring(0, 47) + "...";
                    }

                    PlaceItem item = new PlaceItem(
                            name,
                            address.jibunAddress,
                            address.roadAddress,
                            lat,
                            lng,
                            "주소"
                    );

                    places.add(item);
                } catch (NumberFormatException e) {
                    Log.w(TAG, "Invalid coordinates for address: " + address.roadAddress);
                }
            }
        }

        Log.d(TAG, "Parsed " + places.size() + " places from response");
        return places;
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
