package com.example.timemate.features.home.service;

import android.util.Log;
import com.example.timemate.core.config.ApiConfig;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 날씨 정보 서비스
 * OpenWeather API를 사용하여 날씨 정보 제공
 */
public class WeatherService {

    private static final String TAG = "WeatherService";
    private static final String BASE_URL = "https://api.openweathermap.org/data/2.5/";
    private static final String API_KEY = ApiConfig.OPENWEATHER_API_KEY;

    private ExecutorService executor;

    public interface WeatherCallback {
        void onSuccess(WeatherInfo weather);
        void onError(String error);
    }

    public static class WeatherInfo {
        public String city;
        public double temperature;
        public String description;
        public String icon;
        public int humidity;
        public double windSpeed;
        public long sunrise;
        public long sunset;

        public String getDisplayText() {
            return String.format("%s %.1f°C %s", city, temperature, description);
        }

        public String getDetailedText() {
            return String.format("%s\n%.1f°C %s\n습도: %d%% | 바람: %.1fm/s",
                city, temperature, description, humidity, windSpeed);
        }

        public boolean isDay() {
            long currentTime = System.currentTimeMillis() / 1000;
            return currentTime >= sunrise && currentTime <= sunset;
        }

        // Getter 메서드들 추가
        public String getCityName() {
            return city != null ? city : "서울";
        }

        public double getTemperature() {
            return temperature;
        }

        public String getDescription() {
            return description != null ? description : "날씨 정보 없음";
        }

        public double getFeelsLike() {
            // 체감온도는 실제 온도와 비슷하게 설정 (실제 API에서는 feels_like 필드 사용)
            return temperature;
        }

        public int getHumidity() {
            return humidity;
        }
    }

    public WeatherService() {
        executor = Executors.newSingleThreadExecutor();
    }

    /**
     * 현재 날씨 정보 조회
     */
    public void getCurrentWeather(String city, WeatherCallback callback) {
        executor.execute(() -> {
            try {
                String url = BASE_URL + "weather?q=" + city + "&appid=" + API_KEY + "&units=metric&lang=kr";
                
                Log.d(TAG, "Weather API URL: " + url);

                HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
                connection.setRequestMethod("GET");
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

                    WeatherInfo weather = parseWeatherResponse(response.toString());
                    callback.onSuccess(weather);

                } else {
                    Log.e(TAG, "Weather API error: " + responseCode);
                    callback.onError("날씨 정보를 가져올 수 없습니다 (코드: " + responseCode + ")");
                }

                connection.disconnect();

            } catch (Exception e) {
                Log.e(TAG, "Weather API exception", e);
                callback.onError("네트워크 오류: " + e.getMessage());
            }
        });
    }

    /**
     * 5일 날씨 예보 조회
     */
    public void getForecast(String city, ForecastCallback callback) {
        executor.execute(() -> {
            try {
                String url = BASE_URL + "forecast?q=" + city + "&appid=" + API_KEY + "&units=metric&lang=kr";
                
                HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
                connection.setRequestMethod("GET");
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

                    // TODO: 예보 데이터 파싱
                    callback.onSuccess("예보 데이터");

                } else {
                    callback.onError("예보 정보를 가져올 수 없습니다");
                }

                connection.disconnect();

            } catch (Exception e) {
                Log.e(TAG, "Forecast API exception", e);
                callback.onError("네트워크 오류: " + e.getMessage());
            }
        });
    }

    public interface ForecastCallback {
        void onSuccess(String forecast);
        void onError(String error);
    }

    /**
     * 날씨 API 응답 파싱
     */
    private WeatherInfo parseWeatherResponse(String response) throws Exception {
        JSONObject json = new JSONObject(response);
        
        WeatherInfo weather = new WeatherInfo();
        weather.city = json.getString("name");
        
        JSONObject main = json.getJSONObject("main");
        weather.temperature = main.getDouble("temp");
        weather.humidity = main.getInt("humidity");
        
        JSONObject weatherObj = json.getJSONArray("weather").getJSONObject(0);
        weather.description = weatherObj.getString("description");
        weather.icon = weatherObj.getString("icon");
        
        if (json.has("wind")) {
            JSONObject wind = json.getJSONObject("wind");
            weather.windSpeed = wind.optDouble("speed", 0);
        }
        
        if (json.has("sys")) {
            JSONObject sys = json.getJSONObject("sys");
            weather.sunrise = sys.optLong("sunrise", 0);
            weather.sunset = sys.optLong("sunset", 0);
        }

        Log.d(TAG, "Parsed weather: " + weather.getDisplayText());
        return weather;
    }

    public void shutdown() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
}
