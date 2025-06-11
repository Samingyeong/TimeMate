package com.example.timemate.network.api;

import com.example.timemate.core.config.ApiConfig;
import com.example.timemate.network.interceptor.NaverAuthInterceptor;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.util.concurrent.TimeUnit;

/**
 * 네이버 API 서비스 팩토리
 * 모든 네이버 API 서비스의 중앙 관리
 */
public class NaverApiService {

    private static NaverApiService instance;
    
    // Retrofit 인스턴스들
    private Retrofit naverDevRetrofit;      // 네이버 개발자센터 API
    private Retrofit naverCloudRetrofit;    // 네이버 클라우드 플랫폼 API
    
    // API 서비스 인스턴스들
    private NaverLocalSearchApi localSearchApi;
    private NaverDirectionsApi directionsApi;
    private NaverGeocodingApi geocodingApi;
    private NaverStaticMapApi staticMapApi;

    private NaverApiService() {
        initRetrofitClients();
        initApiServices();
    }

    public static synchronized NaverApiService getInstance() {
        if (instance == null) {
            instance = new NaverApiService();
        }
        return instance;
    }

    /**
     * Retrofit 클라이언트 초기화
     */
    private void initRetrofitClients() {
        // 공통 OkHttp 클라이언트 설정
        OkHttpClient.Builder httpClientBuilder = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS);

        // 네이버 개발자센터 API용 Retrofit
        OkHttpClient naverDevClient = httpClientBuilder
                .addInterceptor(new NaverAuthInterceptor(
                    ApiConfig.NAVER_DEV_CLIENT_ID,
                    ApiConfig.NAVER_DEV_CLIENT_SECRET,
                    NaverAuthInterceptor.AuthType.DEVELOPER_CENTER
                ))
                .build();

