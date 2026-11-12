package com.example.weather.controller;

import com.example.weather.dto.WeatherResponse;
import com.example.weather.service.WeatherService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/weather")
public class WeatherController {

    private final WeatherService weatherService;

    public WeatherController(WeatherService weatherService) {
        this.weatherService = weatherService;
    }

    /**
     * Endpoint: /weather/$username/$password/$location
     *
     * @param username Authenticated client username
     * @param password Client authentication password
     * @param location Requested location name (e.g. hanoi, hochiminh, danang)
     * @return Weather forecast details or 400 Bad Request if location not found.
     */
    @GetMapping("/{username}/{password}/{location}")
    public ResponseEntity<WeatherResponse> getWeatherForecast(
            @PathVariable("username") String username,
            @PathVariable("password") String password,
            @PathVariable("location") String location) {

        WeatherResponse response = weatherService.getWeatherByLocation(location);
        return ResponseEntity.ok(response);
    }
}

