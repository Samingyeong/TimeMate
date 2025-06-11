package com.example.timemate.network.api;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.example.timemate.config.ApiConfig;

/**
 * 다중 교통수단 경로 서비스
 * 대중교통, 자동차, 도보 경로를 모두 제공
 */
public class MultiModalRouteService {

    private static final String TAG = "MultiModalRoute";
    
    // 네이버 클라우드 플랫폼 API 설정
    private static final String CLIENT_ID = ApiConfig.NAVER_CLOUD_CLIENT_ID;
    private static final String CLIENT_SECRET = ApiConfig.NAVER_CLOUD_CLIENT_SECRET;
    private static final String DIRECTIONS_URL = ApiConfig.NAVER_DIRECTIONS_URL;
    
    private ExecutorService executor;
    private RealTimeTrafficService trafficService;
    private WeatherService weatherService;
    private DistanceCalculationService distanceService;

    public MultiModalRouteService() {
        executor = Executors.newSingleThreadExecutor();
        trafficService = new RealTimeTrafficService();
        weatherService = new WeatherService();
        distanceService = new DistanceCalculationService();
        Log.d(TAG, "Multi Modal Route Service initialized with real APIs and distance calculation");
    }

    /**
     * 경로 옵션 클래스
     */
    public static class RouteOption {
        public String transportMode;     // 교통수단 (driving, transit, walking)
        public String transportIcon;     // 교통수단 아이콘
        public String transportName;     // 교통수단 이름
        public String distance;          // 거리
        public String duration;          // 소요시간
        public String cost;              // 비용
        public String description;       // 설명
        public boolean isRecommended;    // 추천 여부
        public int priority;             // 우선순위 (낮을수록 우선)
        public String departure;         // 출발지
        public String destination;       // 도착지

        public RouteOption(String transportMode, String transportIcon, String transportName,
                          String distance, String duration, String cost, String description) {
            this.transportMode = transportMode;
            this.transportIcon = transportIcon;
            this.transportName = transportName;
            this.distance = distance;
            this.duration = duration;
            this.cost = cost;
            this.description = description;
            this.isRecommended = false;
            this.priority = 999;
        }

        public String getDisplayText() {
            return transportIcon + " " + transportName + " • " + duration + " • " + cost;
        }

        public String getDetailText() {
            return "거리: " + distance + " | 시간: " + duration + " | 비용: " + cost;
        }
    }

    /**
     * 경로 검색 결과 콜백
     */
    public interface RouteCallback {
        void onSuccess(List<RouteOption> routes);
        void onError(String error);
    }

    /**
     * 다중 교통수단 경로 검색 (기본 설정)
     */
    public void getMultiModalRoutes(double startLat, double startLng,
                                   double goalLat, double goalLng,
                                   String startName, String goalName,
                                   RouteCallback callback) {
        getMultiModalRoutes(startLat, startLng, goalLat, goalLng, startName, goalName,
                           "time", true, callback);
    }

    /**
     * 다중 교통수단 경로 검색 (고급 설정)
     */
    public void getMultiModalRoutes(double startLat, double startLng,
                                   double goalLat, double goalLng,
                                   String startName, String goalName,
                                   String priority, boolean includeRealtime,
                                   RouteCallback callback) {

        Log.d(TAG, "🔍 [DEBUG] MultiModalRouteService.getMultiModalRoutes 시작");
        Log.d(TAG, "🔍 [DEBUG] 파라미터: " + startName + " → " + goalName +
                  ", 좌표: (" + startLat + ", " + startLng + ") → (" + goalLat + ", " + goalLng + ")");

        if (callback == null) {
            Log.e(TAG, "🔍 [DEBUG] Callback이 null입니다!");
            return;
        }

        if (executor == null || executor.isShutdown()) {
            Log.e(TAG, "🔍 [DEBUG] Executor가 종료되었습니다");
            callback.onError("서비스가 종료되었습니다");
            return;
        }

        Log.d(TAG, "🔍 [DEBUG] Executor에 작업 제출");

        try {
            executor.execute(() -> {
                Log.d(TAG, "🔍 [DEBUG] Executor 작업 시작 - Thread: " + Thread.currentThread().getName());
                List<RouteOption> allRoutes = new ArrayList<>();

                try {
                // 입력값 검증
                if (startLat == 0.0 || startLng == 0.0 || goalLat == 0.0 || goalLng == 0.0) {
                    Log.e(TAG, "잘못된 좌표: start(" + startLat + ", " + startLng + "), goal(" + goalLat + ", " + goalLng + ")");
                    callback.onError("좌표 정보가 올바르지 않습니다");
                    return;
                }

                if (startName == null || goalName == null || startName.trim().isEmpty() || goalName.trim().isEmpty()) {
                    Log.e(TAG, "잘못된 장소명: " + startName + " → " + goalName);
                    callback.onError("장소명이 올바르지 않습니다");
                    return;
                }

                // 실제 거리 계산 (API 사용)
                calculateRealDistanceAndGenerateRoutes(startLat, startLng, goalLat, goalLng,
                                                     startName, goalName, priority, includeRealtime,
                                                     allRoutes, callback);

                } catch (Exception e) {
                    Log.e(TAG, "🔍 [DEBUG] Multi Modal Route Exception", e);
                    callback.onError("경로 검색 오류: " + e.getMessage());
                }
            });
        } catch (Exception executorException) {
            Log.e(TAG, "🔍 [DEBUG] Executor 제출 실패", executorException);
            callback.onError("서비스 실행 오류: " + executorException.getMessage());
        }
    }

