# Weather Forecast Web Service (Spring Boot & Spring Security)

Dịch vụ Web Service dự báo thời tiết được xây dựng theo chuẩn Clean Architecture với **Spring Boot 3**, **Spring Security 6**, và **Spring Data JPA**.

## 1. Yêu cầu & Thiết kế hệ thống

- **Endpoint**: `/weather/{username}/{password}/{location}`
  - `{username}`: Tên người dùng cần xác thực
  - `{password}`: Mật khẩu xác thực của client
  - `{location}`: Tên địa phương cần xem dự báo thời tiết (ví dụ: `hanoi`, `hochiminh`, `danang`, `dalat`,...)
- **Bảo mật**: Sử dụng **Spring Security** với custom filter `UrlPathAuthenticationFilter` trích xuất thông tin xác thực từ đường dẫn URI, sau đó ủy thác việc xác thực qua `AuthenticationManager` và `DaoAuthenticationProvider` với mật khẩu mã hóa `BCrypt`.
- **Database**: H2 In-Memory Database (lưu trữ bảng người dùng `users` và bảng dự báo thời tiết `weather_forecasts`).
- **Xử lý lỗi**:
  - Địa phương không có trong Database: Trả về HTTP Status **400 Bad Request**.
  - Sai tài khoản hoặc mật khẩu: Trả về HTTP Status **401 Unauthorized**.

## 2. Cấu trúc thư mục

```text
src/main/java/com/example/weather/
├── WeatherApplication.java
├── config/
│   └── DataInitializer.java               # Tự động nạp dữ liệu mẫu người dùng và thời tiết
├── controller/
│   └── WeatherController.java             # REST Controller phục vụ endpoint /weather/...
├── dto/
│   ├── ErrorResponse.java                 # Định dạng response lỗi chuẩn RESTful
│   └── WeatherResponse.java               # Dữ liệu dự báo thời tiết trả về client
├── entity/
│   ├── UserEntity.java                    # Bảng người dùng (username, password BCrypt, role)
│   └── WeatherEntity.java                 # Bảng thông tin dự báo thời tiết
├── exception/
│   ├── GlobalExceptionHandler.java        # @RestControllerAdvice xử lý Exception tập trung
│   └── LocationNotFoundException.java     # Exception khi không tìm thấy địa phương (HTTP 400)
├── repository/
│   ├── UserRepository.java
│   └── WeatherRepository.java
├── security/
│   ├── CustomAuthenticationEntryPoint.java# Định dạng phản hồi 401 Unauthorized dạng JSON
│   ├── CustomUserDetails.java
│   ├── CustomUserDetailsService.java     # Load tài khoản từ Database
│   ├── SecurityConfig.java                # Cấu hình Spring Security Chain
│   └── UrlPathAuthenticationFilter.java   # Filter bóc tách username/password từ URI
└── service/
    ├── WeatherService.java
    └── impl/
        └── WeatherServiceImpl.java
```

## 3. Dữ liệu mẫu (Data Seeding)

### Người dùng
- Username: `client1` | Password: `123456`
- Username: `admin`   | Password: `admin123`
- Username: `user`    | Password: `password`

### Địa phương có sẵn
- `hanoi` (Hà Nội)
- `hochiminh` (TP. Hồ Chí Minh)
- `danang` (Đà Nẵng)
- `haiphong` (Hải Phòng)
- `cantho` (Cần Thơ)
- `dalat` (Đà Lạt)

## 4. Hướng dẫn chạy và kiểm thử

### Chạy ứng dụng
```bash
mvn spring-boot:run
```

### Chạy unit/integration tests
```bash
mvn clean test
```

### Ví dụ gọi API bằng cURL

1. **Thành công (200 OK):**
```bash
curl -X GET http://localhost:8080/weather/client1/123456/hanoi
```
Response:
```json
{
  "location": "hanoi",
  "cityName": "Hà Nội",
  "temperature": 24.5,
  "humidity": 75,
  "windSpeed": 12.0,
  "condition": "Cloudy",
  "description": "Nhiều mây, có lúc có mưa rào nhẹ về chiều",
  "forecastDate": "2026-11-12"
}
```

2. **Địa phương không tồn tại trong database (400 Bad Request):**
```bash
curl -X GET http://localhost:8080/weather/client1/123456/london
```
Response:
```json
{
  "timestamp": "2026-11-12T08:46:22.353",
  "status": 400,
  "error": "Bad Request",
  "message": "Location 'london' was not found in the weather database.",
  "path": "/weather/client1/123456/london"
}
```

3. **Sai mật khẩu (401 Unauthorized):**
```bash
curl -X GET http://localhost:8080/weather/client1/wrongpass/hanoi
```
Response:
```json
{
  "timestamp": "2026-11-12T08:46:22.284",
  "status": 401,
  "error": "Unauthorized",
  "message": "Authentication failed: Bad credentials",
  "path": "/weather/client1/wrongpass/hanoi"
}
```

4. **Sai tên người dùng (401 Unauthorized):**
```bash
curl -X GET http://localhost:8080/weather/unknown/123456/hanoi
```
Response:
```json
{
  "timestamp": "2026-11-12T08:46:22.424",
  "status": 401,
  "error": "Unauthorized",
  "message": "Authentication failed: Bad credentials",
  "path": "/weather/unknown/123456/hanoi"
}
```

