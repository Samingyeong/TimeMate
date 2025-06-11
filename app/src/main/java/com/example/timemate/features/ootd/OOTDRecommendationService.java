package com.example.timemate.features.ootd;

import android.content.Context;
import android.util.Log;

import com.example.timemate.data.model.OOTDRecommendation;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;

/**
 * OOTD 추천 서비스
 * 현재 날씨와 계절에 맞는 스타일을 추천
 */
public class OOTDRecommendationService {
    
    private static final String TAG = "OOTDService";
    private Context context;
    private List<OOTDRecommendation> ootdDatabase;
    
    public OOTDRecommendationService(Context context) {
        this.context = context;
        initializeOOTDDatabase();
    }
    
    /**
     * OOTD 데이터베이스 초기화 (실제 패션 트렌드 기반)
     */
    private void initializeOOTDDatabase() {
        ootdDatabase = new ArrayList<>();
        
        // 2024년 봄 트렌드
        ootdDatabase.add(new OOTDRecommendation(
            "플리츠 스커트 + 니트 조합",
            "부드러운 니트와 플리츠 스커트로 완성하는 로맨틱 룩. 봄의 따뜻함을 표현하는 파스텔 톤 추천",
            "https://example.com/spring1.jpg",
            "romantic", "spring", "mild"
        ));
        
        ootdDatabase.add(new OOTDRecommendation(
            "오버사이즈 블레이저 스타일",
            "트렌디한 오버사이즈 블레이저로 완성하는 시크한 룩. 데님과 매치하면 캐주얼하게",
            "https://example.com/spring2.jpg",
            "chic", "spring", "cool"
        ));
        
        // 2024년 여름 트렌드
        ootdDatabase.add(new OOTDRecommendation(
            "린넨 셔츠 + 와이드 팬츠",
            "시원한 린넨 소재로 완성하는 여름 룩. 통풍이 잘 되고 편안한 착용감",
            "https://example.com/summer1.jpg",
            "casual", "summer", "hot"
        ));
        
        ootdDatabase.add(new OOTDRecommendation(
            "플로럴 원피스",
            "여름을 대표하는 플로럴 패턴 원피스. 가볍고 시원한 소재로 더위를 피하세요",
            "https://example.com/summer2.jpg",
            "feminine", "summer", "sunny"
        ));
        
        // 2024년 가을 트렌드
        ootdDatabase.add(new OOTDRecommendation(
            "체크 패턴 코트",
            "가을을 대표하는 체크 패턴 코트. 어스톤 컬러로 계절감을 살린 룩",
            "https://example.com/autumn1.jpg",
            "classic", "autumn", "cool"
        ));
        
        ootdDatabase.add(new OOTDRecommendation(
            "니트 베스트 레이어링",
            "니트 베스트를 활용한 레이어링 룩. 가을의 일교차에 대비한 스타일링",
            "https://example.com/autumn2.jpg",
            "layered", "autumn", "mild"
        ));
        
        // 2024년 겨울 트렌드
        ootdDatabase.add(new OOTDRecommendation(
            "퍼 코트 + 부츠",
            "따뜻한 퍼 코트와 부츠로 완성하는 겨울 룩. 보온성과 스타일을 모두 잡은 코디",
            "https://example.com/winter1.jpg",
            "warm", "winter", "cold"
        ));
        
        ootdDatabase.add(new OOTDRecommendation(
            "터틀넥 + 롱 코트",
            "클래식한 터틀넥과 롱 코트 조합. 심플하면서도 세련된 겨울 스타일",
            "https://example.com/winter2.jpg",
            "minimal", "winter", "cold"
        ));
        
        // 비오는 날 스타일
        ootdDatabase.add(new OOTDRecommendation(
            "트렌치 코트 + 레인부츠",
            "비오는 날을 위한 실용적이면서도 스타일리시한 룩. 방수 기능과 패션성을 모두 고려",
            "https://example.com/rainy1.jpg",
            "practical", "all", "rainy"
        ));
        
        // 더위 대비 스타일
        ootdDatabase.add(new OOTDRecommendation(
            "크롭 탑 + 하이웨스트 팬츠",
            "무더운 날씨를 위한 시원한 크롭 탑 스타일. 하이웨스트 팬츠로 비율 보정까지",
            "https://example.com/hot1.jpg",
            "cool", "summer", "hot"
        ));
        
        Log.d(TAG, "OOTD 데이터베이스 초기화 완료: " + ootdDatabase.size() + "개 스타일");
    }
    
    /**
     * 현재 날씨와 계절에 맞는 OOTD 추천
     */
    public List<OOTDRecommendation> getRecommendations(String weather, double temperature) {
        String currentSeason = getCurrentSeason();
        String weatherCondition = mapWeatherCondition(weather, temperature);
        
        Log.d(TAG, "OOTD 추천 요청 - 계절: " + currentSeason + ", 날씨: " + weatherCondition + ", 온도: " + temperature);
        
        List<OOTDRecommendation> recommendations = new ArrayList<>();
        
        // 날씨와 계절에 맞는 스타일 필터링
        for (OOTDRecommendation ootd : ootdDatabase) {
            if (ootd.isMatchingWeather(weatherCondition, currentSeason) && 
                ootd.isMatchingTemperature(temperature)) {
                recommendations.add(ootd);
            }
        }
        
        // 추천이 없으면 계절에 맞는 것이라도 추천
        if (recommendations.isEmpty()) {
            for (OOTDRecommendation ootd : ootdDatabase) {
                if (ootd.season.equals(currentSeason) || ootd.season.equals("all")) {
                    recommendations.add(ootd);
                }
            }
        }
        
        // 최대 3개까지 랜덤 선택
        if (recommendations.size() > 3) {
            List<OOTDRecommendation> selected = new ArrayList<>();
            Random random = new Random();
            for (int i = 0; i < 3; i++) {
                if (!recommendations.isEmpty()) {
                    int index = random.nextInt(recommendations.size());
                    selected.add(recommendations.remove(index));
                }
            }
            recommendations = selected;
        }
        
        Log.d(TAG, "추천된 OOTD 수: " + recommendations.size());
        return recommendations;
    }
    
    /**
     * 현재 계절 반환
     */
    private String getCurrentSeason() {
        Calendar calendar = Calendar.getInstance();
        int month = calendar.get(Calendar.MONTH) + 1; // 0-based이므로 +1
        
        if (month >= 3 && month <= 5) {
            return "spring";
        } else if (month >= 6 && month <= 8) {
            return "summer";
        } else if (month >= 9 && month <= 11) {
            return "autumn";
        } else {
            return "winter";
        }
    }
    
    /**
     * 날씨 조건을 OOTD 카테고리로 매핑
     */
    private String mapWeatherCondition(String weather, double temperature) {
        if (weather == null) return "mild";
        
        String lowerWeather = weather.toLowerCase();
        
        if (lowerWeather.contains("rain") || lowerWeather.contains("비")) {
            return "rainy";
        } else if (lowerWeather.contains("snow") || lowerWeather.contains("눈")) {
            return "cold";
        } else if (temperature > 28) {
            return "hot";
        } else if (temperature < 10) {
            return "cold";
        } else if (lowerWeather.contains("sun") || lowerWeather.contains("맑")) {
            return "sunny";
        } else if (lowerWeather.contains("cloud") || lowerWeather.contains("흐림")) {
            return "cloudy";
        } else {
            return "mild";
        }
    }
}
