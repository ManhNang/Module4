<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Danh Sách Tờ Khai Y Tế</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet">
    <style>
        :root {
            --primary: #0284c7;
            --primary-dark: #0369a1;
            --primary-light: #e0f2fe;
            --success: #10b981;
            --warning: #f59e0b;
            --danger: #ef4444;
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
            max-width: 1100px;
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

        .header h1 {
            font-size: 22px;
            font-weight: 700;
        }

        .header p {
            font-size: 13px;
            opacity: 0.9;
        }

        .btn-add {
            display: inline-flex;
            align-items: center;
            gap: 6px;
            background: #ffffff;
            color: var(--primary-dark);
            font-weight: 700;
            font-size: 14px;
            padding: 10px 18px;
            border-radius: 8px;
            text-decoration: none;
            box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
            transition: all 0.2s;
        }

        .btn-add:hover {
            background: var(--primary-light);
            transform: translateY(-1px);
        }

        .body-content {
            padding: 25px 30px 35px;
        }

        .table-responsive {
            overflow-x: auto;
            border-radius: 8px;
            border: 1px solid var(--gray-200);
        }

        table {
            width: 100%;
            border-collapse: collapse;
            text-align: left;
            font-size: 14px;
        }

        thead {
            background-color: var(--gray-50);
            border-bottom: 2px solid var(--gray-200);
        }

        th {
            padding: 14px 16px;
            font-weight: 700;
            color: var(--gray-700);
            white-space: nowrap;
        }

        td {
            padding: 14px 16px;
            border-bottom: 1px solid var(--gray-200);
            vertical-align: middle;
        }

        tbody tr:hover {
            background-color: #f8fafc;
        }

        .badge-id {
            display: inline-block;
            background: var(--gray-100);
            color: var(--gray-700);
            font-weight: 700;
            font-size: 12px;
            padding: 3px 8px;
            border-radius: 4px;
        }

        .badge-warning {
            background: #fee2e2;
            color: #b91c1c;
            padding: 3px 8px;
            border-radius: 12px;
            font-size: 11px;
            font-weight: 600;
            display: inline-block;
        }

        .badge-normal {
            background: #e0f2fe;
            color: #0369a1;
            padding: 3px 8px;
            border-radius: 12px;
            font-size: 11px;
            font-weight: 500;
            display: inline-block;
        }

        .btn-action {
            display: inline-flex;
            align-items: center;
            gap: 4px;
            padding: 6px 10px;
            border-radius: 6px;
            font-size: 12px;
            font-weight: 600;
            text-decoration: none;
            transition: all 0.2s;
        }

        .btn-view {
            background: var(--primary-light);
            color: var(--primary-dark);
        }

        .btn-view:hover {
            background: #bae6fd;
        }

        .btn-edit {
            background: #dcfce7;
            color: #15803d;
            margin-left: 6px;
        }

        .btn-edit:hover {
            background: #bbf7d0;
        }

        .alert-success {
            background-color: #ecfdf5;
            border-left: 4px solid var(--success);
            padding: 12px 18px;
            margin-bottom: 20px;
            border-radius: 6px;
            color: #065f46;
            font-size: 14px;
            font-weight: 600;
        }
    </style>
</head>
<body>

<div class="container">
    <div class="header">
        <div>
            <h1>DANH SÁCH TỜ KHAI Y TẾ</h1>
            <p>Hệ thống tiếp nhận thông tin khai báo y tế điện tử</p>
        </div>
        <a href="${pageContext.request.contextPath}/declarations/create" class="btn-add">
            ➕ Khai báo tờ khai mới
        </a>
    </div>

    <div class="body-content">
        <c:if test="${not empty successMessage}">
            <div class="alert-success">
                ✅ ${successMessage}
            </div>
        </c:if>

        <div class="table-responsive">
            <table>
                <thead>
                    <tr>
                        <th>Mã</th>
                        <th>Họ và tên</th>
                        <th>Năm sinh</th>
                        <th>Giới tính</th>
                        <th>Số CCCD/Hộ chiếu</th>
                        <th>Tỉnh/Thành phố</th>
                        <th>Số điện thoại</th>
                        <th>Triệu chứng</th>
                        <th>Tài liệu</th>
                        <th>Thao tác</th>
                    </tr>
                </thead>
                <tbody>
                    <c:forEach var="item" items="${declarations}">
                        <tr>
                            <td><span class="badge-id">#${item.id}</span></td>
                            <td style="font-weight: 600; color: var(--gray-800);">${item.fullName}</td>
                            <td>${item.birthYear}</td>
                            <td>${item.gender}</td>
                            <td>${item.idCard}</td>
                            <td>${item.province}</td>
                            <td>${item.phone}</td>
                            <td>
                                <c:choose>
                                    <c:when test="${not empty item.symptoms}">
                                        <span class="badge-warning">Có triệu chứng (${item.symptoms.size()})</span>
                                    </c:when>
                                    <c:otherwise>
                                        <span class="badge-normal">Bình thường</span>
                                    </c:otherwise>
                                </c:choose>
                            </td>
                            <td>
                                <c:choose>
                                    <c:when test="${not empty item.documentPath}">
                                        <a href="${pageContext.request.contextPath}${item.documentPath}" target="_blank" style="text-decoration: none;">📎 Có tệp</a>
                                    </c:when>
                                    <c:otherwise>
                                        <span style="color: var(--gray-600); font-size: 12px;">Không</span>
                                    </c:otherwise>
                                </c:choose>
                            </td>
                            <td style="white-space: nowrap;">
                                <a href="${pageContext.request.contextPath}/declarations/view?id=${item.id}" class="btn-action btn-view">
                                    👁️ Xem
                                </a>
                                <a href="${pageContext.request.contextPath}/declarations/edit?id=${item.id}" class="btn-action btn-edit">
                                    ✏️ Cập nhật
                                </a>
                            </td>
                        </tr>
                    </c:forEach>
                    <c:if test="${empty declarations}">
                        <tr>
                            <td colspan="10" style="text-align: center; padding: 30px; color: var(--gray-600);">
                                Chưa có tờ khai y tế nào trong hệ thống. Hãy nhấn <strong>"Khai báo tờ khai mới"</strong> để bắt đầu.
                            </td>
                        </tr>
                    </c:if>
                </tbody>
            </table>
        </div>
    </div>
</div>

</body>
</html>
