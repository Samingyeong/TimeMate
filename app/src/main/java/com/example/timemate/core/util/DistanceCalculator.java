package com.example.timemate.core.util;

import android.location.Location;
import android.util.Log;

/**
 * 위치 간 거리 계산 및 교통수단 추천 유틸리티
 */
public class DistanceCalculator {
    
    private static final String TAG = "DistanceCalculator";
    
    // 지구 반지름 (km)
    private static final double EARTH_RADIUS_KM = 6371.0;
    
    /**
     * 두 지점 간의 직선 거리 계산 (Haversine 공식)
     * @param lat1 출발지 위도
     * @param lon1 출발지 경도
     * @param lat2 도착지 위도
     * @param lon2 도착지 경도
     * @return 거리 (미터)
     */
    public static double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        try {
            // 위도와 경도를 라디안으로 변환
            double lat1Rad = Math.toRadians(lat1);
            double lon1Rad = Math.toRadians(lon1);
            double lat2Rad = Math.toRadians(lat2);
            double lon2Rad = Math.toRadians(lon2);
            
            // 위도와 경도 차이
            double deltaLat = lat2Rad - lat1Rad;
            double deltaLon = lon2Rad - lon1Rad;
            
            // Haversine 공식
            double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
                      Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                      Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);
            
            double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
            
            // 거리 계산 (km)
            double distanceKm = EARTH_RADIUS_KM * c;
            
            // 미터로 변환
            double distanceM = distanceKm * 1000;
            
            Log.d(TAG, String.format("거리 계산: %.2fkm (%.0fm)", distanceKm, distanceM));
            
