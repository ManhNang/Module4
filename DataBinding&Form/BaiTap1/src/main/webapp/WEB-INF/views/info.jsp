<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Cấu hình hòm thư điện tử</title>
    <style>
        :root {
            --primary: #4f46e5;
            --primary-hover: #4338ca;
            --bg: #f8fafc;
            --card-bg: #ffffff;
            --text-main: #0f172a;
            --text-muted: #64748b;
            --border: #e2e8f0;
            --success-bg: #ecfdf5;
            --success-text: #065f46;
            --success-border: #a7f3d0;
        }

        * {
            box-sizing: border-box;
            margin: 0;
            padding: 0;
            font-family: 'Segoe UI', system-ui, -apple-system, sans-serif;
        }

        body {
            background-color: var(--bg);
            color: var(--text-main);
            min-height: 100vh;
            display: flex;
            align-items: center;
            justify-content: center;
            padding: 24px;
        }

        .container {
            width: 100%;
            max-width: 540px;
            background: var(--card-bg);
            border-radius: 16px;
            box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.05), 0 8px 10px -6px rgba(0, 0, 0, 0.02);
            border: 1px solid var(--border);
            overflow: hidden;
        }

        .header {
            background: linear-gradient(135deg, #4f46e5, #6366f1);
            color: white;
            padding: 28px 32px;
        }

        .header h1 {
            font-size: 1.5rem;
            font-weight: 700;
            margin-bottom: 6px;
        }

        .header p {
            font-size: 0.925rem;
            opacity: 0.9;
        }

        .content {
            padding: 32px;
        }

        .alert-success {
            background-color: var(--success-bg);
            color: var(--success-text);
            border: 1px solid var(--success-border);
            padding: 12px 16px;
            border-radius: 8px;
            margin-bottom: 24px;
            font-size: 0.95rem;
            display: flex;
            align-items: center;
            gap: 8px;
        }

        .detail-group {
            display: flex;
            flex-direction: column;
            gap: 18px;
            margin-bottom: 28px;
        }

        .detail-item {
            display: flex;
            justify-content: space-between;
            align-items: flex-start;
            padding-bottom: 14px;
            border-bottom: 1px dashed var(--border);
        }

        .detail-item:last-child {
            border-bottom: none;
            padding-bottom: 0;
        }

        .detail-label {
            font-weight: 600;
            color: var(--text-muted);
            font-size: 0.95rem;
            flex: 1;
        }

        .detail-value {
            font-weight: 500;
            color: var(--text-main);
            text-align: right;
            flex: 1.5;
        }

        .badge {
            display: inline-block;
            padding: 4px 10px;
            border-radius: 9999px;
            font-size: 0.85rem;
            font-weight: 600;
        }

        .badge-active {
            background-color: #dbeafe;
            color: #1e40af;
        }

        .badge-inactive {
            background-color: #f1f5f9;
            color: #64748b;
        }

        .signature-box {
            background-color: #f8fafc;
            border: 1px solid var(--border);
            padding: 12px 16px;
            border-radius: 8px;
            white-space: pre-wrap;
            font-family: monospace;
            font-size: 0.9rem;
            color: #334155;
            text-align: left;
            margin-top: 6px;
        }

        .btn {
            display: inline-block;
            width: 100%;
            text-align: center;
            background-color: var(--primary);
            color: white;
            padding: 12px 20px;
            border-radius: 10px;
            font-size: 1rem;
            font-weight: 600;
            text-decoration: none;
            transition: all 0.2s ease;
            box-shadow: 0 4px 12px rgba(79, 70, 229, 0.25);
            border: none;
            cursor: pointer;
        }

        .btn:hover {
            background-color: var(--primary-hover);
            box-shadow: 0 6px 16px rgba(79, 70, 229, 0.35);
            transform: translateY(-1px);
        }
    </style>
</head>
<body>

<div class="container">
    <div class="header">
        <h1>Cấu Hình Hòm Thư</h1>
        <p>Thông tin thiết lập hiện tại của hòm mail điện tử</p>
    </div>

    <div class="content">
        <c:if test="${not empty message}">
            <div class="alert-success">
                <span>✔</span>
                <span>${message}</span>
            </div>
        </c:if>

        <div class="detail-group">
            <div class="detail-item">
                <span class="detail-label">Languages:</span>
                <span class="detail-value"><strong>${emailConfig.languages}</strong></span>
            </div>
            <div class="detail-item">
                <span class="detail-label">Page Size:</span>
                <span class="detail-value">Show <strong>${emailConfig.pageSize}</strong> emails per page</span>
            </div>
            <div class="detail-item">
                <span class="detail-label">Spams Filter:</span>
                <span class="detail-value">
                    <c:choose>
                        <c:when test="${emailConfig.spamsFilter}">
                            <span class="badge badge-active">✔ Enabled</span>
                        </c:when>
                        <c:otherwise>
                            <span class="badge badge-inactive">✖ Disabled</span>
                        </c:otherwise>
                    </c:choose>
                </span>
            </div>
            <div class="detail-item" style="flex-direction: column; align-items: stretch; gap: 6px;">
                <span class="detail-label">Signature:</span>
                <div class="signature-box">${emailConfig.signature}</div>
            </div>
        </div>

        <a href="${pageContext.request.contextPath}/settings/edit" class="btn">Chỉnh sửa cấu hình</a>
    </div>
</div>

</body>
</html>
