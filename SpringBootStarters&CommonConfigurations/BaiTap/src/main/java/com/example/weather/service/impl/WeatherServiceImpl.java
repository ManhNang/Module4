package com.example.weather.service.impl;

import com.example.weather.dto.WeatherResponse;
import com.example.weather.entity.WeatherEntity;
import com.example.weather.exception.LocationNotFoundException;
import com.example.weather.repository.WeatherRepository;
import com.example.weather.service.WeatherService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class WeatherServiceImpl implements WeatherService {

    private final WeatherRepository weatherRepository;

    public WeatherServiceImpl(WeatherRepository weatherRepository) {
        this.weatherRepository = weatherRepository;
    }

    @Override
    public WeatherResponse getWeatherByLocation(String location) {
        if (location == null || location.trim().isEmpty()) {
            throw new LocationNotFoundException("Location must not be empty");
        }

        String searchLocation = location.trim().toLowerCase();

        WeatherEntity entity = weatherRepository.findByLocationIgnoreCase(searchLocation)
                .orElseThrow(() -> new LocationNotFoundException("Location '" + location + "' was not found in the weather database."));

        return new WeatherResponse(
                entity.getLocation(),
                entity.getCityName(),
                entity.getTemperature(),
                entity.getHumidity(),
                entity.getWindSpeed(),
                entity.getCondition(),
                entity.getDescription(),
                entity.getForecastDate()
        );
    }
}

