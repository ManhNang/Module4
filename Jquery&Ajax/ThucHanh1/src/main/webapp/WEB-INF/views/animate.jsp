<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
  <head>
    <title>jQuery Animate Effect</title>
    <script src="https://ajax.googleapis.com/ajax/libs/jquery/3.7.1/jquery.min.js"></script>
    <style>
      body {
        font-family: Arial, sans-serif;
        margin: 40px;
      }
      .btn-start {
        padding: 12px 24px;
        font-size: 16px;
        background-color: #17a2b8;
        color: white;
        border: none;
        border-radius: 4px;
        cursor: pointer;
      }
      #box {
        position: relative; /* Bắt buộc để animate vị trí left/top */
        width: 120px;
        height: 120px;
        background-color: #007bff;
        color: white;
        text-align: center;
        line-height: 120px;
        margin-top: 30px;
        font-size: 16px;
        border-radius: 8px;
      }
    </style>
  </head>
  <body>
    <h2>Thực hành jQuery: Hoạt ảnh tùy chỉnh với .animate()</h2>
    <button id="start" class="btn-start">Start Animation</button>

    <div id="box">Box</div>

    <script>
      $(document).ready(function () {
        // Biến ghi nhớ trạng thái toggle
        let toggled = false;

        $("#start").click(function () {
          if (!toggled) {
            // Lần click 1: Di chuyển sang phải, phóng to kích thước và chữ
            $("#box").animate(
              {
                left: "+=200px",
                width: "220px",
                height: "220px",
                fontSize: "36px",
              },
              800,
            );
          } else {
            // Lần click 2: Thu về vị trí và kích thước ban đầu
            $("#box").animate(
              {
                left: "-=200px",
                width: "120px",
                height: "120px",
                fontSize: "16px",
              },
              800,
            );
          }

          // Đảo trạng thái sau mỗi lần kích hoạt
          toggled = !toggled;
        });
      });
    </script>
  </body>
</html>
