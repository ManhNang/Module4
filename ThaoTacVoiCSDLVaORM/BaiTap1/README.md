# Ứng Dụng Nghe Nhạc Cá Nhân - Music Streaming App

Dự án chuẩn Gradle Webapp cho **Spring MVC 6.2.6** + **Hibernate 6.x** tương thích với **Tomcat 10.1+** (Jakarta EE 10) & **Java 17**.

---

## 1. Cấu Trúc Dự Án
```text
D:\Module4\ThaoTacVoiCSDLVaORM\BaiTap1
├── build.gradle
├── settings.gradle
├── database.sql
├── src
│   ├── main
│   │   ├── java
│   │   │   └── com
│   │   │       └── codegym
│   │   │           └── musicstreaming
│   │   │               ├── configuration
│   │   │               │   ├── AppConfiguration.java
│   │   │               │   └── AppInit.java
│   │   │               ├── controller
│   │   │               │   └── SongController.java
│   │   │               ├── model
│   │   │               │   ├── Song.java
│   │   │               │   └── SongForm.java
│   │   │               └── service
│   │   │                   ├── ISongService.java
│   │   │                   └── SongServiceImpl.java
│   │   ├── resources
│   │   │   └── hibernate.conf.xml
│   │   └── webapp
│   │       ├── WEB-INF
│   │       │   └── views
│   │       │       ├── list.html
│   │       │       ├── create.html
│   │       │       ├── edit.html
│   │       │       └── play.html
│   │       └── uploads/
```

---

## 2. Hướng Dẫn Chuẩn Bị Cơ Sở Dữ Liệu MySQL

1. Mở MySQL Workbench hoặc Terminal và chạy tệp [`database.sql`](file:///D:/Module4/ThaoTacVoiCSDLVaORM/BaiTap1/database.sql):
   ```sql
   CREATE DATABASE IF NOT EXISTS `ungdungnghenhac` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```
2. Kiểm tra thông tin đăng nhập trong [`hibernate.conf.xml`](file:///D:/Module4/ThaoTacVoiCSDLVaORM/BaiTap1/src/main/resources/hibernate.conf.xml):
   ```xml
   <property name="hibernate.connection.username">root</property>
   <property name="hibernate.connection.password">123456</property>
   ```
   *(Thay đổi password phù hợp với cấu hình MySQL trên máy của bạn nếu cần).*

---

## 3. Lệnh Build và Triển Khai

- Biên dịch và đóng gói thành file WAR:
  ```bash
  gradle war
  ```
- File kết quả xuất ra:
  ```text
  build/libs/music-streaming.war
  ```

### Triển khai lên Tomcat 10.1+:
1. **Cách 1 (IntelliJ IDEA):**
   - Tạo Run/Debug Configuration -> **Tomcat Server** -> **Local**.
   - Chọn thư mục cài đặt Tomcat 10.1+.
   - Tab **Deployment**: Thêm Artifact `music-streaming-app:war` hoặc `music-streaming-app:war exploded`.
   - Application context: `/` hoặc `/music-streaming`.
2. **Cách 2 (Copy file WAR trực tiếp):**
   - Sao chép `build/libs/music-streaming.war` vào thư mục `webapps/` của Apache Tomcat 10.1+.
   - Khởi động Tomcat qua `bin/startup.bat`.
   - Truy cập: `http://localhost:8080/music-streaming` hoặc `http://localhost:8080/`.

---

## 4. Các Chức Năng Đã Triển Khai
- [x] **Hiển thị danh sách bài hát** (`/songs`): Giao diện hiện đại phong cách Spotify, hỗ trợ tìm kiếm bài hát theo tên hoặc nghệ sĩ.
- [x] **Thêm bài hát mới kèm tải file nhạc** (`/songs/create`): Upload file audio (.mp3, .wav, .ogg, .m4a), lưu trữ vào thư mục upload.
- [x] **Phát nhạc trực tiếp**: Thanh phát nhạc HTML5 Audio gắn cố định phía dưới màn hình và trang phát chi tiết (`/songs/play/{id}`) với hiệu ứng đĩa than quay sinh động.
- [x] **Cập nhật bài hát** (`/songs/edit/{id}`): Sửa thông tin tên, nghệ sĩ, thể loại và tùy chọn thay thế file âm thanh mới.
- [x] **Xoá bài hát** (`/songs/delete/{id}`): Xoá dữ liệu trong cơ sở dữ liệu và dọn dẹp file vật lý tương ứng.
