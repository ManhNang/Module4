<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
  <head>
    <title>jQuery Slide Effects</title>
    <!-- Nhúng thư viện jQuery qua CDN Google -->
    <script src="https://ajax.googleapis.com/ajax/libs/jquery/3.7.1/jquery.min.js"></script>
    <style>
      body {
        font-family: Arial, sans-serif;
        margin: 40px;
      }
      .btn {
        padding: 10px 20px;
        font-size: 16px;
        cursor: pointer;
        border: none;
        border-radius: 4px;
      }
      .btn-hide {
        background-color: #dc3545;
        color: white;
      }
      .btn-show {
        background-color: #28a745;
        color: white;
      }
      #message {
        margin-top: 20px;
        padding: 20px;
        background-color: #e9ecef;
        border-left: 5px solid #007bff;
        font-size: 18px;
        width: 400px;
      }
    </style>
  </head>
  <body>
    <h2>Thực hành jQuery: Ẩn / Hiện văn bản</h2>
    <button id="hide" class="btn btn-hide">Hide</button>
    <button id="show" class="btn btn-show">Show</button>

    <div id="message">If you click on the "Hide" button, I will disappear!</div>

    <script>
      $(document).ready(function () {
        // Khi toàn bộ trang (DOM) tải xong, đoạn mã mới được thực thi

        // Xử lý sự kiện click nút Hide
        $("#hide").click(function () {
          // Thu hẹp và ẩn phần tử mượt mà trong 500ms
          $("#message").slideUp(500);
        });

        // Xử lý sự kiện click nút Show
        $("#show").click(function () {
          // Trượt xuống và hiển thị lại phần tử trong 500ms
          $("#message").slideDown(500);
        });
      });
    </script>
  </body>
</html>