        naverDevRetrofit = new Retrofit.Builder()
                .baseUrl("https://openapi.naver.com/")
                .client(naverDevClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        // 네이버 클라우드 플랫폼 API용 Retrofit
        OkHttpClient naverCloudClient = httpClientBuilder
                .addInterceptor(new NaverAuthInterceptor(
                    ApiConfig.NAVER_CLOUD_CLIENT_ID,
                    ApiConfig.NAVER_CLOUD_CLIENT_SECRET,
                    NaverAuthInterceptor.AuthType.CLOUD_PLATFORM
                ))
                .build();

        naverCloudRetrofit = new Retrofit.Builder()
                .baseUrl("https://maps.apigw.ntruss.com/")
                .client(naverCloudClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }

    /**
     * API 서비스 인스턴스 초기화
     */
    private void initApiServices() {
        // 네이버 개발자센터 API
        localSearchApi = naverDevRetrofit.create(NaverLocalSearchApi.class);
        
        // 네이버 클라우드 플랫폼 API
        directionsApi = naverCloudRetrofit.create(NaverDirectionsApi.class);
        geocodingApi = naverCloudRetrofit.create(NaverGeocodingApi.class);
        staticMapApi = naverCloudRetrofit.create(NaverStaticMapApi.class);
    }

    // API 서비스 Getter 메서드들
    public NaverLocalSearchApi getLocalSearchApi() {
        return localSearchApi;
    }

    public NaverDirectionsApi getDirectionsApi() {
        return directionsApi;
    }

    public NaverGeocodingApi getGeocodingApi() {
        return geocodingApi;
    }

    public NaverStaticMapApi getStaticMapApi() {
        return staticMapApi;
    }

    /**
     * 네이버 Local Search API 인터페이스
     */
    public interface NaverLocalSearchApi {
        @retrofit2.http.GET("v1/search/local.json")
        retrofit2.Call<LocalSearchResponse> searchLocal(
                @retrofit2.http.Query(value = "query", encoded = true) String query,
                @retrofit2.http.Query("display") int display,
                @retrofit2.http.Query("start") int start,
                @retrofit2.http.Query("sort") String sort
        );
    }

    /**
     * 네이버 Directions API 인터페이스
     */
    public interface NaverDirectionsApi {
        @retrofit2.http.GET("map-direction/v1/driving")
        retrofit2.Call<DirectionsResponse> getDirections(
                @retrofit2.http.Query("start") String start,
                @retrofit2.http.Query("goal") String goal,
                @retrofit2.http.Query("option") String option
        );
    }

    /**
     * 네이버 Geocoding API 인터페이스
     */
    public interface NaverGeocodingApi {
        @retrofit2.http.GET("map-geocode/v2/geocode")
        retrofit2.Call<GeocodingResponse> geocode(
                @retrofit2.http.Query("query") String query
        );

        @retrofit2.http.GET("map-reversegeocode/v2/gc")
        retrofit2.Call<ReverseGeocodingResponse> reverseGeocode(
                @retrofit2.http.Query("coords") String coords,
                @retrofit2.http.Query("output") String output
        );
    }

    /**
     * 네이버 Static Map API 인터페이스
     */
    public interface NaverStaticMapApi {
        @retrofit2.http.GET("map-static/v2/raster")
        retrofit2.Call<okhttp3.ResponseBody> getStaticMap(
                @retrofit2.http.Query("w") int width,
                @retrofit2.http.Query("h") int height,
                @retrofit2.http.Query("center") String center,
                @retrofit2.http.Query("level") int level,
                @retrofit2.http.Query("markers") String markers
        );
    }

    // 응답 데이터 클래스들
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
            public int mapx;
            public int mapy;
        }
    }

    public static class DirectionsResponse {
        public String code;
        public String message;
        public Route route;

        public static class Route {
            public Trafast[] trafast;

            public static class Trafast {
                public Summary summary;
                public Path[] path;

                public static class Summary {
                    public Location start;
                    public Location goal;
                    public int distance;
                    public int duration;
                    public int tollFare;
                    public int fuelPrice;

                    public static class Location {
                        public double[] location;
                    }
                }

                public static class Path {
                    public double[][] coords;
                }
            }
        }
    }

    public static class GeocodingResponse {
        public String status;
        public Meta meta;
        public Address[] addresses;

        public static class Meta {
            public int totalCount;
            public int page;
            public int count;
        }

        public static class Address {
            public String roadAddress;
            public String jibunAddress;
            public String englishAddress;
            public double x;
            public double y;
            public int distance;
        }
    }

    public static class ReverseGeocodingResponse {
        public String status;
        public Result[] results;

        public static class Result {
            public String name;
            public Region region;
            public Land land;

            public static class Region {
                public Area area1;
                public Area area2;
                public Area area3;
                public Area area4;

                public static class Area {
                    public String name;
                    public String coords;
                }
            }

            public static class Land {
                public String type;
                public String number1;
                public String number2;
                public Addition addition0;

                public static class Addition {
                    public String type;
                    public String value;
                }
            }
        }
    }

    /**
     * 서비스 종료 - 모든 네트워크 연결 정리
     */
    public void shutdown() {
        android.util.Log.d("NaverApiService", "Shutting down all Naver API services");

        // OkHttp 클라이언트들의 연결 풀 정리
        try {
            if (naverDevRetrofit != null) {
                OkHttpClient client = (OkHttpClient) naverDevRetrofit.callFactory();
                if (client != null) {
                    client.dispatcher().executorService().shutdown();
                    client.connectionPool().evictAll();
                }
            }

            if (naverCloudRetrofit != null) {
                OkHttpClient client = (OkHttpClient) naverCloudRetrofit.callFactory();
                if (client != null) {
                    client.dispatcher().executorService().shutdown();
                    client.connectionPool().evictAll();
                }
            }
        } catch (Exception e) {
            android.util.Log.e("NaverApiService", "Error during shutdown", e);
        }
    }

    /**
     * 싱글톤 인스턴스 정리
     */
    public static void destroyInstance() {
        if (instance != null) {
            instance.shutdown();
            instance = null;
        }
    }
}
