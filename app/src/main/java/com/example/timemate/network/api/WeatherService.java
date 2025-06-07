package com.example.timemate.network.api;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 날씨 API 서비스
 * OpenWeatherMap API를 사용하여 날씨 정보 제공
 */
public class WeatherService {
    
    private static final String TAG = "WeatherService";
    private static final String API_KEY = "ce9bd35482e8143005005ee942964c02";
    private static final String BASE_URL = "https://api.openweathermap.org/data/2.5/weather";
    
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    /**
     * 날씨 데이터 모델
     */
    public static class WeatherData {
        public double temperature;      // 온도 (섭씨)
        public double feelsLike;       // 체감온도
        public int humidity;           // 습도 (%)
        public String description;     // 날씨 설명
        public String main;            // 주요 날씨
        public double windSpeed;       // 풍속 (m/s)
        public int pressure;           // 기압 (hPa)
        public String cityName;        // 도시명

        public WeatherData() {}

        @Override
        public String toString() {
            return String.format("%s, %.1f°C (체감 %.1f°C), 습도 %d%%", 
                description, temperature, feelsLike, humidity);
        }
    }

    /**
     * 날씨 API 콜백 인터페이스
     */
    public interface WeatherCallback {
        void onSuccess(WeatherData weather);
        void onError(String error);
    }

    /**
     * 현재 날씨 정보 가져오기 (좌표 기반)
     */
    public void getCurrentWeather(double latitude, double longitude, WeatherCallback callback) {
        executor.execute(() -> {
            try {
                String urlString = String.format(
                    "%s?lat=%.6f&lon=%.6f&appid=%s&units=metric&lang=kr",
                    BASE_URL, latitude, longitude, API_KEY
                );
                
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);
                
                int responseCode = connection.getResponseCode();
                Log.d(TAG, "Weather API Response Code: " + responseCode);
                
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream())
                    );
                    StringBuilder response = new StringBuilder();
                    String line;
                    
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();
                    
                    Log.d(TAG, "Weather API response: " + response.toString());
                    parseWeatherResponse(response.toString(), callback);
                } else {
                    callback.onError("날씨 API 호출 실패: " + responseCode);
                }
                
                connection.disconnect();
                
            } catch (Exception e) {
                Log.e(TAG, "Weather API Exception", e);
                callback.onError("네트워크 오류: " + e.getMessage());
            }
        });
    }

    /**
     * 도시명으로 날씨 정보 가져오기
     */
    public void getCurrentWeatherByCity(String cityName, WeatherCallback callback) {
        executor.execute(() -> {
            try {
                String urlString = String.format(
                    "%s?q=%s&appid=%s&units=metric&lang=kr",
                    BASE_URL, cityName, API_KEY
                );
                
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);
                
                int responseCode = connection.getResponseCode();
                
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream())
                    );
                    StringBuilder response = new StringBuilder();
                    String line;
                    
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();
                    
                    parseWeatherResponse(response.toString(), callback);
                } else {
                    callback.onError("날씨 API 호출 실패: " + responseCode);
                }
                
                connection.disconnect();
                
            } catch (Exception e) {
                Log.e(TAG, "Weather API Exception", e);
                callback.onError("네트워크 오류: " + e.getMessage());
            }
        });
    }

    /**
     * 날씨 API 응답 파싱
     */
    private void parseWeatherResponse(String response, WeatherCallback callback) {
        try {
            JSONObject jsonObject = new JSONObject(response);
            
            WeatherData weather = new WeatherData();
            
            // 기본 날씨 정보
            JSONObject main = jsonObject.getJSONObject("main");
            weather.temperature = main.getDouble("temp");
            weather.feelsLike = main.getDouble("feels_like");
            weather.humidity = main.getInt("humidity");
            weather.pressure = main.getInt("pressure");
            
            // 날씨 설명
            JSONArray weatherArray = jsonObject.getJSONArray("weather");
            if (weatherArray.length() > 0) {
                JSONObject weatherObj = weatherArray.getJSONObject(0);
                weather.main = weatherObj.getString("main");
                weather.description = weatherObj.getString("description");
            }
            
            // 바람 정보
            if (jsonObject.has("wind")) {
                JSONObject wind = jsonObject.getJSONObject("wind");
                weather.windSpeed = wind.optDouble("speed", 0.0);
            }
            
            // 도시명
            weather.cityName = jsonObject.optString("name", "알 수 없음");
            
            Log.d(TAG, "Weather parsed: " + weather.toString());
            callback.onSuccess(weather);
            
        } catch (Exception e) {
            Log.e(TAG, "Weather parsing error", e);
            callback.onError("날씨 데이터 파싱 오류: " + e.getMessage());
        }
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
