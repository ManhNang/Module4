<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Chuyển Đổi Tiền Tệ USD sang VNĐ - Spring MVC</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700&display=swap" rel="stylesheet">
    <style>
        :root {
            --primary: #4f46e5;
            --primary-hover: #4338ca;
            --primary-light: #eef2ff;
            --success: #10b981;
            --text-main: #0f172a;
            --text-muted: #64748b;
            --bg-page: #f8fafc;
            --card-bg: #ffffff;
            --border-color: #e2e8f0;
            --shadow: 0 10px 25px -5px rgba(15, 23, 42, 0.08), 0 8px 10px -6px rgba(15, 23, 42, 0.05);
        }

        * {
            box-sizing: border-box;
            margin: 0;
            padding: 0;
        }

        body {
            font-family: 'Plus Jakarta Sans', -apple-system, BlinkMacSystemFont, sans-serif;
            background: linear-gradient(135deg, #f0f4ff 0%, #f8fafc 50%, #eef2ff 100%);
            min-height: 100vh;
            display: flex;
            align-items: center;
            justify-content: center;
            padding: 24px;
            color: var(--text-main);
        }

        .container {
            width: 100%;
            max-width: 520px;
            background: var(--card-bg);
            border-radius: 20px;
            padding: 36px 32px;
            box-shadow: var(--shadow);
            border: 1px solid rgba(226, 232, 240, 0.8);
            backdrop-filter: blur(8px);
        }

        .header {
            text-align: center;
            margin-bottom: 28px;
        }

        .badge {
            display: inline-flex;
            align-items: center;
            gap: 6px;
            background-color: var(--primary-light);
            color: var(--primary);
            font-size: 0.825rem;
            font-weight: 600;
            padding: 6px 14px;
            border-radius: 9999px;
            margin-bottom: 12px;
            letter-spacing: 0.02em;
        }

        .header h1 {
            font-size: 1.65rem;
            font-weight: 700;
            color: var(--text-main);
            margin-bottom: 6px;
            letter-spacing: -0.02em;
        }

        .header p {
            font-size: 0.925rem;
            color: var(--text-muted);
        }

        .form-group {
            margin-bottom: 20px;
        }

        .form-label {
            display: block;
            font-size: 0.875rem;
            font-weight: 600;
            margin-bottom: 8px;
            color: #334155;
        }

        .input-wrapper {
            position: relative;
            display: flex;
            align-items: center;
        }

        .input-wrapper input {
            width: 100%;
            padding: 12px 64px 12px 16px;
            border: 1.5px solid var(--border-color);
            border-radius: 12px;
            font-size: 1rem;
            font-family: inherit;
            color: var(--text-main);
            outline: none;
            transition: all 0.2s ease-in-out;
            background-color: #fdfdfd;
        }

        .input-wrapper input:focus {
            border-color: var(--primary);
            box-shadow: 0 0 0 4px rgba(79, 70, 229, 0.12);
            background-color: #ffffff;
        }

        .currency-suffix {
            position: absolute;
            right: 16px;
            font-size: 0.85rem;
            font-weight: 600;
            color: var(--text-muted);
            pointer-events: none;
            user-select: none;
        }

        .btn-submit {
            width: 100%;
            padding: 14px;
            margin-top: 8px;
            background: linear-gradient(135deg, var(--primary) 0%, #6366f1 100%);
            color: #ffffff;
            font-size: 1rem;
            font-weight: 600;
            font-family: inherit;
            border: none;
            border-radius: 12px;
            cursor: pointer;
            transition: all 0.2s ease-in-out;
            box-shadow: 0 4px 12px rgba(79, 70, 229, 0.3);
        }

        .btn-submit:hover {
            background: linear-gradient(135deg, var(--primary-hover) 0%, #4f46e5 100%);
            box-shadow: 0 6px 16px rgba(79, 70, 229, 0.4);
            transform: translateY(-1px);
        }

        .btn-submit:active {
            transform: translateY(0);
        }

        .error-card {
            background-color: #fef2f2;
            border: 1px solid #fecaca;
            color: #b91c1c;
            padding: 12px 16px;
            border-radius: 12px;
            font-size: 0.875rem;
            margin-bottom: 20px;
        }

        .result-card {
            margin-top: 28px;
            padding: 22px;
            background: linear-gradient(135deg, #f8faff 0%, #f0fdf4 100%);
            border: 1.5px solid #dcfce7;
            border-radius: 16px;
            animation: fadeIn 0.3s ease-out;
        }

        @keyframes fadeIn {
            from { opacity: 0; transform: translateY(8px); }
            to { opacity: 1; transform: translateY(0); }
        }

        .result-title {
            font-size: 0.8rem;
            font-weight: 700;
            text-transform: uppercase;
            letter-spacing: 0.05em;
            color: #166534;
            margin-bottom: 8px;
        }

        .result-main {
            display: flex;
            align-items: baseline;
            gap: 8px;
            font-size: 1.85rem;
            font-weight: 700;
            color: #15803d;
            letter-spacing: -0.02em;
        }

        .result-unit {
            font-size: 1.1rem;
            font-weight: 600;
            color: #16a34a;
        }

        .result-details {
            margin-top: 12px;
            padding-top: 12px;
            border-top: 1px dashed #bbf7d0;
            font-size: 0.875rem;
            color: #4b5563;
            line-height: 1.5;
        }

        .footer {
            text-align: center;
            margin-top: 24px;
            font-size: 0.8rem;
            color: #94a3b8;
        }
    </style>
</head>
<body>

<div class="container">
    <div class="header">
        <span class="badge">Spring MVC 6.2.6 &bull; Tomcat 10.1+</span>
        <h1>Chuyển Đổi Tiền Tệ</h1>
        <p>Quy đổi giá trị từ Dollar Mỹ (USD) sang Việt Nam Đồng (VNĐ)</p>
    </div>

    <c:if test="${not empty errorMessage}">
        <div class="error-card">
            <strong>Lỗi:</strong> ${errorMessage}
        </div>
    </c:if>

    <form action="${pageContext.request.contextPath}/convert" method="post">
        <div class="form-group">
            <label class="form-label" for="rateInput">Tỉ giá quy đổi (VNĐ / 1 USD)</label>
            <div class="input-wrapper">
                <input type="number" step="any" min="0" id="rateInput" name="rate"
                       value="${rate != null ? rate : 25450}" required placeholder="Nhập tỉ giá...">
                <span class="currency-suffix">VNĐ</span>
            </div>
        </div>

        <div class="form-group">
            <label class="form-label" for="usdInput">Số lượng USD cần đổi</label>
            <div class="input-wrapper">
                <input type="number" step="any" min="0" id="usdInput" name="usd"
                       value="${usd != null ? usd : 1}" required placeholder="Nhập số USD...">
                <span class="currency-suffix">USD</span>
            </div>
        </div>

        <button type="submit" class="btn-submit">Chuyển Đổi Ngay</button>
    </form>

    <c:if test="${resultCalculated}">
        <div class="result-card">
            <div class="result-title">Kết quả quy đổi</div>
            <div class="result-main">
                <span>${vndFormatted}</span>
                <span class="result-unit">VNĐ</span>
            </div>
            <div class="result-details">
                ${usdFormatted} USD &times; ${rateFormatted} VNĐ/USD = <strong>${vndFormatted} VNĐ</strong>
            </div>
        </div>
    </c:if>

    <div class="footer">
        CodeGym &bull; Module 4 &bull; Tổng Quan Spring MVC
    </div>
</div>

</body>
</html>
