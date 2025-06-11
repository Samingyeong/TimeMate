package com.example.timemate.model;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;

/**
 * iOS 스타일 길찾기 경로 옵션 모델
 */
public class RouteOption {
    
    public enum RouteType {
        OPTIMAL("추천 최적 경로", "trafast"),
        TOLL_FREE("무료도로 우선", "traavoidtoll"),
        TRANSIT("대중교통", "transit");
        
        private final String displayName;
        private final String apiOption;
        
        RouteType(String displayName, String apiOption) {
            this.displayName = displayName;
            this.apiOption = apiOption;
        }
        
        public String getDisplayName() { return displayName; }
        public String getApiOption() { return apiOption; }
    }
    
    public enum TransportMode {
        CAR("자동차", "ic_route_car"),
        BUS("버스", "ic_route_bus"),
        SUBWAY("지하철", "ic_route_bus"),
        WALK("도보", "ic_route_walk"),
        MIXED("복합", "ic_route_car");
        
        private final String displayName;
        private final String iconResource;
        
        TransportMode(String displayName, String iconResource) {
            this.displayName = displayName;
            this.iconResource = iconResource;
        }
        
        public String getDisplayName() { return displayName; }
        public String getIconResource() { return iconResource; }
    }
    
    // 기본 정보
    public String id;
    public RouteType routeType;
    public TransportMode transportMode;
    public boolean isSelected;
    public boolean isRecommended;
    
    // 경로 정보
    public String departure;
    public String destination;
    public String distance;        // "3.2 km"
    public String duration;        // "25분"
    public String cost;           // "3,200원"
    public String description;    // 상세 설명
    
    // 상세 교통수단 정보 (복합 경로용)
    public List<TransportSegment> segments;
    
    // 좌표 정보
    public double startLatitude;
    public double startLongitude;
    public double endLatitude;
    public double endLongitude;
    
    // 생성자
    public RouteOption() {
        this.isSelected = false;
        this.isRecommended = false;
    }
    
    public RouteOption(RouteType routeType, TransportMode transportMode, 
                      String departure, String destination,
                      String distance, String duration, String cost) {
        this();
        this.routeType = routeType;
        this.transportMode = transportMode;
        this.departure = departure;
        this.destination = destination;
        this.distance = distance;
        this.duration = duration;
        this.cost = cost;
        this.id = generateId();
    }
    
    /**
     * 고유 ID 생성
     */
    private String generateId() {
        return routeType.name() + "_" + System.currentTimeMillis();
    }
    
    /**
     * 소요시간을 분 단위로 변환
     */
    public int getDurationInMinutes() {
        try {
            String cleanDuration = duration.replaceAll("[^0-9]", "");
            return Integer.parseInt(cleanDuration);
        } catch (Exception e) {
            return 0;
        }
    }
    
    /**
     * 비용을 숫자로 변환
     */
    public int getCostInWon() {
        try {
            String cleanCost = cost.replaceAll("[^0-9]", "");
            return cleanCost.isEmpty() ? 0 : Integer.parseInt(cleanCost);
        } catch (Exception e) {
            return 0;
        }
    }
    
    /**
     * JSON 직렬화
     */
    public String toJson() {
        return new Gson().toJson(this);
    }
    
    /**
     * JSON 역직렬화
     */
    public static RouteOption fromJson(String json) {
        return new Gson().fromJson(json, RouteOption.class);
    }
    
    /**
     * 리스트 JSON 직렬화
     */
    public static String listToJson(List<RouteOption> routes) {
        return new Gson().toJson(routes);
    }
    
    /**
     * 리스트 JSON 역직렬화
     */
    public static List<RouteOption> listFromJson(String json) {
        Type listType = new TypeToken<List<RouteOption>>(){}.getType();
        return new Gson().fromJson(json, listType);
    }
    
    /**
     * 교통수단 세그먼트 (복합 경로용)
     */
    public static class TransportSegment {
        public TransportMode mode;
        public String duration;    // "15분"
        public String detail;      // "2호선 → 9호선"
        
        public TransportSegment(TransportMode mode, String duration, String detail) {
            this.mode = mode;
            this.duration = duration;
            this.detail = detail;
        }
    }
    
    @Override
    public String toString() {
        return String.format("%s: %s → %s (%s, %s, %s)", 
                           routeType.getDisplayName(), departure, destination, 
                           distance, duration, cost);
    }
}
