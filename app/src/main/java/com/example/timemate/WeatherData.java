package com.example.timemate;

public class WeatherData {
    private String cityName;
    private double temperature;
    private String description;
    private String icon;
    private int humidity;
    private double windSpeed;
    private double feelsLike;

    public WeatherData() {
    }

    public WeatherData(String cityName, double temperature, String description, String icon, 
                      int humidity, double windSpeed, double feelsLike) {
        this.cityName = cityName;
        this.temperature = temperature;
        this.description = description;
        this.icon = icon;
        this.humidity = humidity;
        this.windSpeed = windSpeed;
        this.feelsLike = feelsLike;
    }

    // Getters and Setters
    public String getCityName() {
        return cityName;
    }

    public void setCityName(String cityName) {
        this.cityName = cityName;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public int getHumidity() {
        return humidity;
    }

    public void setHumidity(int humidity) {
        this.humidity = humidity;
    }

    public double getWindSpeed() {
        return windSpeed;
    }

    public void setWindSpeed(double windSpeed) {
        this.windSpeed = windSpeed;
    }

    public double getFeelsLike() {
        return feelsLike;
    }

    public void setFeelsLike(double feelsLike) {
        this.feelsLike = feelsLike;
    }

    // 온도를 섭씨로 변환 (켈빈 -> 섭씨)
    public int getTemperatureCelsius() {
        return (int) Math.round(temperature - 273.15);
    }

    public int getFeelsLikeCelsius() {
        return (int) Math.round(feelsLike - 273.15);
    }

    // 날씨 설명을 한국어로 변환
    public String getDescriptionKorean() {
        switch (description.toLowerCase()) {
            case "clear sky":
                return "맑음";
            case "few clouds":
                return "구름 조금";
            case "scattered clouds":
                return "구름 많음";
            case "broken clouds":
                return "흐림";
            case "overcast clouds":
                return "흐림";
            case "shower rain":
                return "소나기";
            case "rain":
                return "비";
            case "light rain":
                return "가벼운 비";
            case "moderate rain":
                return "보통 비";
            case "heavy intensity rain":
                return "강한 비";
            case "thunderstorm":
                return "천둥번개";
            case "snow":
                return "눈";
            case "mist":
                return "안개";
            case "fog":
                return "짙은 안개";
            default:
                return description;
        }
    }
}