    /**
     * 실제 거리 계산 후 경로 생성
     */
    private void calculateRealDistanceAndGenerateRoutes(double startLat, double startLng,
                                                       double goalLat, double goalLng,
                                                       String startName, String goalName,
                                                       String priority, boolean includeRealtime,
                                                       List<RouteOption> allRoutes, RouteCallback callback) {

        Log.d(TAG, "실제 거리 계산 시작: " + startName + " → " + goalName);

        // 자동차 경로로 실제 거리 계산
        distanceService.calculateRealDistance(startLat, startLng, goalLat, goalLng, "driving",
            new DistanceCalculationService.DistanceCallback() {
                @Override
                public void onSuccess(DistanceCalculationService.DistanceInfo distanceInfo) {
                    try {
                        double realDistance = distanceInfo.distanceKm;
                        Log.d(TAG, "실제 거리 계산 완료: " + String.format("%.1f", realDistance) + "km");

                        // 실제 거리 기반으로 경로 생성
                        generateRoutesBasedOnDistance(startLat, startLng, goalLat, goalLng,
                                                    startName, goalName, realDistance,
                                                    priority, includeRealtime, allRoutes, callback);

                    } catch (Exception e) {
                        Log.e(TAG, "거리 기반 경로 생성 오류", e);
                        callback.onError("경로 생성 중 오류가 발생했습니다");
                    }
                }

                @Override
                public void onError(String error) {
                    Log.w(TAG, "실제 거리 계산 실패, 직선거리 사용: " + error);

                    // 실제 거리 계산 실패 시 직선거리 사용
                    double straightDistance = calculateDistance(startLat, startLng, goalLat, goalLng);
                    generateRoutesBasedOnDistance(startLat, startLng, goalLat, goalLng,
                                                startName, goalName, straightDistance,
                                                priority, includeRealtime, allRoutes, callback);
                }
            });
    }

