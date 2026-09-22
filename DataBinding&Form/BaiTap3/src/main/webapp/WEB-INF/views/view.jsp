<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Chi Tiết Tờ Khai Y Tế #${declaration.id}</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet">
    <style>
        :root {
            --primary: #0284c7;
            --primary-dark: #0369a1;
            --primary-light: #e0f2fe;
            --success: #10b981;
            --success-bg: #ecfdf5;
            --gray-50: #f8fafc;
            --gray-100: #f1f5f9;
            --gray-200: #e2e8f0;
            --gray-300: #cbd5e1;
            --gray-600: #475569;
            --gray-700: #334155;
            --gray-800: #1e293b;
            --radius: 12px;
            --shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.05), 0 8px 10px -6px rgba(0, 0, 0, 0.05);
        }

        * {
            box-sizing: border-box;
            margin: 0;
            padding: 0;
        }

        body {
            font-family: 'Inter', sans-serif;
            background: linear-gradient(135deg, #f0f9ff 0%, #e0f2fe 100%);
            color: var(--gray-800);
            min-height: 100vh;
            padding: 40px 20px;
        }

        .container {
            max-width: 850px;
            margin: 0 auto;
            background: #ffffff;
            border-radius: 16px;
            box-shadow: var(--shadow);
            overflow: hidden;
            border: 1px solid rgba(2, 132, 199, 0.1);
        }

        .header {
            background: linear-gradient(135deg, #0284c7 0%, #0369a1 100%);
            color: #ffffff;
            padding: 25px 30px;
            display: flex;
            justify-content: space-between;
            align-items: center;
        }

        .header-title h1 {
            font-size: 22px;
            font-weight: 700;
            margin-bottom: 4px;
        }

        .header-title p {
            font-size: 13px;
            opacity: 0.9;
        }

        .badge-id {
            background: rgba(255, 255, 255, 0.2);
            border: 1px solid rgba(255, 255, 255, 0.3);
            padding: 6px 14px;
            border-radius: 20px;
            font-weight: 600;
            font-size: 14px;
        }

        .alert-success {
            background-color: var(--success-bg);
            border-left: 4px solid var(--success);
            padding: 14px 20px;
            margin: 20px 30px 0;
            border-radius: 6px;
            color: #065f46;
            font-weight: 600;
            font-size: 14px;
        }

        .card-body {
            padding: 30px;
        }

        .section-header {
            display: flex;
            align-items: center;
            gap: 10px;
            color: var(--primary-dark);
            font-size: 16px;
            font-weight: 700;
            margin: 25px 0 15px;
            padding-bottom: 8px;
            border-bottom: 2px solid var(--gray-100);
        }

        .section-header:first-of-type {
            margin-top: 0;
        }

        .info-grid {
            display: grid;
            grid-template-columns: repeat(2, 1fr);
            gap: 15px 25px;
        }

        @media (max-width: 650px) {
            .info-grid {
                grid-template-columns: 1fr;
            }
        }

        .info-item {
            display: flex;
            flex-direction: column;
            gap: 4px;
        }

        .info-label {
            font-size: 12px;
            text-transform: uppercase;
            letter-spacing: 0.5px;
            font-weight: 600;
            color: var(--gray-600);
        }

        .info-value {
            font-size: 15px;
            font-weight: 600;
            color: var(--gray-800);
        }

        .chip-list {
            display: flex;
            flex-wrap: wrap;
            gap: 8px;
            margin-top: 5px;
        }

        .chip {
            background: var(--primary-light);
            color: var(--primary-dark);
            padding: 4px 12px;
            border-radius: 16px;
            font-size: 13px;
            font-weight: 500;
        }

        .chip-warning {
            background: #fee2e2;
            color: #b91c1c;
        }

        .chip-normal {
            background: #e2e8f0;
            color: var(--gray-600);
        }

        .attached-file-box {
            background: var(--gray-50);
            border: 1px solid var(--gray-200);
            border-radius: var(--radius);
            padding: 15px;
            display: flex;
            align-items: center;
            gap: 15px;
            margin-top: 10px;
        }

        .preview-img {
            max-width: 250px;
            max-height: 180px;
            border-radius: 8px;
            border: 1px solid var(--gray-300);
            box-shadow: 0 2px 5px rgba(0, 0, 0, 0.1);
        }

        .actions {
            display: flex;
            gap: 12px;
            margin-top: 35px;
            padding-top: 20px;
            border-top: 1px solid var(--gray-200);
        }

        .btn {
            display: inline-flex;
            align-items: center;
            justify-content: center;
            gap: 8px;
            padding: 10px 20px;
            border-radius: 8px;
            font-weight: 600;
            font-size: 14px;
            text-decoration: none;
            cursor: pointer;
            transition: all 0.2s;
            border: none;
        }

        .btn-primary {
            background: var(--primary);
            color: white;
        }

        .btn-primary:hover {
            background: var(--primary-dark);
        }

        .btn-success {
            background: var(--success);
            color: white;
        }

        .btn-success:hover {
            background: #059669;
        }

        .btn-outline {
            background: white;
            color: var(--gray-700);
            border: 1px solid var(--gray-300);
        }

        .btn-outline:hover {
            background: var(--gray-100);
        }
    </style>
</head>
<body>

<div class="container">
    <div class="header">
        <div class="header-title">
            <h1>PHIẾU KHAI BÁO Y TẾ</h1>
            <p>Hệ thống giám sát và phòng dịch tễ Bộ Y Tế</p>
        </div>
        <div class="badge-id">Mã số: #${declaration.id}</div>
    </div>

    <c:if test="${not empty successMessage}">
        <div class="alert-success">
            ✅ ${successMessage}
        </div>
    </c:if>

    <div class="card-body">
        <!-- Phần 1: Thông tin cá nhân -->
        <div class="section-header">
            👤 THÔNG TIN CÁ NHÂN
        </div>
        <div class="info-grid">
            <div class="info-item">
                <span class="info-label">Họ và tên</span>
                <span class="info-value" style="color: var(--primary-dark); font-size: 17px;">${declaration.fullName}</span>
            </div>
            <div class="info-item">
                <span class="info-label">Năm sinh / Tuổi</span>
                <span class="info-value">${declaration.birthYear}</span>
            </div>
            <div class="info-item">
                <span class="info-label">Giới tính</span>
                <span class="info-value">${declaration.gender}</span>
            </div>
            <div class="info-item">
                <span class="info-label">Quốc tịch</span>
                <span class="info-value">${declaration.nationality}</span>
            </div>
            <div class="info-item" style="grid-column: span 2;">
                <span class="info-label">Số CMND / CCCD / Hộ chiếu</span>
                <span class="info-value">${declaration.idCard}</span>
            </div>
        </div>

        <!-- Phần 2: Thông tin di chuyển -->
        <div class="section-header">
            ✈️ THÔNG TIN DI CHUYỂN
        </div>
        <div class="info-grid">
            <div class="info-item">
                <span class="info-label">Phương tiện</span>
                <span class="info-value">${empty declaration.travelInfo ? 'Không di chuyển' : declaration.travelInfo}</span>
            </div>
            <div class="info-item">
                <span class="info-label">Số hiệu phương tiện</span>
                <span class="info-value">${empty declaration.vehicleNumber ? '---' : declaration.vehicleNumber}</span>
            </div>
            <div class="info-item">
                <span class="info-label">Số ghế</span>
                <span class="info-value">${empty declaration.seatNumber ? '---' : declaration.seatNumber}</span>
            </div>
            <div class="info-item">
                <span class="info-label">Thời gian (Khởi hành - Kết thúc)</span>
                <span class="info-value">${empty declaration.departureDate ? '---' : declaration.departureDate} đến ${empty declaration.arrivalDate ? '---' : declaration.arrivalDate}</span>
            </div>
            <div class="info-item" style="grid-column: span 2;">
                <span class="info-label">Tỉnh / Thành phố đã đến (14 ngày qua)</span>
                <span class="info-value">${empty declaration.visitedCities ? 'Không' : declaration.visitedCities}</span>
            </div>
        </div>

        <!-- Phần 3: Địa chỉ liên lạc -->
        <div class="section-header">
            📍 ĐỊA CHỈ LIÊN LẠC TẠI VIỆT NAM
        </div>
        <div class="info-grid">
            <div class="info-item">
                <span class="info-label">Tỉnh / Thành phố</span>
                <span class="info-value">${declaration.province}</span>
            </div>
            <div class="info-item">
                <span class="info-label">Quận / Huyện</span>
                <span class="info-value">${declaration.district}</span>
            </div>
            <div class="info-item">
                <span class="info-label">Phường / Xã</span>
                <span class="info-value">${declaration.ward}</span>
            </div>
            <div class="info-item">
                <span class="info-label">Địa chỉ chi tiết</span>
                <span class="info-value">${empty declaration.addressDetail ? '---' : declaration.addressDetail}</span>
            </div>
            <div class="info-item">
                <span class="info-label">Số điện thoại</span>
                <span class="info-value" style="color: var(--primary);">${declaration.phone}</span>
            </div>
            <div class="info-item">
                <span class="info-label">Email</span>
                <span class="info-value">${empty declaration.email ? '---' : declaration.email}</span>
            </div>
        </div>

        <!-- Phần 4: Triệu chứng & Phơi nhiễm -->
        <div class="section-header">
            🩺 THEO DÕI TRIỆU CHỨNG & PHƠI NHIỄM
        </div>
        <div style="margin-bottom: 15px;">
            <span class="info-label">Triệu chứng xuất hiện trong 14 ngày qua:</span>
            <div class="chip-list">
                <c:choose>
                    <c:when test="${not empty declaration.symptoms}">
                        <c:forEach var="s" items="${declaration.symptoms}">
                            <span class="chip chip-warning">⚠️ ${s}</span>
                        </c:forEach>
                    </c:when>
                    <c:otherwise>
                        <span class="chip chip-normal">Không có triệu chứng</span>
                    </c:otherwise>
                </c:choose>
            </div>
        </div>

        <div>
            <span class="info-label">Lịch sử tiếp xúc / Phơi nhiễm:</span>
            <div class="chip-list">
                <c:choose>
                    <c:when test="${not empty declaration.exposures}">
                        <c:forEach var="e" items="${declaration.exposures}">
                            <span class="chip chip-warning">⚠️ ${e}</span>
                        </c:forEach>
                    </c:when>
                    <c:otherwise>
                        <span class="chip chip-normal">Không có yếu tố phơi nhiễm</span>
                    </c:otherwise>
                </c:choose>
            </div>
        </div>

        <!-- Phần 5: Tệp đính kèm -->
        <c:if test="${not empty declaration.documentPath}">
            <div class="section-header">
                📎 TÀI LIỆU ĐÍNH KÈM
            </div>
            <div class="attached-file-box">
                <c:set var="pathLower" value="${declaration.documentPath.toLowerCase()}"/>
                <c:choose>
                    <c:when test="${pathLower.endsWith('.jpg') || pathLower.endsWith('.jpeg') || pathLower.endsWith('.png') || pathLower.endsWith('.webp')}">
                        <img src="${pageContext.request.contextPath}${declaration.documentPath}" alt="Tài liệu y tế" class="preview-img"/>
                    </c:when>
                    <c:otherwise>
                        <div style="font-size: 32px;">📄</div>
                    </c:otherwise>
                </c:choose>
                <div>
                    <div style="font-weight: 600; margin-bottom: 4px;">${declaration.documentName != null ? declaration.documentName : 'Tài liệu đính kèm'}</div>
                    <a href="${pageContext.request.contextPath}${declaration.documentPath}" target="_blank" class="btn btn-outline" style="padding: 6px 12px; font-size: 13px;">
                        🔍 Xem / Tải về tệp
                    </a>
                </div>
            </div>
        </c:if>

        <!-- Thao tác hành động -->
        <div class="actions">
            <a href="${pageContext.request.contextPath}/declarations/edit?id=${declaration.id}" class="btn btn-success">
                ✏️ Cập nhật thông tin tờ khai này
            </a>
            <a href="${pageContext.request.contextPath}/declarations/create" class="btn btn-primary">
                ➕ Khai báo tờ khai mới
            </a>
            <a href="${pageContext.request.contextPath}/declarations/list" class="btn btn-outline">
                📋 Danh sách các tờ khai
            </a>
        </div>
    </div>
</div>

</body>
</html>
