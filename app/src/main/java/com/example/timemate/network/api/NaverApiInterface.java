package com.example.timemate.network.api;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Query;

/**
 * 네이버 API Retrofit 인터페이스
 * Directions API와 Geocoding API 호출을 위한 인터페이스
 */
public interface NaverApiInterface {
    
    /**
     * 네이버 Directions API - 길찾기
     * @param clientId API 클라이언트 ID
     * @param clientSecret API 클라이언트 시크릿
     * @param acceptLanguage 언어 설정 (ko)
     * @param start 출발지 좌표 (longitude,latitude)
     * @param goal 도착지 좌표 (longitude,latitude)
     * @param option 경로 옵션 (trafast: 실시간 빠른길)
     * @return 길찾기 결과
     */
    @GET("map-direction/v1/driving")
    Call<DirectionsResponse> getDirections(
            @Header("X-NCP-APIGW-API-KEY-ID") String clientId,
            @Header("X-NCP-APIGW-API-KEY") String clientSecret,
            @Header("Accept-Language") String acceptLanguage,
            @Query("start") String start,
            @Query("goal") String goal,
            @Query("option") String option
    );
    
    /**
     * 네이버 Geocoding API - 주소를 좌표로 변환
     * @param clientId API 클라이언트 ID
     * @param clientSecret API 클라이언트 시크릿
     * @param acceptLanguage 언어 설정 (ko)
     * @param query 검색할 주소
     * @return 좌표 변환 결과
     */
    @GET("map-geocode/v2/geocode")
    Call<GeocodingResponse> getCoordinates(
            @Header("X-NCP-APIGW-API-KEY-ID") String clientId,
            @Header("X-NCP-APIGW-API-KEY") String clientSecret,
            @Header("Accept-Language") String acceptLanguage,
            @Query("query") String query
    );
}

/**
 * 길찾기 API 응답 모델
 */
class DirectionsResponse {
    public int code;
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

/**
 * 좌표 변환 API 응답 모델
 */
class GeocodingResponse {
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
        public String x; // longitude
        public String y; // latitude
        public int distance;
    }
}
