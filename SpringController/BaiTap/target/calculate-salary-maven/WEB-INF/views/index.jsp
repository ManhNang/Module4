<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Hệ Thống Tính Lương Nhân Viên</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&display=swap" rel="stylesheet">
    <style>
        :root {
            --primary: #4f46e5;
            --primary-hover: #4338ca;
            --primary-light: #eef2ff;
            --bg-gradient: linear-gradient(135deg, #f0f4ff 0%, #e0e7ff 50%, #f8fafc 100%);
            --card-bg: rgba(255, 255, 255, 0.92);
            --text-main: #1e293b;
            --text-muted: #64748b;
            --border-color: #e2e8f0;
            --focus-ring: rgba(79, 70, 229, 0.2);
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
            margin-bottom: 32px;
        }

        .badge {
            display: inline-block;
            padding: 6px 14px;
            background: var(--primary-light);
            color: var(--primary);
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

        .form-section {
            margin-bottom: 24px;
        }

        .section-label {
            display: flex;
            align-items: center;
            font-size: 14px;
            font-weight: 700;
            color: #334155;
            margin-bottom: 12px;
            text-transform: uppercase;
            letter-spacing: 0.5px;
        }

        .section-label span {
            display: inline-flex;
            align-items: center;
            justify-content: center;
            width: 24px;
            height: 24px;
            background: var(--primary-light);
            color: var(--primary);
            border-radius: 50%;
            margin-right: 8px;
            font-size: 12px;
        }

        .shift-grid {
            display: grid;
            grid-template-columns: repeat(3, 1fr);
            gap: 14px;
        }

        .shift-card {
            background: #ffffff;
            border: 1px solid var(--border-color);
            border-radius: var(--radius-md);
            padding: 16px 12px;
            text-align: center;
            transition: all 0.2s ease;
        }

        .shift-card:focus-within {
            border-color: var(--primary);
            box-shadow: 0 0 0 3px var(--focus-ring);
        }

        .shift-icon {
            font-size: 24px;
            margin-bottom: 6px;
        }

        .shift-title {
            font-size: 13px;
            font-weight: 600;
            color: #475569;
            margin-bottom: 8px;
        }

        .input-wrapper {
            position: relative;
        }

        .shift-input {
            width: 100%;
            padding: 10px 8px;
            font-size: 16px;
            font-weight: 700;
            text-align: center;
            color: #0f172a;
            background: #f8fafc;
            border: 1px solid #cbd5e1;
            border-radius: 8px;
            outline: none;
            transition: border-color 0.2s;
        }

        .shift-input:focus {
            border-color: var(--primary);
            background: #ffffff;
        }

        .select-group {
            background: #ffffff;
            border: 1px solid var(--border-color);
            border-radius: var(--radius-md);
            padding: 16px;
            transition: border-color 0.2s;
        }

        .select-group:focus-within {
            border-color: var(--primary);
            box-shadow: 0 0 0 3px var(--focus-ring);
        }

        .select-label {
            font-size: 13px;
            font-weight: 600;
            color: #475569;
            margin-bottom: 8px;
            display: block;
        }

        .custom-select {
            width: 100%;
            padding: 12px 16px;
            font-size: 15px;
            font-weight: 600;
            color: #1e293b;
            background-color: #f8fafc;
            border: 1px solid #cbd5e1;
            border-radius: 8px;
            outline: none;
            cursor: pointer;
            transition: border-color 0.2s;
        }

        .custom-select:focus {
            border-color: var(--primary);
            background-color: #ffffff;
        }

        .btn-submit {
            width: 100%;
            padding: 14px 24px;
            background: linear-gradient(135deg, #4f46e5 0%, #6366f1 100%);
            color: #ffffff;
            font-size: 16px;
            font-weight: 700;
            border: none;
            border-radius: var(--radius-md);
            cursor: pointer;
            box-shadow: 0 10px 15px -3px rgba(79, 70, 229, 0.35);
            transition: all 0.2s ease;
            margin-top: 10px;
        }

        .btn-submit:hover {
            background: linear-gradient(135deg, #4338ca 0%, #4f46e5 100%);
            transform: translateY(-2px);
            box-shadow: 0 14px 20px -3px rgba(79, 70, 229, 0.4);
        }

        .btn-submit:active {
            transform: translateY(0);
        }

        @media (max-width: 500px) {
            .container {
                padding: 24px 18px;
            }
            .shift-grid {
                grid-template-columns: 1fr;
            }
        }
    </style>
</head>
<body>

<div class="container">
    <div class="header">
        <div class="badge">Spring MVC 6.2 &bull; Jakarta EE 10</div>
        <h1 class="title">Bảng Tính Lương Nhân Viên</h1>
        <p class="subtitle">Nhập số giờ làm việc theo từng ca và chọn mức lương theo giờ</p>
    </div>

    <form action="${pageContext.request.contextPath}/calculate" method="post">
        <!-- 3 ca làm việc -->
        <div class="form-section">
            <div class="section-label">
                <span>1</span> Số giờ làm việc theo ca
            </div>
            <div class="shift-grid">
                <!-- Ca Sáng -->
                <div class="shift-card">
                    <div class="shift-icon">🌅</div>
                    <div class="shift-title">Ca Sáng</div>
                    <div class="input-wrapper">
                        <input type="number" step="0.5" min="0" max="24" class="shift-input"
                               name="shiftHours" value="4.0" required placeholder="Giờ">
                    </div>
                </div>

                <!-- Ca Chiều -->
                <div class="shift-card">
                    <div class="shift-icon">☀️</div>
                    <div class="shift-title">Ca Chiều</div>
                    <div class="input-wrapper">
                        <input type="number" step="0.5" min="0" max="24" class="shift-input"
                               name="shiftHours" value="4.0" required placeholder="Giờ">
                    </div>
                </div>

                <!-- Ca Tối -->
                <div class="shift-card">
                    <div class="shift-icon">🌙</div>
                    <div class="shift-title">Ca Tối</div>
                    <div class="input-wrapper">
                        <input type="number" step="0.5" min="0" max="24" class="shift-input"
                               name="shiftHours" value="0.0" required placeholder="Giờ">
                    </div>
                </div>
            </div>
        </div>

        <!-- Mức lương theo giờ -->
        <div class="form-section">
            <div class="section-label">
                <span>2</span> Mức lương theo giờ
            </div>
            <div class="select-group">
                <label for="hourlyRateSelect" class="select-label">Chọn mức lương áp dụng (VNĐ/giờ):</label>
                <select id="hourlyRateSelect" name="hourlyRate" class="custom-select" required>
                    <option value="25000">25,000 VNĐ / giờ (Bán thời gian cơ bản)</option>
                    <option value="35000" selected>35,000 VNĐ / giờ (Nhân viên chính thức)</option>
                    <option value="50000">50,000 VNĐ / giờ (Trưởng ca / Chuyên viên)</option>
                    <option value="75000">75,000 VNĐ / giờ (Quản lý)</option>
                    <option value="100000">100,000 VNĐ / giờ (Chuyên gia / Cố vấn)</option>
                </select>
            </div>
        </div>

        <!-- Nút tính toán -->
        <button type="submit" class="btn-submit">
            📊 Tính Lương Ngay
        </button>
    </form>
</div>

</body>
</html>
