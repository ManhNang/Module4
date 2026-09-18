<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Email Validation</title>
    <style>
        * {
            box-sizing: border-box;
            margin: 0;
            padding: 0;
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
        }
        body {
            background: linear-gradient(135deg, #f0f4f8 0%, #d9e2ec 100%);
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
            padding: 35px 30px;
            width: 100%;
            max-width: 440px;
        }
        h2 {
            color: #102a43;
            margin-bottom: 24px;
            text-align: center;
            font-size: 24px;
            font-weight: 600;
        }
        .form-group {
            margin-bottom: 20px;
        }
        label {
            display: block;
            margin-bottom: 8px;
            color: #334e68;
            font-size: 14px;
            font-weight: 500;
        }
        input[type="text"] {
            width: 100%;
            padding: 12px 14px;
            border: 1.5px solid #bcccdc;
            border-radius: 8px;
            font-size: 15px;
            color: #102a43;
            outline: none;
            transition: border-color 0.2s, box-shadow 0.2s;
        }
        input[type="text"]:focus {
            border-color: #3b82f6;
            box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.15);
        }
        .btn-submit {
            width: 100%;
            background-color: #2563eb;
            color: #ffffff;
            padding: 12px;
            border: none;
            border-radius: 8px;
            font-size: 16px;
            font-weight: 600;
            cursor: pointer;
            transition: background-color 0.2s, transform 0.1s;
        }
        .btn-submit:hover {
            background-color: #1d4ed8;
        }
        .btn-submit:active {
            transform: scale(0.99);
        }
        .error-message {
            margin-top: 18px;
            padding: 12px 14px;
            border-radius: 8px;
            background-color: #fee2e2;
            border: 1px solid #fca5a5;
            color: #b91c1c;
            font-size: 14px;
            text-align: center;
            font-weight: 500;
        }
    </style>
</head>
<body>

<div class="card">
    <h2>Kiểm tra địa chỉ Email</h2>
    <form action="${pageContext.request.contextPath}/validate" method="post">
        <div class="form-group">
            <label for="email">Nhập Email:</label>
            <input type="text" id="email" name="email" placeholder="ví dụ: user@codegym.vn" required />
        </div>
        <button type="submit" class="btn-submit">Kiểm tra (Validate)</button>
    </form>

    <%-- Hiển thị thông báo lỗi nếu có --%>
    <%
        String message = (String) request.getAttribute("message");
        if (message != null && !message.isEmpty()) {
    %>
        <div class="error-message">
            <%= message %>
        </div>
    <%
        }
    %>
</div>

</body>
</html>
