package com.example.weather.config;

import com.example.weather.entity.UserEntity;
import com.example.weather.entity.WeatherEntity;
import com.example.weather.repository.UserRepository;
import com.example.weather.repository.WeatherRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final WeatherRepository weatherRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository,
                           WeatherRepository weatherRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.weatherRepository = weatherRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        initUsers();
        initWeatherData();
    }

    private void initUsers() {
        if (userRepository.count() == 0) {
            List<UserEntity> users = List.of(
                    new UserEntity("client1", passwordEncoder.encode("123456"), "USER"),
                    new UserEntity("admin", passwordEncoder.encode("admin123"), "ADMIN"),
                    new UserEntity("user", passwordEncoder.encode("password"), "USER")
            );
            userRepository.saveAll(users);
            log.info("Initialized sample users: client1/123456, admin/admin123, user/password");
        }
    }

    private void initWeatherData() {
        if (weatherRepository.count() == 0) {
            LocalDate today = LocalDate.now();
            List<WeatherEntity> forecasts = List.of(
                    new WeatherEntity(
                            "hanoi",
                            "Hà Nội",
                            24.5,
                            75,
                            12.0,
                            "Cloudy",
                            "Nhiều mây, có lúc có mưa rào nhẹ về chiều",
                            today
                    ),
                    new WeatherEntity(
                            "hochiminh",
                            "TP. Hồ Chí Minh",
                            32.0,
                            65,
                            8.5,
                            "Sunny",
                            "Trời nắng, nhiệt độ cao ban trưa, chiều tối có dông vài nơi",
                            today
                    ),
                    new WeatherEntity(
                            "danang",
                            "Đà Nẵng",
                            28.0,
                            70,
                            15.0,
                            "Partly Cloudy",
                            "Nắng gián đoạn, thời tiết mát mẻ dễ chịu",
                            today
                    ),
                    new WeatherEntity(
                            "haiphong",
                            "Hải Phòng",
                            23.0,
                            80,
                            18.5,
                            "Windy",
                            "Gió đông bắc cấp 3-4, trời se lạnh về đêm",
                            today
                    ),
                    new WeatherEntity(
                            "cantho",
                            "Cần Thơ",
                            31.0,
                            72,
                            9.0,
                            "Sunny",
                            "Nắng ráo, gió nhẹ phù hợp hoạt động ngoài trời",
                            today
                    ),
                    new WeatherEntity(
                            "dalat",
                            "Đà Lạt",
                            17.5,
                            85,
                            6.0,
                            "Cool & Foggy",
                            "Trời se lạnh, sáng sớm sương mù giăng mắc",
                            today
                    )
            );
            weatherRepository.saveAll(forecasts);
            log.info("Initialized sample weather forecast data for locations: hanoi, hochiminh, danang, haiphong, cantho, dalat");
        }
    }
}

