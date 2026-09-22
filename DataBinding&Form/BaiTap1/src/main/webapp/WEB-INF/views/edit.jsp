<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Cập nhật cấu hình hòm thư</title>
    <style>
        :root {
            --primary: #4f46e5;
            --primary-hover: #4338ca;
            --bg: #f8fafc;
            --card-bg: #ffffff;
            --text-main: #0f172a;
            --text-muted: #64748b;
            --border: #cbd5e1;
            --input-focus: #4f46e5;
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
            border: 1px solid #e2e8f0;
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

        .form-group {
            margin-bottom: 20px;
            display: flex;
            flex-direction: column;
            gap: 8px;
        }

        .form-label {
            font-weight: 600;
            font-size: 0.95rem;
            color: var(--text-main);
        }

        .form-control {
            width: 100%;
            padding: 10px 14px;
            border: 1px solid var(--border);
            border-radius: 8px;
            font-size: 0.95rem;
            color: var(--text-main);
            background-color: #fff;
            transition: border-color 0.2s, box-shadow 0.2s;
        }

        .form-control:focus {
            outline: none;
            border-color: var(--input-focus);
            box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.15);
        }

        .checkbox-group {
            flex-direction: row;
            align-items: center;
            gap: 12px;
            margin-top: 8px;
            margin-bottom: 20px;
        }

        .checkbox-group input[type="checkbox"] {
            width: 18px;
            height: 18px;
            accent-color: var(--primary);
            cursor: pointer;
        }

        .checkbox-label {
            font-weight: 500;
            font-size: 0.95rem;
            color: var(--text-main);
            cursor: pointer;
        }

        textarea.form-control {
            resize: vertical;
            min-height: 90px;
            font-family: inherit;
        }

        .btn-group {
            display: flex;
            gap: 12px;
            margin-top: 28px;
        }

        .btn {
            flex: 1;
            padding: 12px 20px;
            border-radius: 10px;
            font-size: 1rem;
            font-weight: 600;
            text-align: center;
            cursor: pointer;
            text-decoration: none;
            border: none;
            transition: all 0.2s ease;
        }

        .btn-primary {
            background-color: var(--primary);
            color: white;
            box-shadow: 0 4px 12px rgba(79, 70, 229, 0.25);
        }

        .btn-primary:hover {
            background-color: var(--primary-hover);
            box-shadow: 0 6px 16px rgba(79, 70, 229, 0.35);
        }

        .btn-secondary {
            background-color: #f1f5f9;
            color: #475569;
            border: 1px solid #cbd5e1;
        }

        .btn-secondary:hover {
            background-color: #e2e8f0;
            color: #1e293b;
        }
    </style>
</head>
<body>

<div class="container">
    <div class="header">
        <h1>Cập Nhật Cấu Hình</h1>
        <p>Thay đổi thiết lập hòm mail điện tử</p>
    </div>

    <div class="content">
        <form:form modelAttribute="emailConfig" action="${pageContext.request.contextPath}/settings/update" method="post">
            <div class="form-group">
                <label for="languages" class="form-label">Languages</label>
                <form:select path="languages" id="languages" class="form-control" items="${languagesList}" />
            </div>

            <div class="form-group">
                <label for="pageSize" class="form-label">Page Size</label>
                <div style="display: flex; align-items: center; gap: 8px;">
                    <span style="color: var(--text-muted); font-size: 0.9rem;">Show</span>
                    <form:select path="pageSize" id="pageSize" class="form-control" items="${pageSizeList}" style="width: 100px;" />
                    <span style="color: var(--text-muted); font-size: 0.9rem;">emails per page</span>
                </div>
            </div>

            <div class="form-group checkbox-group">
                <form:checkbox path="spamsFilter" id="spamsFilter" />
                <label for="spamsFilter" class="checkbox-label">Enable spams filter</label>
            </div>

            <div class="form-group">
                <label for="signature" class="form-label">Signature</label>
                <form:textarea path="signature" id="signature" class="form-control" rows="4" placeholder="Nhập chữ ký hòm thư..." />
            </div>

            <div class="btn-group">
                <button type="submit" class="btn btn-primary">Update</button>
                <a href="${pageContext.request.contextPath}/settings" class="btn btn-secondary">Cancel</a>
            </div>
        </form:form>
    </div>
</div>

</body>
</html>
