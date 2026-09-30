# img-of-the-day (NASA APOD Viewer & Phân trang nhận xét với JPA)

Ứng dụng Spring MVC + Spring Data JPA hiển thị Bức ảnh thiên văn trong ngày của NASA (Astronomy Picture of the Day - APOD), cho phép người dùng đánh giá (1-5 sao), để lại nhận xét, xem danh sách nhận xét có **phân trang (Pagination)**, lọc xem nhận xét hôm nay hoặc tất cả, và thả tim (Like) cho các bình luận. Ứng dụng tương thích hoàn toàn với **Tomcat 10.1+** (sử dụng **Jakarta EE 10**, **Spring 6**, **Spring Data JPA 3.2**, **Hibernate 6**).

---

## 📁 Cấu trúc dự án
```text
D:\Module4\JPA\BaiTap2
├── build.gradle
├── settings.gradle
└── src
    └── main
        ├── java
        │   └── com/codegym/customermanagementjpa
        │       ├── configuration
        │       │   ├── AppConfig.java
        │       │   └── AppInit.java
        │       ├── controller
        │       │   └── FeedbackController.java
        │       ├── model
        │       │   └── Feedback.java
        │       ├── repository
        │       │   └── IFeedbackRepository.java (Spring Data JPA JpaRepository)
        │       └── service
        │           ├── IFeedbackService.java
        │           └── FeedbackService.java
        └── webapp
            └── WEB-INF
                └── views
                    └── index.html
```

---

## ⚙️ Cấu hình cơ sở dữ liệu MySQL
Mặc định trong file [AppConfig.java](file:///D:/Module4/JPA/BaiTap2/src/main/java/com/codegym/customermanagementjpa/configuration/AppConfig.java):
- **Database URL**: `jdbc:mysql://localhost:3306/apod_db?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC&characterEncoding=UTF-8`
- **Username**: `root`
- **Password**: `123456`
- **Hibernate DDL Auto**: `update` (tự động tạo bảng `feedbacks` khi ứng dụng khởi chạy).

*Bạn có thể chỉnh sửa username/password trong `AppConfig.java` để phù hợp với tài khoản MySQL cục bộ của bạn.*

---

## 🔑 Cấu hình NASA API Key
Bạn có 2 cách thuận tiện để cấu hình API Key:
1. **Trực tiếp trên giao diện Web**: Nhập API Key vào ô *"NASA API Key"* rồi bấm **"Tải ảnh"** (hệ thống sẽ lưu lại trong LocalStorage của trình duyệt). Bạn có thể dùng `DEMO_KEY` để kiểm tra nhanh.
2. **Trong mã nguồn file `index.html`**: Mở file [index.html](file:///D:/Module4/JPA/BaiTap2/src/main/webapp/WEB-INF/views/index.html) và điền key vào dòng:
   ```javascript
   let apiKey = 'API_KEY_CUA_BAN';
   ```

---

## 🚀 Hướng dẫn Build và Chạy trên Tomcat 10.1+
1. Mở Terminal tại thư mục `D:\Module4\JPA\BaiTap2`:
   ```bash
   gradle build -x test
   ```
2. File WAR sẽ được tạo tại:
   ```text
   build/libs/img-of-the-day-1.0-SNAPSHOT.war
   ```
3. Deploy file WAR vào **Apache Tomcat 10.1+** (trong IntelliJ IDEA / Smart Tomcat / thư mục `webapps/` của Tomcat).
4. Truy cập trình duyệt tại:
   ```text
   http://localhost:8080/
   ```
   *(hoặc `http://localhost:8080/img-of-the-day-1.0-SNAPSHOT/` tùy cấu hình context path).*
