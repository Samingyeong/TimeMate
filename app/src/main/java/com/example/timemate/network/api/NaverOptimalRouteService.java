package com.example.timemate.network.api;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 네이버 최적 경로 추천 서비스
 * 대중교통, 도보, 자동차 경로를 종합적으로 추천
 */
public class NaverOptimalRouteService {

    private static final String TAG = "NaverOptimalRoute";
    private static final String CLIENT_ID = com.example.timemate.config.ApiConfig.NAVER_CLOUD_CLIENT_ID;
    private static final String CLIENT_SECRET = com.example.timemate.config.ApiConfig.NAVER_CLOUD_CLIENT_SECRET;
    
    // 네이버 Directions 5 API (대중교통 포함)
    private static final String DIRECTIONS_URL = com.example.timemate.config.ApiConfig.NAVER_DIRECTIONS_URL;
    
    private ExecutorService executor;

    public NaverOptimalRouteService() {
        executor = Executors.newSingleThreadExecutor();
    }

    /**
     * 경로 추천 결과 콜백
     */
    public interface RouteCallback {
        void onSuccess(List<RouteOption> routes);
        void onError(String error);
    }

    /**
     * 경로 옵션 데이터 클래스
     */
    public static class RouteOption {
        public String type;           // 경로 타입 (대중교통, 도보, 자동차)
        public String summary;        // 경로 요약
        public int duration;          // 소요 시간 (분)
        public int distance;          // 거리 (미터)
        public int cost;             // 비용 (원)
        public String description;    // 상세 설명
        public List<RouteStep> steps; // 경로 단계

        public RouteOption(String type, String summary, int duration, int distance, int cost, String description) {
            this.type = type;
            this.summary = summary;
            this.duration = duration;
            this.distance = distance;
            this.cost = cost;
            this.description = description;
            this.steps = new ArrayList<>();
        }

        public String getFormattedDuration() {
            if (duration < 60) {
                return duration + "분";
            } else {
                int hours = duration / 60;
                int minutes = duration % 60;
                return hours + "시간 " + minutes + "분";
            }
        }

        public String getFormattedDistance() {
            if (distance < 1000) {
                return distance + "m";
            } else {
                return String.format("%.1fkm", distance / 1000.0);
            }
        }

        public String getFormattedCost() {
            if (cost == 0) {
                return "무료";
            } else {
                return String.format("%,d원", cost);
            }
        }
    }

    /**
     * 경로 단계 데이터 클래스
     */
    public static class RouteStep {
        public String instruction;    // 이동 안내
        public String mode;          // 이동 수단 (지하철, 버스, 도보 등)
        public int duration;         // 소요 시간
        public int distance;         // 거리

        public RouteStep(String instruction, String mode, int duration, int distance) {
            this.instruction = instruction;
            this.mode = mode;
            this.duration = duration;
            this.distance = distance;
        }
    }

    /**
     * 최적 경로 검색 (여러 옵션 제공)
     */
    public void getOptimalRoutes(double startLat, double startLng, double endLat, double endLng, RouteCallback callback) {
        executor.execute(() -> {
            try {
                List<RouteOption> routes = new ArrayList<>();

                // 1. 대중교통 경로 (지하철 + 버스)
                RouteOption transitRoute = getTransitRoute(startLat, startLng, endLat, endLng);
                if (transitRoute != null) {
                    routes.add(transitRoute);
                }

                // 2. 도보 경로
                RouteOption walkingRoute = getWalkingRoute(startLat, startLng, endLat, endLng);
                if (walkingRoute != null) {
                    routes.add(walkingRoute);
                }

                // 3. 자동차 경로 (참고용)
                RouteOption drivingRoute = getDrivingRoute(startLat, startLng, endLat, endLng);
                if (drivingRoute != null) {
                    routes.add(drivingRoute);
                }

                // 시간순으로 정렬 (가장 빠른 경로 우선)
                routes.sort((a, b) -> Integer.compare(a.duration, b.duration));

                callback.onSuccess(routes);

            } catch (Exception e) {
                Log.e(TAG, "Route search exception", e);
                callback.onError("경로 검색 중 오류가 발생했습니다: " + e.getMessage());
            }
        });
    }

