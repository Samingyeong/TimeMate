package com.example.timemate.data.model;

/**
 * OOTD 추천 데이터 모델
 */
public class OOTDRecommendation {
    
    public String title;           // 스타일 제목
    public String description;     // 스타일 설명
    public String imageUrl;        // 이미지 URL
    public String category;        // 카테고리 (casual, formal, street, etc.)
    public String season;          // 계절 (spring, summer, autumn, winter)
    public String weather;         // 날씨 (sunny, rainy, cloudy, cold, hot)
    public String temperature;     // 온도 범위 (예: "15-20°C")
    public String tags;            // 태그 (예: "트렌디,편안함,데일리")
    public String source;          // 출처
    public long createdAt;         // 생성 시간
    
    public OOTDRecommendation() {
        this.createdAt = System.currentTimeMillis();
    }
    
    public OOTDRecommendation(String title, String description, String imageUrl, 
                             String category, String season, String weather) {
        this.title = title;
        this.description = description;
        this.imageUrl = imageUrl;
        this.category = category;
        this.season = season;
        this.weather = weather;
        this.createdAt = System.currentTimeMillis();
    }
    
    /**
     * 현재 날씨와 계절에 맞는지 확인
     */
    public boolean isMatchingWeather(String currentWeather, String currentSeason) {
        if (currentWeather == null || currentSeason == null) return false;
        
        boolean weatherMatch = weather == null || weather.isEmpty() || 
                              weather.toLowerCase().contains(currentWeather.toLowerCase());
        boolean seasonMatch = season == null || season.isEmpty() || 
                             season.toLowerCase().contains(currentSeason.toLowerCase());
        
        return weatherMatch && seasonMatch;
    }
    
    /**
     * 온도 범위에 맞는지 확인
     */
    public boolean isMatchingTemperature(double currentTemp) {
        if (temperature == null || temperature.isEmpty()) return true;
        
        try {
            // "15-20°C" 형태에서 숫자 추출
            String[] parts = temperature.replace("°C", "").split("-");
            if (parts.length == 2) {
                double minTemp = Double.parseDouble(parts[0].trim());
                double maxTemp = Double.parseDouble(parts[1].trim());
                return currentTemp >= minTemp && currentTemp <= maxTemp;
            }
        } catch (Exception e) {
            // 파싱 실패 시 true 반환
        }
        
        return true;
    }
}
