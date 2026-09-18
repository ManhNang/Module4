# Email Validate - Spring MVC & Maven

Dự án thực hành kiểm tra tính hợp lệ của địa chỉ Email sử dụng Spring MVC Framework và Apache Maven (CodeGym - Module 4: Spring Controller).

---

## 📌 Giới thiệu

Ứng dụng cho phép người dùng nhập một địa chỉ email thông qua form giao diện. Hệ thống sẽ sử dụng Regular Expression (Regex) để kiểm tra định dạng email:
- Nếu hợp lệ: Chuyển hướng sang trang kết quả thành công (`success.jsp`) và hiển thị email đã nhập.
- Nếu không hợp lệ: Quay trở lại trang chủ (`home.jsp`) kèm thông báo lỗi `"Email is invalid"`.

---

## 🛠 Công nghệ & Thư viện sử dụng

- **Ngôn ngữ:** Java 17
- **Framework:** Spring Web MVC `6.2.6`
- **Servlet API:** Jakarta Servlet API `6.0.0` (chuẩn Jakarta EE 10)
- **View Template:** JSP (JavaServer Pages)
- **Công cụ build:** Apache Maven
- **Server khuyến nghị:** Apache Tomcat 10.1+ (tương thích Jakarta EE 10 / Servlet 6.0)

---

## 📂 Cấu trúc thư mục dự án

```text
ThucHanh/
├── pom.xml                                    # File cấu hình Maven & dependencies
└── src/
    └── main/
        ├── java/
        │   └── com/codegym/emailvalidatemaven/
        │       └── controller/
        │           └── HomeController.java    # Controller xử lý routing & validate email
        └── webapp/
            └── WEB-INF/
                ├── web.xml                    # Cấu hình DispatcherServlet
                ├── configs/
                │   └── spring-config-mvc.xml  # Cấu hình Spring MVC (InternalResourceViewResolver, component-scan)
                └── views/
                    ├── home.jsp               # Giao diện nhập email
                    └── success.jsp            # Giao diện hiển thị kết quả thành công
```

---

## ⚙️ Cấu hình và Luồng hoạt động

### 1. Regex kiểm tra Email
Biểu thức chính quy được định nghĩa trong `HomeController`:
```java
^[A-Za-z0-9]+[A-Za-z0-9]*@[A-Za-z0-9]+(\\.[A-Za-z0-9]+)+$
```
- Bắt đầu với một hoặc nhiều ký tự chữ cái hoặc chữ số.
- Chứa ký tự `@`.
- Sau `@` là tên miền với ít nhất một dấu chấm `.`.

### 2. Endpoints
| HTTP Method | Đường dẫn | Chức năng | View trả về |
| :--- | :--- | :--- | :--- |
| `GET` | `/` | Hiển thị trang chủ với form nhập email | `home.jsp` |
| `POST` | `/validate` | Tiếp nhận và kiểm tra email gửi lên | `success.jsp` (hợp lệ) / `home.jsp` (lỗi) |

---

## 🚀 Hướng dẫn cài đặt & Chạy ứng dụng

### Yêu cầu tiên quyết
- **JDK:** 17 hoặc mới hơn đã cấu hình biến môi trường `JAVA_HOME`.
- **Maven:** Phiên bản 3.8+ (hoặc dùng Maven tích hợp sẵn trong IDE).
- **Web Server:** Apache Tomcat 10.1 trở lên (lưu ý: Tomcat 9 trở xuống dùng `javax.*` sẽ không tương thích với `jakarta.*`).

### Các bước thực hiện

1. **Clone hoặc mở dự án:**
   Mở thư mục dự án trong IntelliJ IDEA / Eclipse / VS Code.

2. **Build dự án với Maven:**
   Mở terminal tại thư mục gốc và chạy lệnh:
   ```bash
   mvn clean package
   ```
   File WAR sẽ được tạo ra tại thư mục `target/email-validate-maven.war`.

3. **Cấu hình Tomcat Server:**
   - Trong IDE (ví dụ: IntelliJ IDEA):
     - Thêm cấu hình chạy **Tomcat Server** -> **Local**.
     - Chọn Tomcat 10.1+.
     - Trong tab **Deployment**, thêm artifact `email-validate-maven:war exploded` hoặc file `.war`.
     - Đặt Application Context (ví dụ: `/` hoặc `/email-validate-maven`).

4. **Khởi chạy & Trải nghiệm:**
   - Khởi động server Tomcat.
   - Mở trình duyệt và truy cập:
     ```text
     http://localhost:8080/
     ```
     *(hoặc `http://localhost:8080/email-validate-maven/` tùy context root cấu hình)*
   - Nhập email để kiểm tra:
     - Thử email hợp lệ: `example@gmail.com`
     - Thử email không hợp lệ: `example@` hoặc `abc`
