# 🏠 홈 화면 (Home) 기능 코드

## 📁 파일 구조
```
app/src/main/java/com/example/timemate/ui/home/
├── HomeActivity.java                    # 메인 홈 화면
└── WeatherService.java                  # 날씨 정보 서비스

app/src/main/res/layout/
└── activity_home.xml                    # 홈 화면 레이아웃
```

## 🎯 주요 기능
- ☀️ 날씨 정보 표시 (OpenWeather API)
- 📅 오늘/내일 일정 미리보기
- 🔔 알림 요약 표시
- 🚀 빠른 일정 추가 버튼

## 📱 HomeActivity.java
```java
package com.example.timemate.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.timemate.R;
import com.example.timemate.ui.schedule.ScheduleAddActivity;

public class HomeActivity extends AppCompatActivity {
    
    // UI 컴포넌트
    private TextView textWeather;
    private TextView textTodaySchedule;
    private TextView textTomorrowSchedule;
    private Button btnQuickAdd;
    
    // 서비스
    private WeatherService weatherService;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        
        initViews();
        setupClickListeners();
        loadWeatherInfo();
        loadSchedulePreview();
    }
    
    private void initViews() {
        textWeather = findViewById(R.id.textWeather);
        textTodaySchedule = findViewById(R.id.textTodaySchedule);
        textTomorrowSchedule = findViewById(R.id.textTomorrowSchedule);
        btnQuickAdd = findViewById(R.id.btnQuickAdd);
    }
    
    private void setupClickListeners() {
        btnQuickAdd.setOnClickListener(v -> {
            Intent intent = new Intent(this, ScheduleAddActivity.class);
            startActivity(intent);
        });
    }
    
    private void loadWeatherInfo() {
        weatherService = new WeatherService();
        weatherService.getCurrentWeather("Seoul", new WeatherService.WeatherCallback() {
            @Override
            public void onSuccess(WeatherService.WeatherInfo weather) {
                runOnUiThread(() -> {
                    textWeather.setText(weather.getDisplayText());
                });
            }
            
            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    textWeather.setText("날씨 정보를 불러올 수 없습니다");
                });
            }
        });
    }
    
    private void loadSchedulePreview() {
        // TODO: 오늘/내일 일정 미리보기 로드
    }
}
```

## ☀️ WeatherService.java
```java
package com.example.timemate.ui.home;

import android.util.Log;
import com.example.timemate.config.ApiConfig;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
        
        public String getDisplayText() {
            return String.format("%s %.1f°C %s", city, temperature, description);
        }
    }
    
    public WeatherService() {
        executor = Executors.newSingleThreadExecutor();
    }
    
    public void getCurrentWeather(String city, WeatherCallback callback) {
        executor.execute(() -> {
            try {
                // OpenWeather API 호출 로직
                String url = BASE_URL + "weather?q=" + city + "&appid=" + API_KEY + "&units=metric&lang=kr";
                
                // HTTP 요청 및 응답 처리
                // TODO: 실제 API 호출 구현
                
                WeatherInfo weather = new WeatherInfo();
                weather.city = city;
                weather.temperature = 22.5;
                weather.description = "맑음";
                
                callback.onSuccess(weather);
                
            } catch (Exception e) {
                Log.e(TAG, "Weather API error", e);
                callback.onError(e.getMessage());
            }
        });
    }
    
    public void shutdown() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
}
```

## 🎨 activity_home.xml
```xml
<?xml version="1.0" encoding="utf-8"?>
<ScrollView xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/sky_blue_light">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="16dp">

        <!-- 날씨 정보 카드 -->
        <androidx.cardview.widget.CardView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginBottom="16dp"
            app:cardCornerRadius="12dp"
            app:cardElevation="4dp">

            <TextView
                android:id="@+id/textWeather"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:padding="16dp"
                android:text="날씨 정보 로딩 중..."
                android:textSize="16sp"
                android:textColor="@color/text_primary" />

        </androidx.cardview.widget.CardView>

        <!-- 오늘 일정 카드 -->
        <androidx.cardview.widget.CardView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginBottom="16dp"
            app:cardCornerRadius="12dp"
            app:cardElevation="4dp">

            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="vertical"
                android:padding="16dp">

                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="오늘의 일정"
                    android:textSize="18sp"
                    android:textStyle="bold"
                    android:textColor="@color/text_primary" />

                <TextView
                    android:id="@+id/textTodaySchedule"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="8dp"
                    android:text="일정이 없습니다"
                    android:textSize="14sp"
                    android:textColor="@color/text_secondary" />

            </LinearLayout>

        </androidx.cardview.widget.CardView>

        <!-- 내일 일정 카드 -->
        <androidx.cardview.widget.CardView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginBottom="16dp"
            app:cardCornerRadius="12dp"
            app:cardElevation="4dp">

            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="vertical"
                android:padding="16dp">

                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="내일의 일정"
                    android:textSize="18sp"
                    android:textStyle="bold"
                    android:textColor="@color/text_primary" />

                <TextView
                    android:id="@+id/textTomorrowSchedule"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="8dp"
                    android:text="일정이 없습니다"
                    android:textSize="14sp"
                    android:textColor="@color/text_secondary" />

            </LinearLayout>

        </androidx.cardview.widget.CardView>

        <!-- 빠른 일정 추가 버튼 -->
        <Button
            android:id="@+id/btnQuickAdd"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="+ 새 일정 추가"
            android:textSize="16sp"
            android:textColor="@color/white"
            android:background="@drawable/button_primary"
            android:padding="16dp" />

    </LinearLayout>

</ScrollView>
```

## 🔗 연관 파일
- `ApiConfig.java` - OpenWeather API 키 관리
- `ScheduleAddActivity.java` - 빠른 일정 추가 연결
- `colors.xml` - 색상 리소스
- `strings.xml` - 문자열 리소스