            return distanceM;
            
        } catch (Exception e) {
            Log.e(TAG, "거리 계산 오류", e);
            return 0;
        }
    }
    
    /**
     * Android Location 객체를 사용한 거리 계산
     */
    public static float calculateDistanceWithLocation(double lat1, double lon1, double lat2, double lon2) {
        try {
            Location startPoint = new Location("start");
            startPoint.setLatitude(lat1);
            startPoint.setLongitude(lon1);
            
            Location endPoint = new Location("end");
            endPoint.setLatitude(lat2);
            endPoint.setLongitude(lon2);
            
            float distance = startPoint.distanceTo(endPoint);
            
            Log.d(TAG, String.format("Android Location 거리: %.0fm", distance));
            
            return distance;
            
        } catch (Exception e) {
            Log.e(TAG, "Android Location 거리 계산 오류", e);
            return 0;
        }
    }
    
    /**
     * 거리에 따른 교통수단 추천 (개선된 버전)
     */
    public static TransportRecommendation getTransportRecommendation(double distanceInMeters) {
        try {
            TransportRecommendation recommendation = new TransportRecommendation();
            recommendation.distance = distanceInMeters;
            recommendation.distanceText = formatDistance(distanceInMeters);

            if (distanceInMeters <= 500) {
                // 500m 이하 - 도보 추천
                recommendation.primaryTransport = "도보";
                recommendation.primaryIcon = "🚶";
                recommendation.primaryTime = formatTime(calculateWalkingTime(distanceInMeters));
                recommendation.primaryDescription = "가까운 거리입니다. 걸어서 이동하세요.";
                recommendation.alternatives.add("자전거 🚴 " + formatTime(calculateBicycleTime(distanceInMeters)));

            } else if (distanceInMeters <= 2000) {
                // 2km 이하 - 자전거 추천
                recommendation.primaryTransport = "자전거";
                recommendation.primaryIcon = "🚴";
                recommendation.primaryTime = formatTime(calculateBicycleTime(distanceInMeters));
                recommendation.primaryDescription = "자전거 이용을 추천합니다.";
                recommendation.alternatives.add("도보 🚶 " + formatTime(calculateWalkingTime(distanceInMeters)));
                recommendation.alternatives.add("대중교통 🚌 " + formatTime(calculatePublicTransportTime(distanceInMeters)));

            } else if (distanceInMeters <= 10000) {
                // 10km 이하 - 대중교통 추천
                recommendation.primaryTransport = "대중교통";
                recommendation.primaryIcon = "🚌";
                recommendation.primaryTime = formatTime(calculatePublicTransportTime(distanceInMeters));
                recommendation.primaryDescription = "대중교통 이용을 추천합니다.";
                recommendation.alternatives.add("택시 🚕 " + formatTime(calculateTaxiTime(distanceInMeters)) + " (" + calculateTaxiCost(distanceInMeters) + ")");
                recommendation.alternatives.add("자동차 🚗 " + formatTime(calculateCarTime(distanceInMeters)) + " (" + calculateCarCost(distanceInMeters) + ")");

            } else if (distanceInMeters <= 50000) {
                // 50km 이하 - 자동차/기차 추천
                recommendation.primaryTransport = "자동차";
                recommendation.primaryIcon = "🚗";
                recommendation.primaryTime = formatTime(calculateCarTime(distanceInMeters));
                recommendation.primaryDescription = "자동차나 기차 이용을 추천합니다.";
                recommendation.alternatives.add("기차 🚄 " + formatTime(calculateTrainTime(distanceInMeters)));
                recommendation.alternatives.add("택시 🚕 " + formatTime(calculateTaxiTime(distanceInMeters)) + " (" + calculateTaxiCost(distanceInMeters) + ")");

            } else {
                // 50km 초과 - 기차/고속버스 추천
                recommendation.primaryTransport = "기차/고속버스";
                recommendation.primaryIcon = "🚄";
                recommendation.primaryTime = formatTime(calculateTrainTime(distanceInMeters));
                recommendation.primaryDescription = "장거리입니다. 기차나 고속버스를 이용하세요.";
                recommendation.alternatives.add("고속버스 🚌 " + formatTime(calculateBusTime(distanceInMeters)));
                recommendation.alternatives.add("자동차 🚗 " + formatTime(calculateCarTime(distanceInMeters)) + " (" + calculateCarCost(distanceInMeters) + ")");
            }

            Log.d(TAG, "교통수단 추천: " + recommendation.primaryTransport + " (" + recommendation.distanceText + ")");

            return recommendation;

        } catch (Exception e) {
            Log.e(TAG, "교통수단 추천 오류", e);
            return getDefaultRecommendation();
        }
    }
    
    /**
     * 시간을 시간:분 형태로 포맷팅
     */
    public static String formatTime(int totalMinutes) {
        if (totalMinutes < 60) {
            return totalMinutes + "분";
        } else {
            int hours = totalMinutes / 60;
            int minutes = totalMinutes % 60;
            if (minutes == 0) {
                return hours + "시간";
            } else {
                return hours + "시간 " + minutes + "분";
            }
        }
    }

    /**
     * 도보 시간 계산 (평균 속도: 4.5km/h)
     */
    public static int calculateWalkingTime(double distanceInMeters) {
        double walkingSpeedKmh = 4.5; // 4.5km/h
        double distanceKm = distanceInMeters / 1000.0;
        int minutes = (int) Math.ceil((distanceKm / walkingSpeedKmh) * 60);
        return Math.max(1, minutes);
    }

    /**
     * 자전거 시간 계산 (평균 속도: 15km/h)
     */
    public static int calculateBicycleTime(double distanceInMeters) {
        double bicycleSpeedKmh = 15.0; // 15km/h
        double distanceKm = distanceInMeters / 1000.0;
        int minutes = (int) Math.ceil((distanceKm / bicycleSpeedKmh) * 60);
        return Math.max(1, minutes);
    }

    /**
     * 대중교통 시간 계산 (평균 속도: 20km/h, 대기시간 포함)
     */
    public static int calculatePublicTransportTime(double distanceInMeters) {
        double publicTransportSpeedKmh = 20.0; // 20km/h
        double distanceKm = distanceInMeters / 1000.0;
        int travelTime = (int) Math.ceil((distanceKm / publicTransportSpeedKmh) * 60);
        int waitingTime = 5; // 대기시간 5분
        return Math.max(5, travelTime + waitingTime);
    }

    /**
     * 택시 시간 계산 (평균 속도: 25km/h)
     */
    public static int calculateTaxiTime(double distanceInMeters) {
        double taxiSpeedKmh = 25.0; // 25km/h
        double distanceKm = distanceInMeters / 1000.0;
        int minutes = (int) Math.ceil((distanceKm / taxiSpeedKmh) * 60);
        return Math.max(3, minutes);
    }

    /**
     * 자동차 시간 계산 (평균 속도: 35km/h)
     */
    public static int calculateCarTime(double distanceInMeters) {
        double carSpeedKmh = 35.0; // 35km/h
        double distanceKm = distanceInMeters / 1000.0;
        int minutes = (int) Math.ceil((distanceKm / carSpeedKmh) * 60);
        return Math.max(3, minutes);
    }

    /**
     * 기차 시간 계산 (평균 속도: 60km/h)
     */
    public static int calculateTrainTime(double distanceInMeters) {
        double trainSpeedKmh = 60.0; // 60km/h
        double distanceKm = distanceInMeters / 1000.0;
        int minutes = (int) Math.ceil((distanceKm / trainSpeedKmh) * 60);
        return Math.max(10, minutes);
    }

    /**
     * 버스 시간 계산 (평균 속도: 30km/h)
     */
    public static int calculateBusTime(double distanceInMeters) {
        double busSpeedKmh = 30.0; // 30km/h
        double distanceKm = distanceInMeters / 1000.0;
        int minutes = (int) Math.ceil((distanceKm / busSpeedKmh) * 60);
        return Math.max(10, minutes);
    }

    /**
     * 택시 비용 계산 (기본요금 4,800원 + 거리요금)
     */
    public static String calculateTaxiCost(double distanceInMeters) {
        int baseFare = 4800; // 기본요금 4,800원
        double distanceKm = distanceInMeters / 1000.0;
        int distanceFare = (int) (distanceKm * 1000); // 1km당 1,000원
        int totalCost = baseFare + distanceFare;
        return String.format("%,d원", totalCost);
    }

    /**
     * 자동차 비용 계산 (연료비 + 톨게이트비)
     */
    public static String calculateCarCost(double distanceInMeters) {
        double distanceKm = distanceInMeters / 1000.0;
        int fuelCost = (int) (distanceKm * 150); // 1km당 연료비 150원
        int tollCost = 0;

        // 10km 이상일 때 톨게이트비 추가
        if (distanceKm > 10) {
            tollCost = (int) ((distanceKm - 10) * 100); // 10km 초과분에 대해 1km당 100원
        }

        int totalCost = fuelCost + tollCost;
        if (tollCost > 0) {
            return String.format("%,d원 (연료비 + 톨게이트)", totalCost);
        } else {
            return String.format("%,d원", totalCost);
        }
    }

    /**
     * 거리 포맷팅
     */
    public static String formatDistance(double distanceInMeters) {
        if (distanceInMeters >= 1000) {
            double km = distanceInMeters / 1000.0;
            return String.format("%.1fkm", km);
        } else {
            return String.format("%.0fm", distanceInMeters);
        }
    }
    
    /**
     * 기본 추천 (오류 시)
     */
    private static TransportRecommendation getDefaultRecommendation() {
        TransportRecommendation recommendation = new TransportRecommendation();
        recommendation.distance = 0;
        recommendation.distanceText = "거리 미상";
        recommendation.primaryTransport = "대중교통";
        recommendation.primaryIcon = "🚌";
        recommendation.primaryTime = "시간 미상";
        recommendation.primaryDescription = "대중교통 이용을 추천합니다.";
        return recommendation;
    }
    
    /**
     * 교통수단 추천 결과 클래스
     */
    public static class TransportRecommendation {
        public double distance;
        public String distanceText;
        public String primaryTransport;
        public String primaryIcon;
        public String primaryTime;
        public String primaryDescription;
        public java.util.List<String> alternatives = new java.util.ArrayList<>();
        
        @Override
        public String toString() {
            return primaryIcon + " " + primaryTransport + " " + primaryTime + " (" + distanceText + ")";
        }
    }
}
