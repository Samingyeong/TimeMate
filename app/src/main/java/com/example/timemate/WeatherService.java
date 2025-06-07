package com.example.timemate;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class WeatherService {
    private static final String TAG = "WeatherService";
    // OpenWeather API 키
    private static final String API_KEY = "ce9bd35482e8143005005ee942964c02";
    private static final String BASE_URL = "https://api.openweathermap.org/data/2.5/weather";
    
    private OkHttpClient client;
    private Gson gson;

    public WeatherService() {
        client = new OkHttpClient();
        gson = new Gson();
    }

    public interface WeatherCallback {
        void onSuccess(WeatherData weatherData);
        void onError(String error);
    }

    // 도시 이름으로 날씨 정보 가져오기
    public void getWeatherByCity(String cityName, WeatherCallback callback) {
        String url = BASE_URL + "?q=" + cityName + "&appid=" + API_KEY + "&lang=kr";
        makeRequest(url, callback);
    }

    // 위도, 경도로 날씨 정보 가져오기
    public void getWeatherByCoordinates(double lat, double lon, WeatherCallback callback) {
        String url = BASE_URL + "?lat=" + lat + "&lon=" + lon + "&appid=" + API_KEY + "&lang=kr";
        makeRequest(url, callback);
    }

    private void makeRequest(String url, WeatherCallback callback) {
        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Weather API request failed", e);
                callback.onError("네트워크 오류: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    Log.e(TAG, "Weather API response not successful: " + response.code());
                    callback.onError("API 오류: " + response.code());
                    return;
                }

                String responseBody = response.body().string();
                Log.d(TAG, "Weather API response: " + responseBody);

                try {
                    WeatherData weatherData = parseWeatherData(responseBody);
                    callback.onSuccess(weatherData);
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing weather data", e);
                    callback.onError("데이터 파싱 오류: " + e.getMessage());
                }
            }
        });
    }

    private WeatherData parseWeatherData(String jsonResponse) {
        JsonObject jsonObject = JsonParser.parseString(jsonResponse).getAsJsonObject();
        
        // 도시 이름
        String cityName = jsonObject.get("name").getAsString();
        
        // 메인 날씨 정보
        JsonObject main = jsonObject.getAsJsonObject("main");
        double temperature = main.get("temp").getAsDouble();
        double feelsLike = main.get("feels_like").getAsDouble();
        int humidity = main.get("humidity").getAsInt();
        
        // 날씨 설명
        JsonObject weather = jsonObject.getAsJsonArray("weather").get(0).getAsJsonObject();
        String description = weather.get("description").getAsString();
        String icon = weather.get("icon").getAsString();
        
        // 바람 정보
        double windSpeed = 0;
        if (jsonObject.has("wind")) {
            JsonObject wind = jsonObject.getAsJsonObject("wind");
            windSpeed = wind.get("speed").getAsDouble();
        }

        return new WeatherData(cityName, temperature, description, icon, humidity, windSpeed, feelsLike);
    }

    // API 키 설정 메서드
    public static String getApiKey() {
        return API_KEY;
    }

    // API 키가 설정되었는지 확인
    public static boolean isApiKeySet() {
        return !API_KEY.equals("YOUR_API_KEY_HERE") && !API_KEY.isEmpty() && !API_KEY.equals("demo_key_for_testing");
    }
}
