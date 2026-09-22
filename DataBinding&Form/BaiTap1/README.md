# Spring MVC - Email Box Configuration (spring-user-model)

Dự án mẫu Spring MVC chuẩn tương thích với **Tomcat 10.1+ (Jakarta EE 10, Spring 6.2.6, Java 17+)** sử dụng **Gradle** để quản lý và đóng gói, minh họa kỹ thuật **Spring Form Data Binding**.

---

## 📌 Giới thiệu dự án

Ứng dụng web cho phép quản trị viên xem và cập nhật cấu hình của một hòm thư điện tử (Email Box), bao gồm các thông số:
- **Languages (Ngôn ngữ)**: `English`, `Vietnamese`, `Japanese`, `Chinese`.
- **Page Size (Số lượng email hiển thị)**: `5`, `10`, `15`, `25`, `50`, `100` emails/trang.
- **Spams Filter (Bộ lọc thư rác)**: Bật/Tắt lọc thư rác.
- **Signature (Chữ ký email)**: Nội dung chữ ký cá nhân kèm theo mỗi thư gửi đi.

---

## 🛠 Công nghệ sử dụng

| Công nghệ | Phiên bản |
| :--- | :--- |
| **Java** | 17+ (LTS) |
| **Servlet Specification** | Jakarta EE 10 (Servlet API 6.0.0) |
| **Spring Framework** | Spring Web MVC 6.2.6 |
| **JSTL** | Jakarta JSTL API 3.0.0 & GlassFish Impl 3.0.1 |
| **Build Tool** | Gradle 9.2+ (có sẵn Gradle Wrapper) |
| **Web Server / Servlet Container** | Apache Tomcat 10.1+ |

---

## 📂 Cấu trúc dự án

```text
D:\Module4\DataBinding&Form\BaiTap1
├── gradle/wrapper/                 # Gradle Wrapper binaries & properties
├── gradlew                         # Gradle wrapper script (Linux/macOS)
├── gradlew.bat                     # Gradle wrapper script (Windows)
├── build.gradle                    # Cấu hình Gradle dependencies và build WAR
├── settings.gradle                 # Khai báo tên project (spring-user-model)
├── README.md                       # Tài liệu hướng dẫn dự án
└── src
    └── main
        ├── java
        │   └── com
        │       └── codegym
        │           ├── controller
        │           │   └── EmailConfigController.java   # Controller điều hướng & xử lý form
        │           └── model
        │               └── EmailConfig.java             # Model lưu giữ cấu hình hòm thư
        └── webapp
            └── WEB-INF
                ├── applicationContext.xml               # Cấu hình Spring beans gốc
                ├── dispatcher-servlet.xml               # Cấu hình Spring MVC & ViewResolver
                ├── web.xml                              # Khai báo DispatcherServlet (Jakarta EE 6.0)
                └── views
                    ├── edit.jsp                         # Trang form cập nhật cấu hình
                    └── info.jsp                         # Trang hiển thị thông tin cấu hình
```

---

## 🚀 Hướng dẫn cài đặt & Chạy ứng dụng

### 1. Yêu cầu hệ thống
- **JDK 17 trở lên** đã được cài đặt và thiết lập biến môi trường `JAVA_HOME`.
- **Apache Tomcat 10.1+** (hỗ trợ Jakarta EE 10 / Servlet 6.0).

### 2. Đóng gói file WAR bằng Gradle

Mở terminal tại thư mục gốc của dự án (`D:\Module4\DataBinding&Form\BaiTap1`) và chạy lệnh:

* Trên Windows:
  ```powershell
  gradle clean war
  # Hoặc dùng gradlew:
  .\gradlew clean war
  ```

* File WAR kết quả sẽ được tạo tại:
  ```text
  build/libs/spring-email-box.war
  ```

---

### 3. Triển khai lên Tomcat 10.1+

#### Cách 1: Chạy trực tiếp qua IntelliJ IDEA (Khuyến nghị)
1. Mở dự án trong IntelliJ IDEA.
2. Đi tới menu **Run** -> **Edit Configurations...**.
3. Bấm dấu `+` và chọn **Tomcat Server** -> **Local**.
4. Chọn đường dẫn đến thư mục cài đặt Tomcat 10.1+ của bạn.
5. Chuyển sang tab **Deployment**:
   - Bấm `+` -> chọn **Artifact...** -> chọn `spring-user-model:war` (hoặc add file `spring-email-box.war` từ `build/libs/`).
   - Đặt **Application context** (ví dụ: `/` hoặc `/spring-email-box`).
6. Bấm **Apply** và **Run** để khởi chạy server.

#### Cách 2: Deploy thủ công vào Tomcat
1. Sao chép file `build/libs/spring-email-box.war` vào thư mục `webapps/` của Apache Tomcat 10.1+.
2. Khởi động Tomcat bằng lệnh `startup.bat` (Windows) hoặc `startup.sh` (Linux).
3. Truy cập theo đường dẫn:
   ```text
   http://localhost:8080/spring-email-box/
   ```

---

## 🌐 Danh sách URL & Luồng hoạt động

| Phương thức | Đường dẫn (Path) | Chức năng | Giao diện |
| :--- | :--- | :--- | :--- |
| `GET` | `/` hoặc `/settings` | Xem thông tin cấu hình hòm thư hiện tại | `info.jsp` |
| `GET` | `/settings/edit` | Hiển thị form thay đổi cấu hình với Spring Form Binding | `edit.jsp` |
| `POST` | `/settings/update` | Tiếp nhận dữ liệu cấu hình đã sửa, lưu lại và chuyển hướng về trang xem | Chuyển hướng về `/settings` kèm Flash Message |

---

## 📝 Điểm kỹ thuật nổi bật

1. **Jakarta EE 10 Namespace**:
   - Chuyển toàn bộ package từ `javax.servlet` sang `jakarta.servlet`.
   - `web.xml` sử dụng XML Schema chuẩn Jakarta EE 6.0:
     ```xml
     <web-app xmlns="https://jakarta.ee/xml/ns/jakartaee" version="6.0">
     ```
2. **Spring 6.2.6 & Java 17 `-parameters`**:
   - `build.gradle` tích hợp cờ `-parameters` cho JavaCompile để Spring MVC map tên tham số controller chính xác:
     ```groovy
     tasks.withType(JavaCompile).configureEach {
         options.compilerArgs += ['-parameters']
     }
     ```
3. **Spring Form Data Binding**:
   - Sử dụng thẻ thư viện `<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>`.
   - Binding 2 chiều giữa Model `EmailConfig` và các thẻ `<form:select>`, `<form:checkbox>`, `<form:textarea>`.
