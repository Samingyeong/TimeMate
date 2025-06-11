package com.example.timemate.network.api;

import android.util.Log;

import java.net.URLEncoder;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Query;

/**
 * 네이버 대중교통 길찾기 API 서비스
 * 버스, 지하철 등 대중교통 최적경로 제공
 */
public class NaverTransitService {
    
    private static final String TAG = "NaverTransit";
    private static final String BASE_URL = "https://naveropenapi.apigw.ntruss.com/";
    
    // 네이버 클라우드 플랫폼에서 발급받은 API 키
    private static final String CLIENT_ID = "dnnydofmgg";
    private static final String CLIENT_SECRET = "GlevAwH7wuE5x2zfXuzwL9KVcHJUq5p7P7zYSF45";
    
    private NaverTransitApiInterface apiInterface;
    
    public NaverTransitService() {
        setupRetrofit();
    }
    
    /**
     * Retrofit 설정
     */
    private void setupRetrofit() {
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor(message -> {
            Log.d(TAG, "HTTP: " + message);
        });
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        
        apiInterface = retrofit.create(NaverTransitApiInterface.class);
    }
    
    /**
     * 대중교통 경로 결과 콜백
     */
    public interface TransitCallback {
        void onSuccess(TransitResult result);
        void onError(String error);
    }
    
    /**
     * 대중교통 경로 결과 데이터
     */
    public static class TransitResult {
        public String startAddress;
        public String goalAddress;
        public String totalTime;
        public String totalDistance;
        public String totalFare;
        public String busTime;
        public String subwayTime;
        public String walkTime;
        public String transferCount;
        public String optimalRoute;
        
        public TransitResult(String startAddress, String goalAddress, String totalTime, 
                           String totalDistance, String totalFare, String busTime, 
                           String subwayTime, String walkTime, String transferCount, String optimalRoute) {
            this.startAddress = startAddress;
            this.goalAddress = goalAddress;
            this.totalTime = totalTime;
            this.totalDistance = totalDistance;
            this.totalFare = totalFare;
            this.busTime = busTime;
            this.subwayTime = subwayTime;
            this.walkTime = walkTime;
            this.transferCount = transferCount;
            this.optimalRoute = optimalRoute;
        }
        
        @Override
        public String toString() {
            return String.format("총 시간: %s, 요금: %s, 환승: %s회", totalTime, totalFare, transferCount);
        }
    }
    
    /**
     * 대중교통 최적경로 조회
     */
    public void getTransitRoute(String start, String goal, TransitCallback callback) {
        Log.d(TAG, "대중교통 경로 요청: " + start + " → " + goal);
        
        // 1단계: 출발지 좌표 변환
        getCoordinatesFromAddress(start, new GeocodingCallback() {
            @Override
            public void onSuccess(String coordinates) {
                String startCoords = coordinates;
                
                // 2단계: 도착지 좌표 변환
                getCoordinatesFromAddress(goal, new GeocodingCallback() {
                    @Override
                    public void onSuccess(String coordinates) {
                        String goalCoords = coordinates;
                        
                        // 3단계: 대중교통 경로 API 호출
                        callTransitApi(startCoords, goalCoords, start, goal, callback);
                    }
                    
                    @Override
                    public void onError(String error) {
                        callback.onError("도착지 좌표 변환 실패: " + error);
                    }
                });
            }
            
            @Override
            public void onError(String error) {
                callback.onError("출발지 좌표 변환 실패: " + error);
            }
        });
    }
    
    /**
     * 좌표 변환 콜백
     */
    private interface GeocodingCallback {
        void onSuccess(String coordinates);
        void onError(String error);
    }
    
