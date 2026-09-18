<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Email Hợp Lệ</title>
    <style>
        * {
            box-sizing: border-box;
            margin: 0;
            padding: 0;
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
        }
        body {
            background: linear-gradient(135deg, #f0fdf4 0%, #dcfce7 100%);
            min-height: 100vh;
            display: flex;
            justify-content: center;
            align-items: center;
            padding: 20px;
        }
        .card {
            background-color: #ffffff;
            border-radius: 12px;
            box-shadow: 0 10px 25px rgba(0, 0, 0, 0.08);
            padding: 40px 30px;
            width: 100%;
            max-width: 440px;
            text-align: center;
        }
        .icon-success {
            width: 64px;
            height: 64px;
            background-color: #dcfce7;
            color: #16a34a;
            border-radius: 50%;
            display: flex;
            align-items: center;
            justify-content: center;
            margin: 0 auto 20px auto;
            font-size: 32px;
            font-weight: bold;
        }
        h2 {
            color: #166534;
            margin-bottom: 12px;
            font-size: 24px;
            font-weight: 600;
        }
        p {
            color: #4b5563;
            margin-bottom: 20px;
            font-size: 15px;
        }
        .email-display {
            display: inline-block;
            background-color: #f3f4f6;
            color: #1f2937;
            padding: 8px 16px;
            border-radius: 6px;
            font-weight: 600;
            word-break: break-all;
            margin-bottom: 24px;
        }
        .btn-back {
            display: inline-block;
            background-color: #16a34a;
            color: #ffffff;
            padding: 12px 24px;
            border-radius: 8px;
            text-decoration: none;
            font-size: 15px;
            font-weight: 600;
            transition: background-color 0.2s;
        }
        .btn-back:hover {
            background-color: #15803d;
        }
    </style>
</head>
<body>

<div class="card">
    <div class="icon-success">✓</div>
    <h2>Kiểm tra thành công!</h2>
    <p>Địa chỉ email sau đây hoàn toàn hợp lệ:</p>
    <div class="email-display">
        <%= request.getAttribute("email") != null ? request.getAttribute("email") : "" %>
    </div>
    <div>
        <a href="${pageContext.request.contextPath}/" class="btn-back">Quay lại trang chủ</a>
    </div>
</div>

</body>
</html>