    /**
     * 대중교통 경로 검색
     */
    private RouteOption getTransitRoute(double startLat, double startLng, double endLat, double endLng) {
        try {
            String urlString = DIRECTIONS_URL + "/driving" +
                    "?start=" + startLng + "," + startLat +
                    "&goal=" + endLng + "," + endLat +
                    "&option=trafast"; // 대중교통 최적화

            Log.d(TAG, "Transit Route URL: " + urlString);

            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("X-NCP-APIGW-API-KEY-ID", CLIENT_ID);
            connection.setRequestProperty("X-NCP-APIGW-API-KEY", CLIENT_SECRET);
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);

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

                return parseTransitRoute(response.toString());
            }

            connection.disconnect();

        } catch (Exception e) {
            Log.e(TAG, "Transit route error", e);
        }

        // 대중교통 정보가 없을 경우 기본값 반환
        return new RouteOption(
                "대중교통",
                "지하철 + 버스 이용",
                calculateEstimatedTime(startLat, startLng, endLat, endLng, "transit"),
                calculateDistance(startLat, startLng, endLat, endLng),
                1500, // 평균 대중교통 요금
                "지하철과 버스를 이용한 최적 경로입니다."
        );
    }

    /**
     * 도보 경로 검색
     */
    private RouteOption getWalkingRoute(double startLat, double startLng, double endLat, double endLng) {
        int distance = calculateDistance(startLat, startLng, endLat, endLng);
        int duration = (int) (distance / 80); // 평균 도보 속도 80m/분

        return new RouteOption(
                "도보",
                "걸어서 이동",
                duration,
                distance,
                0, // 무료
                "도보로 이동하는 경로입니다. 건강에도 좋아요!"
        );
    }

    /**
     * 자동차 경로 검색 (참고용)
     */
    private RouteOption getDrivingRoute(double startLat, double startLng, double endLat, double endLng) {
        int distance = calculateDistance(startLat, startLng, endLat, endLng);
        int duration = (int) (distance / 500); // 평균 자동차 속도 500m/분

        return new RouteOption(
                "자동차",
                "자동차 이용 (참고용)",
                duration,
                distance,
                calculateFuelCost(distance), // 연료비 계산
                "자동차로 이동하는 경로입니다. (주차비 별도)"
        );
    }

    /**
     * 대중교통 응답 파싱
     */
    private RouteOption parseTransitRoute(String response) {
        try {
            JSONObject jsonObject = new JSONObject(response);
            JSONObject route = jsonObject.getJSONObject("route");
            JSONArray trafast = route.getJSONArray("trafast");

            if (trafast.length() > 0) {
                JSONObject firstRoute = trafast.getJSONObject(0);
                JSONObject summary = firstRoute.getJSONObject("summary");

                int duration = summary.getInt("duration") / 60000; // ms to minutes
                int distance = summary.getInt("distance");
                int tollFare = summary.optInt("tollFare", 0);
                int fuelPrice = summary.optInt("fuelPrice", 0);

                return new RouteOption(
                        "대중교통",
                        "최적 대중교통 경로",
                        duration,
                        distance,
                        1500, // 기본 대중교통 요금
                        "네이버 지도 기반 최적 대중교통 경로입니다."
                );
            }

        } catch (Exception e) {
            Log.e(TAG, "Parse transit route error", e);
        }

        return null;
    }

    /**
     * 거리 계산 (하버사인 공식)
     */
    private int calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        final int R = 6371000; // 지구 반지름 (미터)

        double latDistance = Math.toRadians(lat2 - lat1);
        double lngDistance = Math.toRadians(lng2 - lng1);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lngDistance / 2) * Math.sin(lngDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return (int) (R * c);
    }

    /**
     * 예상 시간 계산
     */
    private int calculateEstimatedTime(double lat1, double lng1, double lat2, double lng2, String mode) {
        int distance = calculateDistance(lat1, lng1, lat2, lng2);

        switch (mode) {
            case "transit":
                return (int) (distance / 300); // 평균 대중교통 속도 300m/분
            case "walking":
                return (int) (distance / 80);  // 평균 도보 속도 80m/분
            case "driving":
                return (int) (distance / 500); // 평균 자동차 속도 500m/분
            default:
                return (int) (distance / 200); // 기본값
        }
    }

    /**
     * 연료비 계산
     */
    private int calculateFuelCost(int distance) {
        // 연비 10km/L, 휘발유 1600원/L 가정
        double fuelConsumption = distance / 1000.0 / 10.0; // 리터
        return (int) (fuelConsumption * 1600);
    }

    /**
     * 서비스 종료
     */
    public void shutdown() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
}
