package com.example.timemate.network.api;

import android.util.Log;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Retrofit을 사용한 개선된 네이버 Directions API 서비스
 * 한글 인코딩과 로깅이 제대로 적용된 버전
 */
public class RetrofitNaverDirectionsService {
    
    private static final String TAG = "RetrofitNaverDirections";
    private static final String BASE_URL = "https://maps.apigw.ntruss.com/";
    
    // 네이버 클라우드 플랫폼에서 발급받은 API 키
    private static final String CLIENT_ID = "dnnydofmgg";
    private static final String CLIENT_SECRET = "GlevAwH7wuE5x2zfXuzwL9KVcHJUq5p7P7zYSF45";
    
    private NaverApiInterface apiInterface;
    
    public RetrofitNaverDirectionsService() {
        setupRetrofit();
    }
    
    /**
     * Retrofit 설정 (로깅 인터셉터 포함)
     */
    private void setupRetrofit() {
        // 로깅 인터셉터 설정
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor(message -> {
            Log.d(TAG, "HTTP: " + message);
        });
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        
        // OkHttpClient 설정
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        
        // Retrofit 설정
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        
        apiInterface = retrofit.create(NaverApiInterface.class);
    }
    
    /**
     * 길찾기 결과 콜백 인터페이스
     */
    public interface DirectionsCallback {
        void onSuccess(DirectionsResult result);
        void onError(String error);
    }
    
    /**
     * 길찾기 결과 데이터 클래스
     */
    public static class DirectionsResult {
        public String startAddress;
        public String goalAddress;
        public String distance;
        public String duration;
        public String tollFare;
        public String fuelPrice;
        
        public DirectionsResult(String startAddress, String goalAddress, 
                              String distance, String duration, String tollFare, String fuelPrice) {
            this.startAddress = startAddress;
            this.goalAddress = goalAddress;
            this.distance = distance;
            this.duration = duration;
            this.tollFare = tollFare;
            this.fuelPrice = fuelPrice;
        }
        
        @Override
        public String toString() {
            return String.format("거리: %s, 시간: %s, 통행료: %s", distance, duration, tollFare);
        }

        // Getter 메서드들
        public String getDistance() {
            return distance;
        }

        public String getDuration() {
            return duration;
        }

        public String getTollFare() {
            return tollFare;
        }

        public String getFuelPrice() {
            return fuelPrice;
        }

        public String getRouteSummary() {
            return startAddress + " → " + goalAddress;
        }
    }
    
    /**
     * 두 지점 간의 경로 정보를 조회합니다.
     * @param start 출발지 주소
     * @param goal 도착지 주소
     * @param callback 결과 콜백
     */
    public void getDirections(String start, String goal, DirectionsCallback callback) {
        Log.d(TAG, "길찾기 요청: " + start + " → " + goal);
        
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
                        
                        // 3단계: 길찾기 API 호출
                        callDirectionsApi(startCoords, goalCoords, start, goal, callback);
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
     * 좌표 변환 콜백 인터페이스
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
     * 길찾기 API 호출
     */
    private void callDirectionsApi(String startCoords, String goalCoords, 
                                 String startAddress, String goalAddress, 
                                 DirectionsCallback callback) {
        Log.d(TAG, "길찾기 API 호출: " + startCoords + " → " + goalCoords);
        
        Call<DirectionsResponse> call = apiInterface.getDirections(
                CLIENT_ID, CLIENT_SECRET, "ko", startCoords, goalCoords, "trafast");
        
        call.enqueue(new Callback<DirectionsResponse>() {
            @Override
            public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    DirectionsResponse directionsResponse = response.body();
                    
                    if (directionsResponse.route != null && 
                        directionsResponse.route.trafast != null && 
                        directionsResponse.route.trafast.length > 0) {
                        
                        DirectionsResponse.Route.Trafast.Summary summary = 
                                directionsResponse.route.trafast[0].summary;
                        
                        String distance = formatDistance(summary.distance);
                        String duration = formatDuration(summary.duration);
                        String tollFare = formatPrice(summary.tollFare);
                        String fuelPrice = formatPrice(summary.fuelPrice);
                        
                        DirectionsResult result = new DirectionsResult(
                                startAddress, goalAddress, distance, duration, tollFare, fuelPrice);
                        
                        Log.d(TAG, "길찾기 성공: " + result.toString());
                        callback.onSuccess(result);
                        
                    } else {
                        Log.e(TAG, "길찾기 결과 없음");
                        callback.onError("경로를 찾을 수 없습니다");
                    }
                } else {
                    Log.e(TAG, "길찾기 API 오류: " + response.code());
                    callback.onError("길찾기 API 오류: " + response.code());
                }
            }
            
            @Override
            public void onFailure(Call<DirectionsResponse> call, Throwable t) {
                Log.e(TAG, "길찾기 네트워크 오류", t);
                callback.onError("네트워크 오류: " + t.getMessage());
            }
        });
    }
    
    /**
     * 거리 포맷팅 (미터 → 킬로미터)
     */
    private String formatDistance(int distanceInMeters) {
        if (distanceInMeters >= 1000) {
            double km = distanceInMeters / 1000.0;
            return String.format("%.1fkm", km);
        } else {
            return distanceInMeters + "m";
        }
    }
    
    /**
     * 시간 포맷팅 (밀리초 → 분)
     */
    private String formatDuration(int durationInMillis) {
        int minutes = durationInMillis / (1000 * 60);
        if (minutes >= 60) {
            int hours = minutes / 60;
            int remainingMinutes = minutes % 60;
            return hours + "시간 " + remainingMinutes + "분";
        } else {
            return minutes + "분";
        }
    }
    
    /**
     * 가격 포맷팅
     */
    private String formatPrice(int price) {
        if (price == 0) {
            return "무료";
        } else {
            return String.format("%,d원", price);
        }
    }

    /**
     * 서비스 종료 (리소스 정리)
     * Activity 종료 시 호출하여 네트워크 연결 정리
     */
    public void shutdown() {
        // Retrofit은 자동으로 리소스를 관리하므로 특별한 정리가 필요하지 않음
        // 필요시 여기에 추가적인 정리 로직 구현 가능
        Log.d(TAG, "NaverDirectionsService shutdown");
    }
}
