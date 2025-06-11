package com.example.timemate.network.api;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Query;

/**
 * 네이버 클라우드 플랫폼 Place API 인터페이스
 * Retrofit을 사용한 실시간 장소 검색
 */
public interface NaverPlaceApiService {

    /**
     * 네이버 클라우드 플랫폼 Geocoding API
     * 주소/키워드를 좌표로 변환
     */
    @GET("map-geocode/v2/geocode")
    Call<GeocodingResponse> geocode(
            @Header("X-NCP-APIGW-API-KEY-ID") String clientId,
            @Header("X-NCP-APIGW-API-KEY") String clientSecret,
            @Header("Accept-Language") String language,
            @Query(value = "query", encoded = true) String query
    );

    /**
     * Geocoding API 응답 데이터 클래스
     */
    class GeocodingResponse {
        public String status;
        public Meta meta;
        public Address[] addresses;
        public String errorMessage;

        public static class Meta {
            public int totalCount;
            public int page;
            public int count;
        }

        public static class Address {
            public String roadAddress;
            public String jibunAddress;
            public String englishAddress;
            public AddressElement[] addressElements;
            public String x; // 경도
            public String y; // 위도
            public double distance;

            public static class AddressElement {
                public String[] types;
                public String longName;
                public String shortName;
                public String code;
            }
        }
    }

    /**
     * 네이버 클라우드 플랫폼 Reverse Geocoding API
     * 좌표를 주소로 변환
     */
    @GET("map-reversegeocode/v2/gc")
    Call<ReverseGeocodingResponse> reverseGeocode(
            @Header("X-NCP-APIGW-API-KEY-ID") String clientId,
            @Header("X-NCP-APIGW-API-KEY") String clientSecret,
            @Header("Accept-Language") String language,
            @Query("coords") String coords,
            @Query("sourcecrs") String sourceCrs,
            @Query("targetcrs") String targetCrs,
            @Query("orders") String orders
    );

    /**
     * Reverse Geocoding API 응답 데이터 클래스
     */
    class ReverseGeocodingResponse {
        public String status;
        public Result[] results;

        public static class Result {
            public String name;
            public Code code;
            public Region region;
            public Land land;

            public static class Code {
                public String id;
                public String type;
                public String mappingId;
            }

            public static class Region {
                public Area area0;
                public Area area1;
                public Area area2;
                public Area area3;
                public Area area4;

                public static class Area {
                    public String name;
                    public Coords coords;

                    public static class Coords {
                        public Center center;

                        public static class Center {
                            public String crs;
                            public String x;
                            public String y;
                        }
                    }
                }
            }

            public static class Land {
                public String type;
                public String number1;
                public String number2;
                public Addition addition0;
                public Addition addition1;
                public Addition addition2;
                public Addition addition3;
                public Addition addition4;
                public Coords coords;

                public static class Addition {
                    public String type;
                    public String value;
                }

                public static class Coords {
                    public Center center;

                    public static class Center {
                        public String crs;
                        public String x;
                        public String y;
                    }
                }
            }
        }
    }
}
