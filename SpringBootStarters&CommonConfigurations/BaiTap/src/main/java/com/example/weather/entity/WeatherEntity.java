package com.example.weather.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "weather_forecasts")
public class WeatherEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String location; // lowercase location key, e.g. "hanoi", "hochiminh"

    @Column(nullable = false, length = 100)
    private String cityName; // Display name e.g. "Hà Nội", "Hồ Chí Minh"

    @Column(nullable = false)
    private Double temperature; // Celsius

    @Column(nullable = false)
    private Integer humidity; // Percentage (%)

    @Column(nullable = false)
    private Double windSpeed; // km/h

    @Column(nullable = false, length = 100)
    private String condition; // e.g. "Sunny", "Rainy", "Cloudy"

    @Column(length = 255)
    private String description;

    @Column(nullable = false)
    private LocalDate forecastDate;

    public WeatherEntity() {
    }

    public WeatherEntity(String location, String cityName, Double temperature, Integer humidity,
                         Double windSpeed, String condition, String description, LocalDate forecastDate) {
        this.location = location != null ? location.toLowerCase().trim() : null;
        this.cityName = cityName;
        this.temperature = temperature;
        this.humidity = humidity;
        this.windSpeed = windSpeed;
        this.condition = condition;
        this.description = description;
        this.forecastDate = forecastDate;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location != null ? location.toLowerCase().trim() : null;
    }

    public String getCityName() {
        return cityName;
    }

    public void setCityName(String cityName) {
        this.cityName = cityName;
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    public Integer getHumidity() {
        return humidity;
    }

    public void setHumidity(Integer humidity) {
        this.humidity = humidity;
    }

    public Double getWindSpeed() {
        return windSpeed;
    }

    public void setWindSpeed(Double windSpeed) {
        this.windSpeed = windSpeed;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getForecastDate() {
        return forecastDate;
    }

    public void setForecastDate(LocalDate forecastDate) {
        this.forecastDate = forecastDate;
    }
}

