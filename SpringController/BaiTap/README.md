# Calculate Salary Maven - Spring MVC 6.2.6 & Tomcat 10.1+

Ứng dụng web tính toán lương nhân viên dựa trên tổng số giờ làm việc trong 3 ca (Sáng, Chiều, Tối) và mức lương theo giờ, được xây dựng bằng **Spring Web MVC 6.2.6** trên nền tảng **Jakarta EE 10** và **Java 17+**, tương thích hoàn toàn với **Apache Tomcat 10.1+**.

---

## 🛠 Công Nghệ Sử Dụng

- **Java Version**: 17+ LTS
- **Servlet Specification**: Jakarta EE 10 (`jakarta.servlet-api:6.0.0`)
- **Spring Framework**: Spring Web MVC `6.2.6`
- **Build Tool**: Apache Maven (`war` packaging)
- **Application Server**: Apache Tomcat 10.1+
- **Frontend / View**: JSP, JSTL / Expression Language (EL), CSS3 Modern UI

---

## 📁 Cấu Trúc Dự Án

```
calculate-salary-maven/
├── pom.xml
├── README.md
└── src/
    └── main/
        ├── java/
        │   └── com/
        │       └── codegym/
        │           └── calculatesalary/
        │               └── controller/
        │                   └── SalaryController.java
        └── webapp/
            └── WEB-INF/
                ├── configs/
                │   └── spring-config-mvc.xml
                ├── views/
                │   ├── index.jsp
                │   └── result.jsp
                └── web.xml
```

---

## ⚙️ Chi Tiết Cấu Hình & Thành Phần

### 1. `pom.xml`
- Khai báo dependency `jakarta.servlet-api:6.0.0` với scope `provided`.
- Khai báo dependency `spring-webmvc:6.2.6`.
- Cấu hình `maven-compiler-plugin` với Java 17.
- Cấu hình `maven-war-plugin` xuất file có tên cố định: `calculate-salary-maven.war`.

### 2. `WEB-INF/web.xml` (Servlet 6.0)
- Cấu hình `DispatcherServlet` (tên servlet: `dispatcher`) ánh xạ tới URL pattern `/`.
- Nạp cấu hình ứng dụng từ `/WEB-INF/configs/spring-config-mvc.xml`.
- Tích hợp `CharacterEncodingFilter` UTF-8 để hỗ trợ tiếng Việt đầy đủ.

### 3. `WEB-INF/configs/spring-config-mvc.xml`
- Bật `<mvc:annotation-driven/>`.
- Bật quét controller qua `<context:component-scan base-package="com.codegym.calculatesalary.controller"/>`.
- Cấu hình `InternalResourceViewResolver` trỏ tới `/WEB-INF/views/` với đuôi file `.jsp`.

### 4. `SalaryController.java`
- `@GetMapping("/")`: Trả về trang nhập liệu `index.jsp`.
- `@PostMapping("/calculate")`:
  - Tiếp nhận mảng số giờ làm từ 3 ca: `shiftHours` (`double[]`).
  - Tiếp nhận mức lương theo giờ: `hourlyRate` (`double`).
  - Tính tổng số giờ làm: $T = \text{Ca Sáng} + \text{Ca Chiều} + \text{Ca Tối}$.
  - Tính tổng tiền lương: $\text{Tổng Lương} = T \times \text{hourlyRate}$.
  - Truyền dữ liệu vào `Model` và điều hướng sang view `result.jsp`.

### 5. Views
- `index.jsp`: Form POST với 3 trường input `shiftHours` (Ca Sáng, Ca Chiều, Ca Tối) và 1 select `hourlyRate`.
- `result.jsp`: Sử dụng Expression Language (EL) để hiển thị chi tiết mức lương, số giờ từng ca, tổng số giờ và tổng tiền lương nhận được, kèm nút điều hướng quay về trang nhập liệu.

---

## 🚀 Hướng Dẫn Build & Chạy Ứng Dụng

### Bước 1: Build file WAR bằng Maven
Tại thư mục gốc của dự án (`D:\Module4\SpringController\BaiTap`), mở terminal và chạy lệnh:
```bash
mvn clean package
```
Sau khi build thành công, file WAR sẽ được tạo tại:
```
target/calculate-salary-maven.war
```

### Bước 2: Triển khai trên Apache Tomcat 10.1+

#### Cách 1: Copy file WAR vào thư mục `webapps`
1. Đảm bảo bạn đã cài đặt **Tomcat 10.1.x** (hỗ trợ Jakarta EE 10).
2. Sao chép file `target/calculate-salary-maven.war` vào thư mục `<TOMCAT_HOME>/webapps/`.
3. Khởi động Tomcat bằng cách chạy `<TOMCAT_HOME>/bin/startup.bat` (Windows) hoặc `startup.sh` (Linux/Mac).
4. Mở trình duyệt và truy cập:
   ```
   http://localhost:8080/calculate-salary-maven/
   ```

#### Cách 2: Chạy trực tiếp qua IntelliJ IDEA / Eclipse
1. Thêm cấu hình **Tomcat Server** -> **Local**.
2. Chọn phiên bản Tomcat 10.1+.
3. Trong tab **Deployment**, thêm artifact `calculate-salary-maven:war` hoặc `calculate-salary-maven:war exploded`.
4. Đặt **Application context** là `/calculate-salary-maven` hoặc `/`.
5. Nhấn **Run** hoặc **Debug**.
