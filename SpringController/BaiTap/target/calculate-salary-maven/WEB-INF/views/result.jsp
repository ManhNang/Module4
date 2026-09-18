<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Kết Quả Tính Lương</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&display=swap" rel="stylesheet">
    <style>
        :root {
            --primary: #4f46e5;
            --primary-hover: #4338ca;
            --primary-light: #eef2ff;
            --success: #10b981;
            --success-light: #ecfdf5;
            --bg-gradient: linear-gradient(135deg, #f0f4ff 0%, #e0e7ff 50%, #f8fafc 100%);
            --card-bg: rgba(255, 255, 255, 0.94);
            --text-main: #1e293b;
            --text-muted: #64748b;
            --border-color: #e2e8f0;
            --shadow-lg: 0 20px 25px -5px rgba(0, 0, 0, 0.08), 0 8px 10px -6px rgba(0, 0, 0, 0.04);
            --radius-md: 12px;
            --radius-lg: 20px;
        }

        * {
            box-sizing: border-box;
            margin: 0;
            padding: 0;
        }

        body {
            font-family: 'Plus Jakarta Sans', sans-serif;
            background: var(--bg-gradient);
            color: var(--text-main);
            min-height: 100vh;
            display: flex;
            align-items: center;
            justify-content: center;
            padding: 24px;
        }

        .container {
            width: 100%;
            max-width: 580px;
            background: var(--card-bg);
            backdrop-filter: blur(16px);
            border: 1px solid rgba(255, 255, 255, 0.8);
            border-radius: var(--radius-lg);
            padding: 40px;
            box-shadow: var(--shadow-lg);
            animation: fadeIn 0.4s ease-out;
        }

        @keyframes fadeIn {
            from { opacity: 0; transform: translateY(15px); }
            to { opacity: 1; transform: translateY(0); }
        }

        .header {
            text-align: center;
            margin-bottom: 28px;
        }

        .badge-success {
            display: inline-block;
            padding: 6px 14px;
            background: var(--success-light);
            color: var(--success);
            font-size: 13px;
            font-weight: 700;
            border-radius: 9999px;
            margin-bottom: 12px;
            letter-spacing: 0.5px;
            text-transform: uppercase;
        }

        .title {
            font-size: 26px;
            font-weight: 800;
            color: #0f172a;
            margin-bottom: 8px;
        }

        .subtitle {
            font-size: 14px;
            color: var(--text-muted);
        }

        .salary-highlight-card {
            background: linear-gradient(135deg, #10b981 0%, #059669 100%);
            border-radius: var(--radius-md);
            padding: 24px;
            color: #ffffff;
            text-align: center;
            margin-bottom: 24px;
            box-shadow: 0 10px 18px -3px rgba(16, 185, 129, 0.35);
        }

        .salary-label {
            font-size: 13px;
            font-weight: 600;
            text-transform: uppercase;
            letter-spacing: 1px;
            opacity: 0.9;
            margin-bottom: 6px;
        }

        .salary-amount {
            font-size: 34px;
            font-weight: 800;
            letter-spacing: -0.5px;
        }

        .details-card {
            background: #ffffff;
            border: 1px solid var(--border-color);
            border-radius: var(--radius-md);
            padding: 20px;
            margin-bottom: 24px;
        }

        .detail-row {
            display: flex;
            justify-content: space-between;
            align-items: center;
            padding: 12px 0;
            border-bottom: 1px solid #f1f5f9;
        }

        .detail-row:last-child {
            border-bottom: none;
        }

        .detail-label {
            display: flex;
            align-items: center;
            font-size: 14px;
            font-weight: 600;
            color: #475569;
        }

        .detail-label .icon {
            margin-right: 8px;
            font-size: 18px;
        }

        .detail-value {
            font-size: 15px;
            font-weight: 700;
            color: #0f172a;
        }

        .shifts-breakdown {
            display: grid;
            grid-template-columns: repeat(3, 1fr);
            gap: 10px;
            background: #f8fafc;
            border-radius: 8px;
            padding: 12px;
            margin-top: 10px;
            text-align: center;
        }

        .shift-item-name {
            font-size: 12px;
            font-weight: 600;
            color: #64748b;
            margin-bottom: 4px;
        }

        .shift-item-val {
            font-size: 14px;
            font-weight: 700;
            color: #1e293b;
        }

        .btn-back {
            display: flex;
            align-items: center;
            justify-content: center;
            gap: 8px;
            width: 100%;
            padding: 14px 24px;
            background: #f1f5f9;
            color: #334155;
            font-size: 15px;
            font-weight: 700;
            text-decoration: none;
            border-radius: var(--radius-md);
            transition: all 0.2s ease;
        }

        .btn-back:hover {
            background: #e2e8f0;
            color: #0f172a;
            transform: translateY(-1px);
        }

        @media (max-width: 500px) {
            .container {
                padding: 24px 18px;
            }
            .salary-amount {
                font-size: 28px;
            }
        }
    </style>
</head>
<body>

<div class="container">
    <div class="header">
        <div class="badge-success">&#10003; Tính Toán Hoàn Tất</div>
        <h1 class="title">Báo Cáo Lương Nhân Viên</h1>
        <p class="subtitle">Chi tiết tổng số giờ làm và tổng tiền lương nhận được</p>
    </div>

    <!-- Tổng tiền lương nổi bật -->
    <div class="salary-highlight-card">
        <div class="salary-label">Tổng Tiền Lương Nhận Được</div>
        <div class="salary-amount">${formattedTotalSalary} VNĐ</div>
    </div>

    <!-- Thông tin chi tiết -->
    <div class="details-card">
        <div class="detail-row">
            <span class="detail-label">
                <span class="icon">💰</span> Mức lương mỗi giờ:
            </span>
            <span class="detail-value">${formattedHourlyRate} VNĐ / giờ</span>
        </div>

        <div class="detail-row">
            <span class="detail-label">
                <span class="icon">⏱️</span> Tổng số giờ làm việc:
            </span>
            <span class="detail-value">${totalHours} giờ</span>
        </div>

        <div style="margin-top: 10px;">
            <span class="detail-label" style="font-size: 13px; color: #64748b;">
                <span class="icon">📅</span> Chi tiết số giờ từng ca làm việc:
            </span>
            <div class="shifts-breakdown">
                <div>
                    <div class="shift-item-name">🌅 Ca Sáng</div>
                    <div class="shift-item-val">${morningHours} h</div>
                </div>
                <div>
                    <div class="shift-item-name">☀️ Ca Chiều</div>
                    <div class="shift-item-val">${afternoonHours} h</div>
                </div>
                <div>
                    <div class="shift-item-name">🌙 Ca Tối</div>
                    <div class="shift-item-val">${eveningHours} h</div>
                </div>
            </div>
        </div>
    </div>

    <!-- Nút quay lại trang tính toán -->
    <a href="${pageContext.request.contextPath}/" class="btn-back">
        &#8592; Quay lại trang tính lương
    </a>
</div>

</body>
</html>