    /**
     * 주소를 좌표로 변환
     */
    private void getCoordinatesFromAddress(String address, GeocodingCallback callback) {
        Log.d(TAG, "좌표 변환 요청: " + address);
        
        Call<GeocodingResponse> call = apiInterface.getCoordinates(
                CLIENT_ID, CLIENT_SECRET, "ko", address);
        
        call.enqueue(new Callback<GeocodingResponse>() {
            @Override
            public void onResponse(Call<GeocodingResponse> call, Response<GeocodingResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    GeocodingResponse geocodingResponse = response.body();
                    
                    if (geocodingResponse.addresses != null && geocodingResponse.addresses.length > 0) {
                        GeocodingResponse.Address firstAddress = geocodingResponse.addresses[0];
                        String coordinates = firstAddress.x + "," + firstAddress.y;
                        
                        Log.d(TAG, "좌표 변환 성공: " + address + " → " + coordinates);
                        callback.onSuccess(coordinates);
                    } else {
                        Log.e(TAG, "좌표 변환 결과 없음: " + address);
                        callback.onError("주소를 찾을 수 없습니다");
                    }
                } else {
                    Log.e(TAG, "좌표 변환 API 오류: " + response.code());
                    callback.onError("좌표 변환 API 오류: " + response.code());
                }
            }
            
            @Override
            public void onFailure(Call<GeocodingResponse> call, Throwable t) {
                Log.e(TAG, "좌표 변환 네트워크 오류", t);
                callback.onError("네트워크 오류: " + t.getMessage());
            }
        });
    }
    
    /**
     * 대중교통 경로 API 호출 (더미 구현)
     * 실제로는 네이버 대중교통 API나 다른 대중교통 API 사용
     */
    private void callTransitApi(String startCoords, String goalCoords, 
                               String startAddress, String goalAddress, 
                               TransitCallback callback) {
        Log.d(TAG, "대중교통 API 호출: " + startCoords + " → " + goalCoords);
        
        // 더미 데이터로 대중교통 정보 생성
        // 실제로는 네이버 대중교통 API 또는 서울시 지하철 API 등을 사용
        
        // 거리 기반 예상 시간 계산
        String[] startLatLng = startCoords.split(",");
        String[] goalLatLng = goalCoords.split(",");
        
        double distance = calculateDistance(
                Double.parseDouble(startLatLng[1]), Double.parseDouble(startLatLng[0]),
                Double.parseDouble(goalLatLng[1]), Double.parseDouble(goalLatLng[0])
        );
        
        // 대중교통 예상 정보 계산
        int totalMinutes = (int) (distance / 30.0 * 60); // 평균 시속 30km
        int busMinutes = (int) (totalMinutes * 0.6); // 버스 60%
        int subwayMinutes = (int) (totalMinutes * 0.3); // 지하철 30%
        int walkMinutes = (int) (totalMinutes * 0.1); // 도보 10%
        int transferCount = distance > 10 ? 2 : (distance > 5 ? 1 : 0);
        int fare = 1400 + (transferCount * 100); // 기본요금 + 환승비
        
        TransitResult result = new TransitResult(
                startAddress, goalAddress,
                formatTime(totalMinutes),
                formatDistance(distance),
                formatPrice(fare),
                formatTime(busMinutes),
                formatTime(subwayMinutes),
                formatTime(walkMinutes),
                String.valueOf(transferCount),
                generateOptimalRoute(distance, transferCount)
        );
        
        Log.d(TAG, "대중교통 경로 성공: " + result.toString());
        callback.onSuccess(result);
    }
    
    /**
     * 거리 계산 (하버사인 공식)
     */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // 지구 반지름 (km)
        
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return R * c;
    }
    
    /**
     * 시간 포맷팅
     */
    private String formatTime(int minutes) {
        if (minutes >= 60) {
            int hours = minutes / 60;
            int remainingMinutes = minutes % 60;
            return hours + "시간 " + remainingMinutes + "분";
        } else {
            return minutes + "분";
        }
    }
    
    /**
     * 거리 포맷팅
     */
    private String formatDistance(double distanceInKm) {
        if (distanceInKm >= 1.0) {
            return String.format("%.1fkm", distanceInKm);
        } else {
            return String.format("%.0fm", distanceInKm * 1000);
        }
    }
    
    /**
     * 가격 포맷팅
     */
    private String formatPrice(int price) {
        return String.format("%,d원", price);
    }
    
    /**
     * 최적 경로 설명 생성
     */
    private String generateOptimalRoute(double distance, int transferCount) {
        if (distance < 2) {
            return "도보 이용 권장";
        } else if (distance < 5) {
            return "버스 이용 (환승 없음)";
        } else if (distance < 15) {
            return "지하철 + 버스 (환승 " + transferCount + "회)";
        } else {
            return "지하철 + 고속버스 (환승 " + transferCount + "회)";
        }
    }
    
    /**
     * 서비스 종료
     */
    public void shutdown() {
        Log.d(TAG, "NaverTransitService shutdown");
    }
}

/**
 * 대중교통 API 인터페이스
 */
interface NaverTransitApiInterface {
    @GET("map-geocode/v2/geocode")
    Call<GeocodingResponse> getCoordinates(
            @Header("X-NCP-APIGW-API-KEY-ID") String clientId,
            @Header("X-NCP-APIGW-API-KEY") String clientSecret,
            @Header("Accept-Language") String acceptLanguage,
            @Query("query") String query
    );
}
