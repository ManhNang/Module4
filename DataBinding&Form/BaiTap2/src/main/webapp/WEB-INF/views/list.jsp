<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Thư Viện Bài Hát - SoundWave</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700&display=swap" rel="stylesheet">
    <style>
        :root {
            --primary: #6366f1;
            --primary-hover: #4f46e5;
            --bg-gradient: linear-gradient(135deg, #0f172a 0%, #1e1b4b 50%, #311042 100%);
            --card-bg: rgba(255, 255, 255, 0.98);
            --text-dark: #0f172a;
            --text-muted: #64748b;
            --border-color: #e2e8f0;
            --success-bg: #ecfdf5;
            --success-border: #a7f3d0;
            --success-text: #065f46;
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
            padding: 40px 24px;
            color: var(--text-dark);
            display: flex;
            justify-content: center;
        }

        .container {
            width: 100%;
            max-width: 1080px;
            background: var(--card-bg);
            border-radius: 24px;
            box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.4);
            overflow: hidden;
            border: 1px solid rgba(255, 255, 255, 0.2);
            animation: fadeIn 0.4s ease-out;
            height: fit-content;
        }

        @keyframes fadeIn {
            from { opacity: 0; transform: translateY(16px); }
            to { opacity: 1; transform: translateY(0); }
        }

        .header {
            background: linear-gradient(135deg, #4f46e5 0%, #7c3aed 50%, #ec4899 100%);
            padding: 32px 40px;
            color: #ffffff;
            display: flex;
            justify-content: space-between;
            align-items: center;
            flex-wrap: wrap;
            gap: 20px;
        }

        .header-title h1 {
            font-size: 1.85rem;
            font-weight: 700;
            display: flex;
            align-items: center;
            gap: 12px;
        }

        .header-title p {
            margin-top: 6px;
            font-size: 0.95rem;
            color: rgba(255, 255, 255, 0.85);
        }

        .btn-add {
            background: #ffffff;
            color: #4f46e5;
            padding: 12px 24px;
            border-radius: 50px;
            text-decoration: none;
            font-weight: 700;
            font-size: 0.95rem;
            display: inline-flex;
            align-items: center;
            gap: 8px;
            box-shadow: 0 4px 14px rgba(0, 0, 0, 0.15);
            transition: all 0.2s;
        }

        .btn-add:hover {
            transform: translateY(-2px);
            box-shadow: 0 6px 20px rgba(0, 0, 0, 0.25);
            background: #f8fafc;
        }

        .content {
            padding: 36px 40px;
        }

        .alert-success {
            background-color: var(--success-bg);
            border: 1px solid var(--success-border);
            color: var(--success-text);
            padding: 14px 20px;
            border-radius: 12px;
            font-size: 0.95rem;
            font-weight: 500;
            margin-bottom: 24px;
            display: flex;
            align-items: center;
            gap: 10px;
        }

        .table-responsive {
            overflow-x: auto;
            border-radius: 16px;
            border: 1px solid var(--border-color);
        }

        table {
            width: 100%;
            border-collapse: collapse;
            text-align: left;
        }

        thead {
            background: #f8fafc;
            border-bottom: 2px solid var(--border-color);
        }

        th {
            padding: 16px 20px;
            font-size: 0.85rem;
            font-weight: 700;
            text-transform: uppercase;
            letter-spacing: 0.05em;
            color: #475569;
        }

        td {
            padding: 18px 20px;
            font-size: 0.95rem;
            border-bottom: 1px solid var(--border-color);
            vertical-align: middle;
        }

        tbody tr:hover {
            background-color: #f8faff;
        }

        .song-name {
            font-weight: 700;
            color: #1e293b;
            font-size: 1.05rem;
        }

        .song-artist {
            color: #64748b;
            font-weight: 500;
        }

        .genre-badge-list {
            display: flex;
            flex-wrap: wrap;
            gap: 6px;
        }

        .genre-badge {
            background: #eff6ff;
            color: #2563eb;
            border: 1px solid #bfdbfe;
            padding: 4px 10px;
            border-radius: 20px;
            font-size: 0.78rem;
            font-weight: 600;
        }

        .file-info {
            font-family: monospace;
            font-size: 0.85rem;
            color: #475569;
            background: #f1f5f9;
            padding: 4px 8px;
            border-radius: 6px;
            display: inline-block;
            max-width: 200px;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
        }

        .audio-player {
            max-width: 240px;
            height: 36px;
        }

        .empty-state {
            text-align: center;
            padding: 60px 20px;
            color: var(--text-muted);
        }

        .empty-state-icon {
            font-size: 3rem;
            margin-bottom: 16px;
        }
    </style>
</head>
<body>

<div class="container">
    <div class="header">
        <div class="header-title">
            <h1><span>🎧</span> Thư Viện Bài Hát Đã Tải Lên</h1>
            <p>Danh sách các bài hát và tệp âm thanh trong hệ thống</p>
        </div>
        <a href="${pageContext.request.contextPath}/songs/create" class="btn-add">
            <span>➕</span> Tải lên bài hát mới
        </a>
    </div>

    <div class="content">
        <!-- Flash message on successful upload -->
        <c:if test="${not empty successMessage}">
            <div class="alert-success">
                <span>🎉</span>
                <span>${successMessage}</span>
            </div>
        </c:if>

        <c:choose>
            <c:when test="${empty songs}">
                <div class="empty-state">
                    <div class="empty-state-icon">🎼</div>
                    <h3>Chưa có bài hát nào được tải lên</h3>
                    <p style="margin-top: 8px;">Hãy bấm "Tải lên bài hát mới" để bắt đầu bộ sưu tập âm nhạc của bạn!</p>
                </div>
            </c:when>
            <c:otherwise>
                <div class="table-responsive">
                    <table>
                        <thead>
                        <tr>
                            <th style="width: 60px;">#</th>
                            <th>Tên bài hát</th>
                            <th>Nghệ sĩ</th>
                            <th>Thể loại nhạc</th>
                            <th>Tên file bài hát</th>
                            <th>Trình phát</th>
                        </tr>
                        </thead>
                        <tbody>
                        <c:forEach var="song" items="${songs}" varStatus="loop">
                            <tr>
                                <td><strong>${loop.index + 1}</strong></td>
                                <td>
                                    <div class="song-name">🎵 ${song.name}</div>
                                </td>
                                <td>
                                    <div class="song-artist">👤 ${song.artist}</div>
                                </td>
                                <td>
                                    <div class="genre-badge-list">
                                        <c:forEach var="genre" items="${song.genres}">
                                            <span class="genre-badge">${genre}</span>
                                        </c:forEach>
                                    </div>
                                </td>
                                <td>
                                    <span class="file-info" title="${song.filePath}">${song.filePath}</span>
                                </td>
                                <td>
                                    <audio controls class="audio-player">
                                        <source src="${pageContext.request.contextPath}/uploads/${song.filePath}">
                                        Trình duyệt của bạn không hỗ trợ phát audio.
                                    </audio>
                                </td>
                            </tr>
                        </c:forEach>
                        </tbody>
                    </table>
                </div>
            </c:otherwise>
        </c:choose>
    </div>
</div>

</body>
</html>
