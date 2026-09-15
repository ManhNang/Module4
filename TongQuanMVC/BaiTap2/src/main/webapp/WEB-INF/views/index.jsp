<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Từ Điển Anh - Việt | Spring MVC</title>
    <!-- Google Fonts -->
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&display=swap" rel="stylesheet">
    <style>
        :root {
            --primary: #4f46e5;
            --primary-hover: #4338ca;
            --primary-light: #eef2ff;
            --success-bg: #ecfdf5;
            --success-text: #065f46;
            --success-border: #a7f3d0;
            --error-bg: #fef2f2;
            --error-text: #991b1b;
            --error-border: #fecaca;
            --text-main: #0f172a;
            --text-muted: #64748b;
            --card-bg: rgba(255, 255, 255, 0.92);
            --border-color: #e2e8f0;
            --radius-lg: 20px;
            --radius-md: 12px;
            --radius-sm: 8px;
            --shadow-card: 0 20px 40px -15px rgba(0, 0, 0, 0.08), 0 0 0 1px rgba(226, 232, 240, 0.8);
        }

        * {
            box-sizing: border-box;
            margin: 0;
            padding: 0;
        }

        body {
            font-family: 'Plus Jakarta Sans', sans-serif;
            background: linear-gradient(135deg, #f0f4ff 0%, #e0e7ff 50%, #f5f3ff 100%);
            min-height: 100vh;
            display: flex;
            align-items: center;
            justify-content: center;
            padding: 24px 16px;
            color: var(--text-main);
        }

        .container {
            width: 100%;
            max-width: 620px;
        }

        .card {
            background: var(--card-bg);
            backdrop-filter: blur(16px);
            border-radius: var(--radius-lg);
            padding: 36px 32px;
            box-shadow: var(--shadow-card);
            border: 1px solid rgba(255, 255, 255, 0.6);
            animation: fadeIn 0.4s ease-out;
        }

        @keyframes fadeIn {
            from {
                opacity: 0;
                transform: translateY(12px);
            }
            to {
                opacity: 1;
                transform: translateY(0);
            }
        }

        .header {
            text-align: center;
            margin-bottom: 28px;
        }

        .badge-app {
            display: inline-flex;
            align-items: center;
            gap: 6px;
            background: var(--primary-light);
            color: var(--primary);
            font-size: 12px;
            font-weight: 700;
            text-transform: uppercase;
            letter-spacing: 0.05em;
            padding: 6px 14px;
            border-radius: 9999px;
            margin-bottom: 12px;
        }

        .header h1 {
            font-size: 28px;
            font-weight: 800;
            color: var(--text-main);
            letter-spacing: -0.02em;
            margin-bottom: 8px;
        }

        .header p {
            color: var(--text-muted);
            font-size: 15px;
        }

        .search-form {
            display: flex;
            gap: 10px;
            margin-bottom: 24px;
        }

        .input-group {
            position: relative;
            flex: 1;
        }

        .input-group input {
            width: 100%;
            height: 52px;
            padding: 0 18px 0 46px;
            font-size: 16px;
            font-family: inherit;
            border: 2px solid var(--border-color);
            border-radius: var(--radius-md);
            outline: none;
            background: #ffffff;
            transition: all 0.2s ease;
            color: var(--text-main);
        }

        .input-group input:focus {
            border-color: var(--primary);
            box-shadow: 0 0 0 4px rgba(79, 70, 229, 0.12);
        }

        .input-group svg {
            position: absolute;
            left: 16px;
            top: 50%;
            transform: translateY(-50%);
            width: 20px;
            height: 20px;
            color: var(--text-muted);
            pointer-events: none;
        }

        .btn-submit {
            height: 52px;
            padding: 0 24px;
            background: var(--primary);
            color: #ffffff;
            border: none;
            border-radius: var(--radius-md);
            font-size: 15px;
            font-weight: 700;
            font-family: inherit;
            cursor: pointer;
            transition: all 0.2s ease;
            display: inline-flex;
            align-items: center;
            gap: 8px;
            white-space: nowrap;
        }

        .btn-submit:hover {
            background: var(--primary-hover);
            transform: translateY(-1px);
            box-shadow: 0 6px 16px rgba(79, 70, 229, 0.3);
        }

        .btn-submit:active {
            transform: translateY(0);
        }

        /* Result Section */
        .result-box {
            border-radius: var(--radius-md);
            padding: 20px;
            margin-bottom: 24px;
            border-left: 4px solid;
            animation: fadeIn 0.3s ease;
        }

        .result-success {
            background: var(--success-bg);
            border-color: #10b981;
        }

        .result-error {
            background: var(--error-bg);
            border-color: #ef4444;
        }

        .result-label {
            font-size: 12px;
            font-weight: 700;
            text-transform: uppercase;
            letter-spacing: 0.05em;
            margin-bottom: 6px;
        }

        .result-success .result-label {
            color: #059669;
        }

        .result-error .result-label {
            color: #dc2626;
        }

        .result-content {
            display: flex;
            flex-direction: column;
            gap: 4px;
        }

        .result-word {
            font-size: 18px;
            font-weight: 700;
            color: var(--text-main);
        }

        .result-meaning {
            font-size: 20px;
            font-weight: 800;
            color: #065f46;
        }

        .result-message {
            font-size: 15px;
            font-weight: 600;
            color: var(--error-text);
        }

        /* Sample Words Section */
        .sample-section {
            border-top: 1px solid var(--border-color);
            padding-top: 20px;
        }

        .sample-title {
            font-size: 13px;
            font-weight: 600;
            color: var(--text-muted);
            margin-bottom: 12px;
            display: flex;
            align-items: center;
            gap: 6px;
        }

        .sample-tags {
            display: flex;
            flex-wrap: wrap;
            gap: 8px;
        }

        .tag-word {
            font-size: 13px;
            font-weight: 600;
            color: #374151;
            background: #f3f4f6;
            padding: 6px 12px;
            border-radius: var(--radius-sm);
            cursor: pointer;
            border: 1px solid #e5e7eb;
            transition: all 0.15s ease;
            text-decoration: none;
        }

        .tag-word:hover {
            background: var(--primary);
            color: #ffffff;
            border-color: var(--primary);
            transform: translateY(-1px);
        }

        .footer {
            text-align: center;
            margin-top: 20px;
            font-size: 13px;
            color: var(--text-muted);
        }

        .footer span {
            font-weight: 600;
            color: var(--primary);
        }
    </style>
</head>
<body>

<div class="container">
    <div class="card">
        <div class="header">
            <span class="badge-app">
                <svg width="12" height="12" viewBox="0 0 24 24" fill="currentColor">
                    <circle cx="12" cy="12" r="10"/>
                </svg>
                Spring MVC 6.2.6 &bull; Tomcat 10.1+
            </span>
            <h1>Từ Điển Anh - Việt</h1>
            <p>Tra cứu nhanh nghĩa của các từ tiếng Anh thông dụng</p>
        </div>

        <!-- Form tra cứu -->
        <form action="${pageContext.request.contextPath}/lookup" method="POST" class="search-form">
            <div class="input-group">
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                </svg>
                <input type="text"
                       id="searchInput"
                       name="word"
                       placeholder="Nhập từ tiếng Anh cần tra (vd: hello, computer, software)..."
                       value="${word}"
                       required
                       autofocus />
            </div>
            <button type="submit" class="btn-submit">
                Tra cứu
            </button>
        </form>

        <!-- Hiển thị kết quả tra cứu -->
        <c:if test="${hasSearched}">
            <c:choose>
                <c:when test="${found}">
                    <div class="result-box result-success">
                        <div class="result-label">&#10004; Tìm thấy kết quả</div>
                        <div class="result-content">
                            <span class="result-word">Từ tiếng Anh: <em>${word}</em></span>
                            <span class="result-meaning">Nghĩa tiếng Việt: ${meaning}</span>
                        </div>
                    </div>
                </c:when>
                <c:otherwise>
                    <div class="result-box result-error">
                        <div class="result-label">&#9888; Kết quả tra cứu</div>
                        <div class="result-message">${message}</div>
                    </div>
                </c:otherwise>
            </c:choose>
        </c:if>

        <!-- Danh sách từ mẫu có sẵn trong từ điển -->
        <div class="sample-section">
            <div class="sample-title">
                <svg width="14" height="14" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
                Gợi ý từ có trong từ điển (nhấp để tra cứu nhanh):
            </div>
            <div class="sample-tags">
                <c:forEach var="sample" items="${sampleWords}">
                    <a href="${pageContext.request.contextPath}/lookup?word=${sample}"
                       class="tag-word">${sample}</a>
                </c:forEach>
            </div>
        </div>
    </div>

    <div class="footer">
        Dự án <strong>spring-greeting</strong> | CodeGym Module 4 &bull; <span>Spring MVC</span>
    </div>
</div>

<script>
    function quickFill(word) {
        var input = document.getElementById('searchInput');
        input.value = word;
        input.focus();
    }
</script>

</body>
</html>
