package com.example.weather.service;

import com.example.weather.dto.WeatherResponse;

public interface WeatherService {
    WeatherResponse getWeatherByLocation(String location);
}