    /**
     * 거리 기반 경로 생성
     */
    private void generateRoutesBasedOnDistance(double startLat, double startLng,
                                             double goalLat, double goalLng,
                                             String startName, String goalName, double distance,
                                             String priority, boolean includeRealtime,
                                             List<RouteOption> allRoutes, RouteCallback callback) {
        try {
            Log.d(TAG, "거리 기반 경로 생성 시작: " + String.format("%.1f", distance) + "km");

            if (distance <= 0.0 || distance > 1000.0) {
                Log.e(TAG, "비현실적인 거리: " + distance + "km");
                callback.onError("거리가 너무 멀거나 가깝습니다");
                return;
            }

            // 1. 도보 경로 (5km 이하)
            if (distance <= 5.0) {
                getWalkingRoute(startLat, startLng, goalLat, goalLng, distance, allRoutes);
            }

            // 2. 자전거 경로 (15km 이하)
            if (distance <= 15.0) {
                getBicycleRoute(startLat, startLng, goalLat, goalLng, distance, allRoutes);
            }

            // 3. 자동차 경로 (실제 API 사용)
            getDrivingRouteFromAPI(startLat, startLng, goalLat, goalLng, startName, goalName, allRoutes);

            // 4. 대중교통 경로 (30km 이하)
            if (distance <= 30.0) {
                getBusRoute(startLat, startLng, goalLat, goalLng, distance, allRoutes);
                getSubwayRoute(startLat, startLng, goalLat, goalLng, distance, allRoutes);
            }

            // 5. 기차 경로 (50km 이상)
            if (distance >= 50.0) {
                getTrainRoute(startLat, startLng, goalLat, goalLng, distance, allRoutes);
            }

            // 6. 비행기 경로 (200km 이상)
            if (distance >= 200.0) {
                getFlightRoute(startLat, startLng, goalLat, goalLng, distance, allRoutes);
            }

            // 최소 1개 경로는 있어야 함
            if (allRoutes.isEmpty()) {
                Log.e(TAG, "❌ 모든 경로 생성 실패 - 기본 경로 추가");

                // 기본 경로라도 추가 (앱 크래시 방지)
                RouteOption fallbackRoute = new RouteOption(
                    "walking", "🚶", "도보 (예상)",
                    String.format("%.1f km", distance),
                    formatDuration((int)(distance * 15)), // 도보 4km/h 기준
                    "무료",
                    "예상 경로입니다. 정확한 경로는 지도 앱을 확인해주세요."
                );
                fallbackRoute.priority = 1;
                fallbackRoute.isRecommended = true;
                allRoutes.add(fallbackRoute);

                Log.d(TAG, "✅ 기본 도보 경로 추가됨");
            }

            // 추천 경로 설정 및 정렬
            setRecommendedRoute(allRoutes, distance, priority, includeRealtime, startLat, startLng, goalLat, goalLng);
            sortRoutesByPriority(allRoutes, priority);

            Log.d(TAG, "경로 검색 완료: " + allRoutes.size() + "개 옵션");
            callback.onSuccess(allRoutes);

        } catch (Exception e) {
            Log.e(TAG, "거리 기반 경로 생성 오류", e);
            callback.onError("경로 생성 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 실제 네이버 Directions API를 사용한 자동차 경로 검색
     */
    private void getDrivingRouteFromAPI(double startLat, double startLng,
                                       double goalLat, double goalLng,
                                       String startName, String goalName,
                                       List<RouteOption> routes) {
        try {
            // 좌표 형식: longitude,latitude
            String startCoords = startLng + "," + startLat;
            String goalCoords = goalLng + "," + goalLat;
            
            String urlString = DIRECTIONS_URL + "/driving" +
                "?start=" + startCoords +
                "&goal=" + goalCoords +
                "&option=trafast";
            
            Log.d(TAG, "자동차 경로 API 호출: " + urlString);
            
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            
            connection.setRequestProperty("X-NCP-APIGW-API-KEY-ID", CLIENT_ID);
            connection.setRequestProperty("X-NCP-APIGW-API-KEY", CLIENT_SECRET);
            connection.setRequestProperty("Accept-Language", "ko");
            
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            
            int responseCode = connection.getResponseCode();
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), "UTF-8"));
                StringBuilder response = new StringBuilder();
                String line;
                
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                
                parseDrivingResponse(response.toString(), routes);
                
            } else {
                Log.e(TAG, "자동차 경로 API 오류: " + responseCode);
                // 폴백: 예상 자동차 경로
                addFallbackDrivingRoute(startLat, startLng, goalLat, goalLng, routes);
            }
            
            connection.disconnect();
            
        } catch (Exception e) {
            Log.e(TAG, "자동차 경로 Exception", e);
            // 폴백: 예상 자동차 경로
            addFallbackDrivingRoute(startLat, startLng, goalLat, goalLng, routes);
        }
    }

    /**
     * 도보 경로 추가
     */
    private void getWalkingRoute(double startLat, double startLng,
                                double goalLat, double goalLng,
                                double distance, List<RouteOption> routes) {

        // 도보 속도: 평균 4km/h (실제 보행 속도 고려)
        int walkingTimeMinutes = (int) Math.ceil((distance * 60) / 4.0);

        String walkingDistance = String.format("%.1f km", distance);
        String walkingDuration = formatDuration(walkingTimeMinutes);
        String walkingCost = "무료";

        // 거리별 설명 개선
        String walkingDescription;
        if (distance <= 0.5) {
            walkingDescription = "가까운 거리 • 빠른 도보 이동 • 건강에 좋음";
        } else if (distance <= 1.0) {
            walkingDescription = "적당한 거리 • 산책하기 좋음 • 환경친화적";
        } else {
            walkingDescription = "긴 거리 • 운동 효과 • 시간 여유 있을 때 추천";
        }

        RouteOption walkingRoute = new RouteOption(
            "walking", "🚶", "도보",
            walkingDistance, walkingDuration, walkingCost, walkingDescription);

        walkingRoute.priority = 3;

        // 1.3km 이하 (도보 20분 이하)면 추천
        if (distance <= 1.3) {
            walkingRoute.isRecommended = true;
            walkingRoute.priority = 1;
            Log.d(TAG, "도보 추천: " + walkingDistance + " (" + walkingDuration + ")");
        }

        routes.add(walkingRoute);
        Log.d(TAG, "도보 경로 추가: " + walkingDuration + ", " + walkingDistance);
    }

    /**
     * 자전거 경로 추가
     */
    private void getBicycleRoute(double startLat, double startLng,
                                double goalLat, double goalLng,
                                double distance, List<RouteOption> routes) {

        // 자전거 속도: 평균 15km/h
        int bicycleTimeMinutes = (int) Math.ceil((distance * 60) / 15.0);

        String bicycleDistance = String.format("%.1f km", distance);
        String bicycleDuration = formatDuration(bicycleTimeMinutes);
        String bicycleCost = "무료";

        // 거리별 설명
        String bicycleDescription;
        if (distance <= 3.0) {
            bicycleDescription = "가까운 거리 • 빠른 이동 • 운동 효과";
        } else if (distance <= 8.0) {
            bicycleDescription = "적당한 거리 • 경제적 • 환경친화적";
        } else {
            bicycleDescription = "긴 거리 • 체력 필요 • 시간 여유 있을 때";
        }

        RouteOption bicycleRoute = new RouteOption(
            "bicycle", "🚴", "자전거",
            bicycleDistance, bicycleDuration, bicycleCost, bicycleDescription);

        bicycleRoute.priority = 3;

        // 3-10km에서 추천 (자전거 최적 거리)
        if (distance >= 3.0 && distance <= 10.0) {
            bicycleRoute.isRecommended = true;
            bicycleRoute.priority = 2;
            Log.d(TAG, "자전거 추천: " + bicycleDistance + " (" + bicycleDuration + ")");
        }

        routes.add(bicycleRoute);
        Log.d(TAG, "자전거 경로 추가: " + bicycleDuration + ", " + bicycleDistance);
    }

    /**
     * 버스 경로 추가
     */
    private void getBusRoute(double startLat, double startLng,
                            double goalLat, double goalLng,
                            double distance, List<RouteOption> routes) {

        // 버스 예상 시간 (거리별 계산)
        int busTimeMinutes;
        String busCost;
        String busDescription;

        if (distance <= 5.0) {
            busTimeMinutes = (int) (distance * 10 + 15); // 시내버스
            busCost = "1,500원";
            busDescription = "시내버스 • 편리한 이동 • 경제적";
        } else if (distance <= 20.0) {
            busTimeMinutes = (int) (distance * 6 + 20); // 시외버스
            busCost = "2,500원";
            busDescription = "시외버스 • 중거리 이동 • 편안함";
        } else {
            busTimeMinutes = (int) (distance * 4 + 30); // 고속버스
            busCost = "15,000원";
            busDescription = "고속버스 • 장거리 이동 • 빠른 이동";
        }

        String busDistance = String.format("%.1f km", distance);
        String busDuration = formatDuration(busTimeMinutes);

        RouteOption busRoute = new RouteOption(
            "bus", "🚌", "버스",
            busDistance, busDuration, busCost, busDescription);

        busRoute.priority = 2;

        // 5-30km에서 추천
        if (distance >= 5.0 && distance <= 30.0) {
            busRoute.isRecommended = true;
            busRoute.priority = 1;
        }

        routes.add(busRoute);
        Log.d(TAG, "버스 경로 추가: " + busDuration + ", " + busCost);
    }

    /**
     * 지하철 경로 추가
     */
    private void getSubwayRoute(double startLat, double startLng,
                               double goalLat, double goalLng,
                               double distance, List<RouteOption> routes) {

        // 지하철 예상 시간
        int subwayTimeMinutes;
        String subwayCost = "1,500원";
        String subwayDescription;

        if (distance <= 10.0) {
            subwayTimeMinutes = (int) (distance * 5 + 10); // 도심 지하철
            subwayDescription = "지하철 • 정시성 • 빠른 이동";
        } else if (distance <= 25.0) {
            subwayTimeMinutes = (int) (distance * 4 + 15); // 광역 지하철
            subwayDescription = "광역지하철 • 환승 포함 • 편리함";
        } else {
            subwayTimeMinutes = (int) (distance * 3 + 20); // 수도권 전철
            subwayDescription = "수도권전철 • 장거리 • 경제적";
        }

        String subwayDistance = String.format("%.1f km", distance);
        String subwayDuration = formatDuration(subwayTimeMinutes);

        RouteOption subwayRoute = new RouteOption(
            "subway", "🚇", "지하철",
            subwayDistance, subwayDuration, subwayCost, subwayDescription);

        subwayRoute.priority = 1;

        // 3-25km에서 추천 (지하철 최적 구간)
        if (distance >= 3.0 && distance <= 25.0) {
            subwayRoute.isRecommended = true;
            subwayRoute.priority = 1;
        }

        routes.add(subwayRoute);
        Log.d(TAG, "지하철 경로 추가: " + subwayDuration + ", " + subwayCost);
    }

    // 대중교통 메서드는 버스와 지하철로 분리되어 제거됨

    /**
     * 자동차 경로 응답 파싱
     */
    private void parseDrivingResponse(String response, List<RouteOption> routes) {
        try {
            JSONObject jsonObject = new JSONObject(response);
            
            if (jsonObject.optInt("code", -1) != 0) {
                addFallbackDrivingRoute(0, 0, 0, 0, routes);
                return;
            }
            
            JSONObject route = jsonObject.optJSONObject("route");
            if (route == null) {
                addFallbackDrivingRoute(0, 0, 0, 0, routes);
                return;
            }
            
            JSONArray trafast = route.optJSONArray("trafast");
            if (trafast == null || trafast.length() == 0) {
                addFallbackDrivingRoute(0, 0, 0, 0, routes);
                return;
            }
            
            JSONObject routeInfo = trafast.getJSONObject(0);
            JSONObject summary = routeInfo.getJSONObject("summary");
            
            // 거리, 시간, 비용 정보 추출
            int distanceInMeters = summary.getInt("distance");
            String distance = String.format("%.1f km", distanceInMeters / 1000.0);
            
            int durationInMs = summary.getInt("duration");
            String duration = formatDuration(durationInMs / (1000 * 60));
            
            int tollFare = summary.optInt("tollFare", 0);
            int fuelPrice = summary.optInt("fuelPrice", 0);
            int totalCost = tollFare + fuelPrice;
            String cost = String.format("%,d원", totalCost);
            
            String description = "실시간 최적 경로 • 빠른 이동";
            if (tollFare > 0) {
                description += " • 통행료 포함";
            }
            
            RouteOption drivingRoute = new RouteOption(
                "driving", "🚗", "자동차", 
                distance, duration, cost, description);
            
            drivingRoute.priority = 2;
            
            routes.add(drivingRoute);
            Log.d(TAG, "자동차 경로 파싱 성공: " + duration + ", " + cost);
            
        } catch (Exception e) {
            Log.e(TAG, "자동차 경로 파싱 오류", e);
            addFallbackDrivingRoute(0, 0, 0, 0, routes);
        }
    }

    /**
     * 폴백 자동차 경로 추가
     */
    private void addFallbackDrivingRoute(double startLat, double startLng, 
                                        double goalLat, double goalLng, 
                                        List<RouteOption> routes) {
        
        double distance = calculateDistance(startLat, startLng, goalLat, goalLng);
        
        // 자동차 예상 시간 (평균 30km/h 도심 기준)
        int drivingTimeMinutes = (int) Math.ceil((distance * 60) / 30.0);
        
        // 예상 비용 (연료비 + 주차비)
        int estimatedCost = (int) (distance * 150) + 2000; // km당 150원 + 주차비 2000원
        
        String drivingDistance = String.format("%.1f km", distance);
        String drivingDuration = formatDuration(drivingTimeMinutes);
        String drivingCost = String.format("%,d원", estimatedCost);
        String drivingDescription = "예상 경로 • 편리한 이동 • 주차비 포함";
        
        RouteOption drivingRoute = new RouteOption(
            "driving", "🚗", "자동차", 
            drivingDistance, drivingDuration, drivingCost, drivingDescription);
        
        drivingRoute.priority = 2;
        
        routes.add(drivingRoute);
        Log.d(TAG, "폴백 자동차 경로 추가: " + drivingDuration + ", " + drivingCost);
    }

    /**
     * 기차 경로 추가 (장거리용)
     */
    private void getTrainRoute(double startLat, double startLng,
                              double goalLat, double goalLng,
                              double distance, List<RouteOption> routes) {

        // 기차 예상 시간 및 비용
        int trainTimeMinutes;
        String trainCost;
        String trainDescription;

        if (distance <= 100.0) {
            trainTimeMinutes = (int) (distance * 1.5 + 30); // 일반열차
            trainCost = "25,000원";
            trainDescription = "일반열차 • 편안한 이동 • 경제적";
        } else if (distance <= 300.0) {
            trainTimeMinutes = (int) (distance * 1.0 + 45); // KTX/SRT
            trainCost = "45,000원";
            trainDescription = "KTX/SRT • 고속철도 • 빠른 이동";
        } else {
            trainTimeMinutes = (int) (distance * 0.8 + 60); // 장거리 고속철도
            trainCost = "65,000원";
            trainDescription = "고속철도 • 장거리 • 최고 속도";
        }

        String trainDistance = String.format("%.1f km", distance);
        String trainDuration = formatDuration(trainTimeMinutes);

        RouteOption trainRoute = new RouteOption(
            "train", "🚂", "기차",
            trainDistance, trainDuration, trainCost, trainDescription);

        trainRoute.priority = 1;

        // 50-400km에서 추천 (기차 최적 구간)
        if (distance >= 50.0 && distance <= 400.0) {
            trainRoute.isRecommended = true;
            trainRoute.priority = 1;
        }

        routes.add(trainRoute);
        Log.d(TAG, "기차 경로 추가: " + trainDuration + ", " + trainCost);
    }

    /**
     * 비행기 경로 추가 (초장거리용)
     */
    private void getFlightRoute(double startLat, double startLng,
                               double goalLat, double goalLng,
                               double distance, List<RouteOption> routes) {

        // 비행기 예상 시간 및 비용 (공항 이동 시간 포함)
        int flightTimeMinutes;
        String flightCost;
        String flightDescription;

        if (distance <= 500.0) {
            flightTimeMinutes = (int) (distance * 0.3 + 120); // 국내선 (공항 2시간 포함)
            flightCost = "120,000원";
            flightDescription = "국내선 • 가장 빠름 • 공항 이동 포함";
        } else if (distance <= 2000.0) {
            flightTimeMinutes = (int) (distance * 0.2 + 180); // 단거리 국제선
            flightCost = "300,000원";
            flightDescription = "단거리 국제선 • 빠른 이동 • 공항 절차 포함";
        } else {
            flightTimeMinutes = (int) (distance * 0.15 + 240); // 장거리 국제선
            flightCost = "800,000원";
            flightDescription = "장거리 국제선 • 초고속 • 유일한 선택";
        }

        String flightDistance = String.format("%.1f km", distance);
        String flightDuration = formatDuration(flightTimeMinutes);

        RouteOption flightRoute = new RouteOption(
            "flight", "✈️", "비행기",
            flightDistance, flightDuration, flightCost, flightDescription);

        flightRoute.priority = 1;

        // 200km 이상에서 추천 (비행기 최적 구간)
        if (distance >= 200.0) {
            flightRoute.isRecommended = true;
            flightRoute.priority = 1;
        }

        routes.add(flightRoute);
        Log.d(TAG, "비행기 경로 추가: " + flightDuration + ", " + flightCost);
    }

    /**
     * 추천 경로 설정 (거리별 최적 교통수단)
     */
    private void setRecommendedRoute(List<RouteOption> routes, double distance) {
        if (routes.isEmpty()) return;

        String recommendedMode = "";

        // 거리별 최적 교통수단 결정
        if (distance <= 1.3) {
            // 1.3km 이하: 도보 추천 (20분 이하)
            recommendedMode = "walking";
            Log.d(TAG, "도보 추천 설정: " + distance + "km (도보 20분 이하)");
        } else if (distance <= 3.0) {
            // 3km 이하: 자전거 추천 (빠르고 경제적)
            recommendedMode = "bicycle";
            Log.d(TAG, "자전거 추천 설정: " + distance + "km (자전거 최적)");
        } else if (distance <= 10.0) {
            // 10km 이하: 지하철 추천 (정시성, 편리함)
            recommendedMode = "subway";
            Log.d(TAG, "지하철 추천 설정: " + distance + "km (지하철 최적)");
        } else if (distance <= 30.0) {
            // 30km 이하: 버스 추천 (경제적, 편리함)
            recommendedMode = "bus";
            Log.d(TAG, "버스 추천 설정: " + distance + "km (버스 최적)");
        } else if (distance <= 100.0) {
            // 100km 이하: 자동차 추천 (중거리 최적)
            recommendedMode = "driving";
            Log.d(TAG, "자동차 추천 설정: " + distance + "km (자동차 최적)");
        } else if (distance <= 400.0) {
            // 400km 이하: 기차 추천 (장거리 최적)
            recommendedMode = "train";
            Log.d(TAG, "기차 추천 설정: " + distance + "km (기차 최적)");
        } else {
            // 400km 이상: 비행기 추천 (초장거리 유일)
            recommendedMode = "flight";
            Log.d(TAG, "비행기 추천 설정: " + distance + "km (비행기 필수)");
        }

        // 추천 교통수단 설정
        for (RouteOption route : routes) {
            if (recommendedMode.equals(route.transportMode)) {
                route.isRecommended = true;
                route.priority = 1;
                Log.d(TAG, "추천 설정 완료: " + route.transportName + " (" + route.transportIcon + ")");
                break;
            }
        }

        // 추천 교통수단이 없으면 첫 번째 옵션을 추천
        boolean hasRecommended = routes.stream().anyMatch(route -> route.isRecommended);
        if (!hasRecommended && !routes.isEmpty()) {
            routes.get(0).isRecommended = true;
            routes.get(0).priority = 1;
            Log.d(TAG, "기본 추천 설정: " + routes.get(0).transportName);
        }
    }

    /**
     * 두 지점 간 직선 거리 계산 (Haversine formula)
     */
    private double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        final int R = 6371; // 지구 반지름 (km)
        
        double latDistance = Math.toRadians(lat2 - lat1);
        double lngDistance = Math.toRadians(lng2 - lng1);
        
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lngDistance / 2) * Math.sin(lngDistance / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return R * c;
    }

    /**
     * 사용자 우선순위에 따른 추천 경로 설정 (실제 좌표 사용)
     */
    private void setRecommendedRoute(List<RouteOption> routes, double distance,
                                   String priority, boolean includeRealtime,
                                   double startLat, double startLng,
                                   double goalLat, double goalLng) {
        if (routes.isEmpty()) return;

        Log.d(TAG, "추천 경로 설정 - 우선순위: " + priority + ", 실시간 데이터: " + includeRealtime);

        // 기존 추천 초기화
        for (RouteOption route : routes) {
            route.isRecommended = false;
        }

        RouteOption bestRoute = null;

        if ("cost".equals(priority)) {
            // 비용 우선: 가장 저렴한 경로
            bestRoute = routes.stream()
                .filter(r -> !r.cost.equals("무료")) // 무료 제외하고 가장 저렴한 유료 경로
                .min((a, b) -> {
                    int costA = extractCostValue(a.cost);
                    int costB = extractCostValue(b.cost);
                    return Integer.compare(costA, costB);
                })
                .orElse(routes.stream()
                    .filter(r -> r.cost.equals("무료"))
                    .findFirst()
                    .orElse(routes.get(0)));
        } else {
            // 시간 우선: 가장 빠른 경로
            bestRoute = routes.stream()
                .min((a, b) -> {
                    int timeA = extractTimeValue(a.duration);
                    int timeB = extractTimeValue(b.duration);
                    return Integer.compare(timeA, timeB);
                })
                .orElse(routes.get(0));
        }

        if (bestRoute != null) {
            bestRoute.isRecommended = true;
            bestRoute.priority = 0; // 최우선
            Log.d(TAG, "추천 경로 선택: " + bestRoute.transportName + " (" + bestRoute.duration + ")");
        }

        // 실시간 데이터 반영 (실제 좌표 사용)
        if (includeRealtime) {
            applyRealtimeFactorsWithCoords(routes, startLat, startLng, goalLat, goalLng);
        }
    }

    /**
     * 사용자 우선순위에 따른 경로 정렬
     */
    private void sortRoutesByPriority(List<RouteOption> routes, String priority) {
        if ("cost".equals(priority)) {
            // 비용 우선 정렬
            routes.sort((a, b) -> {
                // 추천 경로가 먼저
                if (a.isRecommended != b.isRecommended) {
                    return a.isRecommended ? -1 : 1;
                }
                // 비용 순으로 정렬
                int costA = extractCostValue(a.cost);
                int costB = extractCostValue(b.cost);
                return Integer.compare(costA, costB);
            });
        } else {
            // 시간 우선 정렬 (기본)
            routes.sort((a, b) -> {
                // 추천 경로가 먼저
                if (a.isRecommended != b.isRecommended) {
                    return a.isRecommended ? -1 : 1;
                }
                // 시간 순으로 정렬
                int timeA = extractTimeValue(a.duration);
                int timeB = extractTimeValue(b.duration);
                return Integer.compare(timeA, timeB);
            });
        }
    }

    /**
     * 실시간 데이터 반영 (실제 좌표 사용)
     */
    private void applyRealtimeFactorsWithCoords(List<RouteOption> routes,
                                              double startLat, double startLng,
                                              double goalLat, double goalLng) {
        if (routes.isEmpty()) return;

        Log.d(TAG, "실시간 데이터 적용 시작 - 좌표: (" + startLat + ", " + startLng + ") → (" + goalLat + ", " + goalLng + ")");

        // 각 교통수단별로 실시간 교통 정보 적용
        for (RouteOption route : routes) {
            applyRealTimeTrafficToRoute(route, startLat, startLng, goalLat, goalLng);
        }
    }

    /**
     * 실시간 데이터 반영 (기본 좌표 사용 - 하위 호환성)
     */
    private void applyRealtimeFactors(List<RouteOption> routes) {
        if (routes.isEmpty()) return;

        // 서울 기본 좌표 사용
        double lat = 37.5665;
        double lng = 126.9780;

        // 각 교통수단별로 실시간 교통 정보 적용
        for (RouteOption route : routes) {
            applyRealTimeTrafficToRoute(route, lat, lng, lat, lng);
        }
    }

    /**
     * 개별 경로에 실시간 교통 정보 적용 (실제 좌표 사용)
     */
    private void applyRealTimeTrafficToRoute(RouteOption route, double startLat, double startLng,
                                           double goalLat, double goalLng) {
        String transportMode = getTransportModeForAPI(route.transportName);

        trafficService.getRealTimeTrafficCondition(startLat, startLng, goalLat, goalLng, transportMode,
            new RealTimeTrafficService.TrafficCallback() {
                @Override
                public void onSuccess(RealTimeTrafficService.TrafficCondition condition) {
                    try {
                        String originalDuration = route.duration;

                        // 실시간 교통 배수 적용
                        if (condition.trafficMultiplier > 1.0) {
                            int originalTime = extractTimeValue(route.duration);
                            int adjustedTime = (int) (originalTime * condition.trafficMultiplier);
                            route.duration = formatDuration(adjustedTime);

                            // 설명에 실시간 정보 추가
                            if (!condition.description.isEmpty()) {
                                route.description += " • " + condition.description;
                            }

                            // 교통 레벨 표시
                            if (!"원활".equals(condition.trafficLevel)) {
                                route.description += " • " + condition.trafficLevel;
                            }

                            Log.d(TAG, String.format("실시간 조정: %s %s → %s (배수: %.1f)",
                                  route.transportName, originalDuration, route.duration, condition.trafficMultiplier));
                        }

                        // 날씨 영향 추가
                        if (condition.isBadWeather && !condition.weatherImpact.isEmpty()) {
                            route.description += " • " + condition.weatherImpact;
                        }

                    } catch (Exception e) {
                        Log.e(TAG, "Error applying real-time traffic to route: " + route.transportName, e);
                    }
                }

                @Override
                public void onError(String error) {
                    Log.w(TAG, "Failed to get real-time traffic for " + route.transportName + ": " + error);
                    // 실시간 정보 실패해도 기본 경로는 유지
                }
            });
    }

    /**
     * 실제 네이버 대중교통 API를 사용한 경로 검색
     */
    private void getPublicTransitRouteFromAPI(double startLat, double startLng,
                                            double goalLat, double goalLng,
                                            String startName, String goalName,
                                            List<RouteOption> routes) {
        try {
            // 네이버 대중교통 API 호출
            String startCoords = startLng + "," + startLat;
            String goalCoords = goalLng + "," + goalLat;

            String urlString = DIRECTIONS_URL + "/transit" +
                "?start=" + startCoords +
                "&goal=" + goalCoords +
                "&option=trafast";

            Log.d(TAG, "대중교통 경로 API 호출: " + urlString);

            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            connection.setRequestProperty("X-NCP-APIGW-API-KEY-ID", CLIENT_ID);
            connection.setRequestProperty("X-NCP-APIGW-API-KEY", CLIENT_SECRET);
            connection.setRequestProperty("Accept-Language", "ko");

            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            int responseCode = connection.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), "UTF-8"));
                StringBuilder response = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                parseTransitResponse(response.toString(), routes);

            } else {
                Log.w(TAG, "대중교통 API 오류: " + responseCode + ", 기본 경로 사용");
                // API 실패 시 기본 버스/지하철 경로 추가
                double distance = calculateDistance(startLat, startLng, goalLat, goalLng);
                if (distance <= 30.0) {
                    getBusRoute(startLat, startLng, goalLat, goalLng, distance, routes);
                }
                if (distance <= 25.0) {
                    getSubwayRoute(startLat, startLng, goalLat, goalLng, distance, routes);
                }
            }

            connection.disconnect();

        } catch (Exception e) {
            Log.e(TAG, "대중교통 API Exception", e);
            // 예외 발생 시 기본 경로 추가
            double distance = calculateDistance(startLat, startLng, goalLat, goalLng);
            if (distance <= 30.0) {
                getBusRoute(startLat, startLng, goalLat, goalLng, distance, routes);
            }
            if (distance <= 25.0) {
                getSubwayRoute(startLat, startLng, goalLat, goalLng, distance, routes);
            }
        }
    }

    /**
     * 대중교통 API 응답 파싱
     */
    private void parseTransitResponse(String response, List<RouteOption> routes) {
        try {
            JSONObject jsonObject = new JSONObject(response);

            if (jsonObject.optInt("code", -1) != 0) {
                Log.w(TAG, "대중교통 API 응답 코드 오류");
                return;
            }

            JSONObject route = jsonObject.optJSONObject("route");
            if (route == null) {
                Log.w(TAG, "대중교통 경로 정보 없음");
                return;
            }

            JSONArray trafast = route.optJSONArray("trafast");
            if (trafast == null || trafast.length() == 0) {
                Log.w(TAG, "대중교통 경로 배열 없음");
                return;
            }

            // 첫 번째 경로 정보 파싱
            JSONObject routeInfo = trafast.getJSONObject(0);
            JSONObject summary = routeInfo.getJSONObject("summary");

            // 거리, 시간 정보 추출
            int distanceInMeters = summary.getInt("distance");
            String distance = String.format("%.1f km", distanceInMeters / 1000.0);

            int durationInMs = summary.getInt("duration");
            String duration = formatDuration(durationInMs / (1000 * 60));

            // 대중교통 비용 (기본값)
            String cost = "1,500원";
            String description = "실시간 대중교통 • 정시성 • 환승 최적화";

            RouteOption transitRoute = new RouteOption(
                "transit", "🚌🚇", "대중교통",
                distance, duration, cost, description);

            transitRoute.priority = 1;

            routes.add(transitRoute);
            Log.d(TAG, "대중교통 경로 파싱 성공: " + duration + ", " + cost);

        } catch (Exception e) {
            Log.e(TAG, "대중교통 경로 파싱 오류", e);
        }
    }

    /**
     * 교통수단명을 API용 모드로 변환
     */
    private String getTransportModeForAPI(String transportName) {
        switch (transportName) {
            case "도보": return "walking";
            case "자전거": return "bicycle";
            case "버스": return "bus";
            case "지하철": return "subway";
            case "자동차": return "driving";
            case "기차": return "train";
            case "비행기": return "flight";
            case "대중교통": return "transit";
            default: return "driving";
        }
    }

    /**
     * 비용 문자열에서 숫자 추출
     */
    private int extractCostValue(String cost) {
        if ("무료".equals(cost)) return 0;
        try {
            return Integer.parseInt(cost.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return Integer.MAX_VALUE;
        }
    }

    /**
     * 시간 문자열에서 분 단위 숫자 추출
     */
    private int extractTimeValue(String duration) {
        try {
            int totalMinutes = 0;
            if (duration.contains("시간")) {
                String[] parts = duration.split("시간");
                totalMinutes += Integer.parseInt(parts[0].trim()) * 60;
                if (parts.length > 1 && parts[1].contains("분")) {
                    totalMinutes += Integer.parseInt(parts[1].replaceAll("[^0-9]", ""));
                }
            } else if (duration.contains("분")) {
                totalMinutes = Integer.parseInt(duration.replaceAll("[^0-9]", ""));
            }
            return totalMinutes;
        } catch (NumberFormatException e) {
            return Integer.MAX_VALUE;
        }
    }

    /**
     * 시간 포맷팅
     */
    private String formatDuration(int minutes) {
        if (minutes < 60) {
            return minutes + "분";
        } else {
            int hours = minutes / 60;
            int remainingMinutes = minutes % 60;
            if (remainingMinutes == 0) {
                return hours + "시간";
            } else {
                return hours + "시간 " + remainingMinutes + "분";
            }
        }
    }

    /**
     * 서비스 종료
     */
    public void shutdown() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
        if (trafficService != null) {
            trafficService.shutdown();
        }
        if (weatherService != null) {
            weatherService.shutdown();
        }
        if (distanceService != null) {
            distanceService.shutdown();
        }
    }
}
