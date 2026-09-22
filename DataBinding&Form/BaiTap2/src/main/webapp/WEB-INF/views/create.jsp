<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Tải Lên Bài Hát Mới - SoundWave</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700&display=swap" rel="stylesheet">
    <style>
        :root {
            --primary: #6366f1;
            --primary-hover: #4f46e5;
            --primary-light: #e0e7ff;
            --secondary: #ec4899;
            --bg-gradient: linear-gradient(135deg, #0f172a 0%, #1e1b4b 50%, #311042 100%);
            --card-bg: rgba(255, 255, 255, 0.96);
            --text-dark: #0f172a;
            --text-muted: #64748b;
            --border-color: #e2e8f0;
            --danger-bg: #fef2f2;
            --danger-border: #fecaca;
            --danger-text: #991b1b;
        }

        * {
            box-sizing: border-box;
            margin: 0;
            padding: 0;
            font-family: 'Plus Jakarta Sans', -apple-system, BlinkMacSystemFont, sans-serif;
        }

        body {
            min-height: 100vh;
            background: var(--bg-gradient);
            display: flex;
            align-items: center;
            justify-content: center;
            padding: 40px 20px;
            color: var(--text-dark);
        }

        .container {
            width: 100%;
            max-width: 680px;
            background: var(--card-bg);
            backdrop-filter: blur(16px);
            border-radius: 24px;
            box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.4);
            overflow: hidden;
            border: 1px solid rgba(255, 255, 255, 0.2);
            animation: fadeIn 0.4s ease-out;
        }

        @keyframes fadeIn {
            from { opacity: 0; transform: translateY(16px); }
            to { opacity: 1; transform: translateY(0); }
        }

        .header {
            background: linear-gradient(135deg, #4f46e5 0%, #7c3aed 50%, #ec4899 100%);
            padding: 32px 36px;
            color: #ffffff;
            position: relative;
        }

        .header h1 {
            font-size: 1.75rem;
            font-weight: 700;
            letter-spacing: -0.02em;
            display: flex;
            align-items: center;
            gap: 12px;
        }

        .header p {
            margin-top: 8px;
            font-size: 0.95rem;
            color: rgba(255, 255, 255, 0.85);
        }

        .form-content {
            padding: 36px;
        }

        .alert-error {
            background-color: var(--danger-bg);
            border: 1px solid var(--danger-border);
            color: var(--danger-text);
            padding: 14px 18px;
            border-radius: 12px;
            font-size: 0.92rem;
            font-weight: 500;
            margin-bottom: 24px;
            display: flex;
            align-items: center;
            gap: 10px;
        }

        .form-group {
            margin-bottom: 22px;
        }

        label {
            display: block;
            font-size: 0.9rem;
            font-weight: 600;
            color: #334155;
            margin-bottom: 8px;
        }

        .required-star {
            color: #ef4444;
        }

        .form-control {
            width: 100%;
            padding: 12px 16px;
            border: 1.5px solid var(--border-color);
            border-radius: 12px;
            font-size: 0.95rem;
            background-color: #ffffff;
            color: #1e293b;
            transition: all 0.2s ease;
        }

        .form-control:focus {
            outline: none;
            border-color: var(--primary);
            box-shadow: 0 0 0 4px rgba(99, 102, 241, 0.15);
        }

        .genre-chips-container {
            display: flex;
            flex-wrap: wrap;
            gap: 8px;
            margin-bottom: 12px;
        }

        .genre-chip-label {
            display: inline-flex;
            align-items: center;
            padding: 7px 14px;
            background: #f1f5f9;
            border: 1.5px solid #e2e8f0;
            border-radius: 50px;
            font-size: 0.85rem;
            font-weight: 500;
            color: #475569;
            cursor: pointer;
            transition: all 0.2s ease;
            user-select: none;
        }

        .genre-chip-label:hover {
            border-color: var(--primary);
            background: var(--primary-light);
            color: var(--primary);
        }

        .genre-chip-label input[type="checkbox"] {
            margin-right: 6px;
            accent-color: var(--primary);
        }

        .genre-chip-label:has(input[type="checkbox"]:checked) {
            background: var(--primary);
            border-color: var(--primary);
            color: #ffffff;
        }

        .file-upload-box {
            border: 2px dashed #cbd5e1;
            border-radius: 14px;
            padding: 24px;
            text-align: center;
            background-color: #f8fafc;
            cursor: pointer;
            transition: all 0.2s ease;
        }

        .file-upload-box:hover {
            border-color: var(--primary);
            background-color: #f5f7ff;
        }

        .file-upload-box input[type="file"] {
            width: 100%;
            cursor: pointer;
        }

        .file-hint {
            margin-top: 8px;
            font-size: 0.82rem;
            color: var(--text-muted);
        }

        .file-hint strong {
            color: #4338ca;
        }

        .actions {
            display: flex;
            align-items: center;
            justify-content: space-between;
            margin-top: 32px;
            gap: 16px;
        }

        .btn-submit {
            flex: 1;
            background: linear-gradient(135deg, #4f46e5, #7c3aed);
            color: white;
            border: none;
            padding: 14px 28px;
            border-radius: 12px;
            font-size: 1rem;
            font-weight: 600;
            cursor: pointer;
            transition: all 0.2s;
            box-shadow: 0 4px 14px rgba(99, 102, 241, 0.35);
        }

        .btn-submit:hover {
            transform: translateY(-1px);
            box-shadow: 0 6px 20px rgba(99, 102, 241, 0.45);
        }

        .btn-cancel {
            padding: 14px 22px;
            color: #64748b;
            text-decoration: none;
            font-size: 0.95rem;
            font-weight: 600;
            border-radius: 12px;
            transition: all 0.2s;
            border: 1.5px solid #cbd5e1;
        }

        .btn-cancel:hover {
            background-color: #f1f5f9;
            color: #334155;
        }
    </style>
</head>
<body>

<div class="container">
    <div class="header">
        <h1>
            <span>🎵</span> Tải Lên Bài Hát Mới
        </h1>
        <p>Thêm bài hát yêu thích của bạn vào thư viện âm nhạc</p>
    </div>

    <div class="form-content">
        <!-- Error Notification -->
        <c:if test="${not empty errorMessage}">
            <div class="alert-error">
                <span>⚠️</span>
                <span>${errorMessage}</span>
            </div>
        </c:if>

        <form:form action="${pageContext.request.contextPath}/songs/save"
                   method="post"
                   modelAttribute="songForm"
                   enctype="multipart/form-data">

            <!-- Song Name -->
            <div class="form-group">
                <label for="name">Tên bài hát <span class="required-star">*</span></label>
                <form:input path="name" id="name" cssClass="form-control" placeholder="Ví dụ: Nắng Ấm Xa Dần, See Tình..." required="required"/>
            </div>

            <!-- Artist -->
            <div class="form-group">
                <label for="artist">Nghệ sĩ thể hiện <span class="required-star">*</span></label>
                <form:input path="artist" id="artist" cssClass="form-control" placeholder="Ví dụ: Sơn Tùng M-TP, Hoàng Thùy Linh..." required="required"/>
            </div>

            <!-- Music Genres (List Selection & Custom Input) -->
            <div class="form-group">
                <label>Thể loại nhạc (chọn danh sách thể loại):</label>
                <div class="genre-chips-container">
                    <c:forEach var="genre" items="${availableGenres}">
                        <label class="genre-chip-label">
                            <input type="checkbox" name="genres" value="${genre}"/> ${genre}
                        </label>
                    </c:forEach>
                </div>
                <label for="customGenres" style="font-weight: normal; font-size: 0.85rem; color: var(--text-muted);">
                    Hoặc nhập thêm các thể loại khác (cách nhau bởi dấu phẩy):
                </label>
                <form:input path="customGenres" id="customGenres" cssClass="form-control" placeholder="Ví dụ: Lofi, Chill, Rap..."/>
            </div>

            <!-- Audio File Upload -->
            <div class="form-group">
                <label for="songFile">Chọn file bài hát <span class="required-star">*</span></label>
                <div class="file-upload-box">
                    <input type="file" id="songFile" name="songFile" accept=".mp3,.wav,.ogg,.m4p" required="required"/>
                    <div class="file-hint">
                        Chỉ chấp nhận các định dạng âm thanh: <strong>.mp3, .wav, .ogg, .m4p</strong>
                    </div>
                </div>
            </div>

            <!-- Actions -->
            <div class="actions">
                <a href="${pageContext.request.contextPath}/songs" class="btn-cancel">Danh sách bài hát</a>
                <button type="submit" class="btn-submit">🚀 Tải lên bài hát</button>
            </div>

        </form:form>
    </div>
</div>

</body>
</html>
