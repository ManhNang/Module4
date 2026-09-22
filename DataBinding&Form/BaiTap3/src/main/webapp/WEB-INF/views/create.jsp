<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Tờ Khai Y Tế - Bộ Y Tế Việt Nam</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet">
    <style>
        :root {
            --primary: #0284c7;
            --primary-dark: #0369a1;
            --primary-light: #e0f2fe;
            --success: #10b981;
            --danger: #ef4444;
            --danger-bg: #fef2f2;
            --danger-border: #fecaca;
            --gray-50: #f8fafc;
            --gray-100: #f1f5f9;
            --gray-200: #e2e8f0;
            --gray-300: #cbd5e1;
            --gray-600: #475569;
            --gray-700: #334155;
            --gray-800: #1e293b;
            --radius: 10px;
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
            max-width: 900px;
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
            padding: 30px;
            text-align: center;
        }

        .header h1 {
            font-size: 24px;
            font-weight: 700;
            text-transform: uppercase;
            letter-spacing: 0.5px;
            margin-bottom: 8px;
        }

        .header p {
            font-size: 14px;
            opacity: 0.9;
        }

        .nav-actions {
            display: flex;
            justify-content: flex-end;
            padding: 15px 30px 0;
        }

        .btn-link {
            display: inline-flex;
            align-items: center;
            gap: 6px;
            color: var(--primary);
            font-weight: 600;
            font-size: 14px;
            text-decoration: none;
            padding: 8px 14px;
            border-radius: 6px;
            background: var(--primary-light);
            transition: all 0.2s;
        }

        .btn-link:hover {
            background: #bae6fd;
            color: var(--primary-dark);
        }

        .form-content {
            padding: 30px;
        }

        /* Alert error banner */
        .alert-error {
            background-color: var(--danger-bg);
            border: 1px solid var(--danger-border);
            border-radius: var(--radius);
            padding: 16px 20px;
            margin-bottom: 25px;
            color: #991b1b;
        }

        .alert-error h4 {
            font-size: 15px;
            font-weight: 700;
            margin-bottom: 8px;
            display: flex;
            align-items: center;
            gap: 8px;
        }

        .alert-error ul {
            margin-left: 24px;
            font-size: 14px;
            line-height: 1.6;
        }

        .section-title {
            font-size: 17px;
            font-weight: 700;
            color: var(--primary-dark);
            margin: 25px 0 15px;
            padding-bottom: 8px;
            border-bottom: 2px solid var(--gray-100);
            display: flex;
            align-items: center;
            gap: 10px;
        }

        .section-title:first-of-type {
            margin-top: 0;
        }

        .badge-step {
            display: inline-flex;
            align-items: center;
            justify-content: center;
            width: 26px;
            height: 26px;
            background: var(--primary);
            color: white;
            border-radius: 50%;
            font-size: 13px;
        }

        .form-row {
            display: flex;
            flex-wrap: wrap;
            margin: 0 -10px 15px;
        }

        .form-group {
            padding: 0 10px;
            margin-bottom: 15px;
            flex: 1 1 100%;
        }

        .col-3 { flex: 1 1 33.333%; }
        .col-4 { flex: 1 1 50%; }
        .col-6 { flex: 1 1 50%; }
        .col-12 { flex: 1 1 100%; }

        @media (max-width: 768px) {
            .col-3, .col-4, .col-6 {
                flex: 1 1 100%;
            }
        }

        label {
            display: block;
            font-size: 13px;
            font-weight: 600;
            color: var(--gray-700);
            margin-bottom: 6px;
        }

        .required {
            color: var(--danger);
            font-weight: 700;
        }

        .form-control {
            width: 100%;
            height: 42px;
            padding: 8px 14px;
            font-size: 14px;
            font-family: inherit;
            border: 1px solid var(--gray-300);
            border-radius: 8px;
            background-color: #fff;
            color: var(--gray-800);
            transition: border-color 0.2s, box-shadow 0.2s;
        }

        .form-control:focus {
            outline: none;
            border-color: var(--primary);
            box-shadow: 0 0 0 3px rgba(2, 132, 199, 0.15);
        }

        .form-control.is-invalid {
            border-color: var(--danger);
            background-color: #fff5f5;
        }

        .field-error {
            color: var(--danger);
            font-size: 12px;
            margin-top: 5px;
            display: block;
            font-weight: 500;
        }

        .radio-group, .checkbox-group {
            display: flex;
            flex-wrap: wrap;
            gap: 16px;
            padding-top: 6px;
        }

        .custom-option {
            display: inline-flex;
            align-items: center;
            gap: 8px;
            font-size: 14px;
            color: var(--gray-700);
            cursor: pointer;
        }

        .custom-option input[type="radio"],
        .custom-option input[type="checkbox"] {
            width: 17px;
            height: 17px;
            accent-color: var(--primary);
            cursor: pointer;
        }

        .symptom-card {
            background: var(--gray-50);
            border: 1px solid var(--gray-200);
            border-radius: var(--radius);
            padding: 16px;
            margin-bottom: 15px;
        }

        .symptom-grid {
            display: grid;
            grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
            gap: 12px;
            margin-top: 10px;
        }

        .file-upload-box {
            border: 2px dashed var(--gray-300);
            border-radius: var(--radius);
            padding: 20px;
            text-align: center;
            background: var(--gray-50);
            cursor: pointer;
            transition: all 0.2s;
        }

        .file-upload-box:hover {
            border-color: var(--primary);
            background: var(--primary-light);
        }

        .file-upload-box input[type="file"] {
            display: block;
            width: 100%;
            font-size: 13px;
            color: var(--gray-600);
            margin-top: 8px;
        }

        .btn-submit {
            display: block;
            width: 100%;
            padding: 14px;
            font-size: 16px;
            font-weight: 700;
            color: white;
            background: linear-gradient(135deg, #0284c7 0%, #0369a1 100%);
            border: none;
            border-radius: 8px;
            cursor: pointer;
            box-shadow: 0 4px 6px -1px rgba(2, 132, 199, 0.3);
            transition: all 0.2s;
            margin-top: 20px;
        }

        .btn-submit:hover {
            background: linear-gradient(135deg, #0369a1 0%, #075985 100%);
            transform: translateY(-1px);
            box-shadow: 0 6px 12px -2px rgba(2, 132, 199, 0.4);
        }
    </style>
</head>
<body>

<div class="container">
    <div class="header">
        <h1>TỜ KHAI Y TẾ</h1>
        <p>HỆ THỐNG QUẢN LÝ TỜ KHAI Y TẾ PHÒNG CHỐNG DỊCH BỆNH</p>
    </div>

    <div class="nav-actions">
        <a href="${pageContext.request.contextPath}/declarations/list" class="btn-link">
            📋 Xem danh sách tờ khai đã gửi
        </a>
    </div>

    <div class="form-content">
        <!-- Thông báo lỗi khi người dùng chưa điền đầy đủ các mục bắt buộc -->
        <c:if test="${not empty missingFields}">
            <div class="alert-error" id="errorBanner">
                <h4>⚠️ Vui lòng điền đầy đủ các mục thông tin bắt buộc sau:</h4>
                <ul>
                    <c:forEach var="item" items="${missingFields}">
                        <li>${item}</li>
                    </c:forEach>
                </ul>
            </div>
        </c:if>

        <form:form modelAttribute="declarationForm" action="${pageContext.request.contextPath}/declarations/save" method="post" enctype="multipart/form-data">
            
            <!-- Phần 1: Thông tin cá nhân -->
            <div class="section-title">
                <span class="badge-step">1</span> Thông tin cá nhân
            </div>

            <div class="form-row">
                <div class="form-group col-12">
                    <label>Họ và tên (chữ in hoa) <span class="required">(*)</span></label>
                    <form:input path="fullName" cssClass="form-control ${errors['fullName'] != null ? 'is-invalid' : ''}" placeholder="VD: NGUYỄN VĂN A"/>
                    <c:if test="${errors['fullName'] != null}">
                        <span class="field-error">${errors['fullName']}</span>
                    </c:if>
                </div>
            </div>

            <div class="form-row">
                <div class="form-group col-4">
                    <label>Năm sinh <span class="required">(*)</span></label>
                    <form:select path="birthYear" cssClass="form-control ${errors['birthYear'] != null ? 'is-invalid' : ''}">
                        <form:option value="" label="-- Chọn năm sinh --"/>
                        <form:options items="${years}"/>
                    </form:select>
                    <c:if test="${errors['birthYear'] != null}">
                        <span class="field-error">${errors['birthYear']}</span>
                    </c:if>
                </div>

                <div class="form-group col-4">
                    <label>Giới tính <span class="required">(*)</span></label>
                    <form:select path="gender" cssClass="form-control ${errors['gender'] != null ? 'is-invalid' : ''}">
                        <form:options items="${genders}"/>
                    </form:select>
                    <c:if test="${errors['gender'] != null}">
                        <span class="field-error">${errors['gender']}</span>
                    </c:if>
                </div>

                <div class="form-group col-4">
                    <label>Quốc tịch <span class="required">(*)</span></label>
                    <form:select path="nationality" cssClass="form-control ${errors['nationality'] != null ? 'is-invalid' : ''}">
                        <form:options items="${nationalities}"/>
                    </form:select>
                    <c:if test="${errors['nationality'] != null}">
                        <span class="field-error">${errors['nationality']}</span>
                    </c:if>
                </div>
            </div>

            <div class="form-row">
                <div class="form-group col-12">
                    <label>Số Hộ chiếu / CMND / CCCD <span class="required">(*)</span></label>
                    <form:input path="idCard" cssClass="form-control ${errors['idCard'] != null ? 'is-invalid' : ''}" placeholder="Nhập số CCCD / CMND hoặc Hộ chiếu"/>
                    <c:if test="${errors['idCard'] != null}">
                        <span class="field-error">${errors['idCard']}</span>
                    </c:if>
                </div>
            </div>

            <!-- Phần 2: Thông tin đi lại -->
            <div class="section-title">
                <span class="badge-step">2</span> Thông tin đi lại
            </div>

            <div class="form-row">
                <div class="form-group col-12">
                    <label>Thông tin đi lại (Phương tiện di chuyển)</label>
                    <div class="radio-group">
                        <c:forEach var="v" items="${vehicles}">
                            <label class="custom-option">
                                <form:radiobutton path="travelInfo" value="${v}"/>
                                <span>${v}</span>
                            </label>
                        </c:forEach>
                    </div>
                </div>
            </div>

            <div class="form-row">
                <div class="form-group col-6">
                    <label>Số hiệu phương tiện (Chuyến bay / Biển số xe / Tàu)</label>
                    <form:input path="vehicleNumber" cssClass="form-control" placeholder="VD: VN-254, 29B-12345"/>
                </div>
                <div class="form-group col-6">
                    <label>Số ghế</label>
                    <form:input path="seatNumber" cssClass="form-control" placeholder="VD: 14B"/>
                </div>
            </div>

            <div class="form-row">
                <div class="form-group col-6">
                    <label>Ngày khởi hành</label>
                    <form:input path="departureDate" type="date" cssClass="form-control"/>
                </div>
                <div class="form-group col-6">
                    <label>Ngày kết thúc</label>
                    <form:input path="arrivalDate" type="date" cssClass="form-control"/>
                </div>
            </div>

            <div class="form-row">
                <div class="form-group col-12">
                    <label>Trong vòng 14 ngày qua, Anh/Chị có đến tỉnh / thành phố nào?</label>
                    <form:input path="visitedCities" cssClass="form-control" placeholder="Ghi rõ tên các tỉnh/thành phố, cách nhau bằng dấu phẩy"/>
                </div>
            </div>

            <!-- Phần 3: Địa chỉ liên lạc tại Việt Nam -->
            <div class="section-title">
                <span class="badge-step">3</span> Địa chỉ liên lạc tại Việt Nam
            </div>

            <div class="form-row">
                <div class="form-group col-4">
                    <label>Tỉnh / Thành phố <span class="required">(*)</span></label>
                    <form:select path="province" cssClass="form-control ${errors['province'] != null ? 'is-invalid' : ''}">
                        <form:option value="" label="-- Chọn Tỉnh / Thành phố --"/>
                        <form:options items="${provinces}"/>
                    </form:select>
                    <c:if test="${errors['province'] != null}">
                        <span class="field-error">${errors['province']}</span>
                    </c:if>
                </div>

                <div class="form-group col-4">
                    <label>Quận / Huyện <span class="required">(*)</span></label>
                    <form:input path="district" cssClass="form-control ${errors['district'] != null ? 'is-invalid' : ''}" placeholder="VD: Cầu Giấy, Quận 1"/>
                    <c:if test="${errors['district'] != null}">
                        <span class="field-error">${errors['district']}</span>
                    </c:if>
                </div>

                <div class="form-group col-4">
                    <label>Phường / Xã <span class="required">(*)</span></label>
                    <form:input path="ward" cssClass="form-control ${errors['ward'] != null ? 'is-invalid' : ''}" placeholder="VD: Dịch Vọng Hậu"/>
                    <c:if test="${errors['ward'] != null}">
                        <span class="field-error">${errors['ward']}</span>
                    </c:if>
                </div>
            </div>

            <div class="form-row">
                <div class="form-group col-12">
                    <label>Địa chỉ nơi ở (Số nhà, phố, tổ dân phố / thôn / xóm)</label>
                    <form:input path="addressDetail" cssClass="form-control" placeholder="Số nhà, đường phố, thôn xóm..."/>
                </div>
            </div>

            <div class="form-row">
                <div class="form-group col-6">
                    <label>Điện thoại liên hệ <span class="required">(*)</span></label>
                    <form:input path="phone" cssClass="form-control ${errors['phone'] != null ? 'is-invalid' : ''}" placeholder="VD: 0987654321"/>
                    <c:if test="${errors['phone'] != null}">
                        <span class="field-error">${errors['phone']}</span>
                    </c:if>
                </div>

                <div class="form-group col-6">
                    <label>Email</label>
                    <form:input path="email" type="email" cssClass="form-control" placeholder="VD: email@example.com"/>
                </div>
            </div>

            <!-- Phần 4: Triệu chứng trong vòng 14 ngày qua -->
            <div class="section-title">
                <span class="badge-step">4</span> Theo dõi triệu chứng & Phơi nhiễm
            </div>

            <div class="symptom-card">
                <label style="margin-bottom: 10px;">Trong vòng 14 ngày qua, Anh/Chị có thấy xuất hiện ít nhất 1 trong các dấu hiệu sau không?</label>
                <div class="symptom-grid">
                    <c:forEach var="symptom" items="${symptomOptions}">
                        <label class="custom-option">
                            <form:checkbox path="symptoms" value="${symptom}"/>
                            <span>${symptom}</span>
                        </label>
                    </c:forEach>
                </div>
            </div>

            <div class="symptom-card">
                <label style="margin-bottom: 10px;">Trong vòng 14 ngày qua, Anh/Chị có:</label>
                <div class="checkbox-group" style="flex-direction: column; gap: 10px;">
                    <c:forEach var="exposure" items="${exposureOptions}">
                        <label class="custom-option">
                            <form:checkbox path="exposures" value="${exposure}"/>
                            <span>${exposure}</span>
                        </label>
                    </c:forEach>
                </div>
            </div>

            <!-- Phần 5: Đính kèm tài liệu y tế (Upload file) -->
            <div class="section-title">
                <span class="badge-step">5</span> Đính kèm tài liệu y tế (Tải lên ảnh / file)
            </div>

            <div class="file-upload-box">
                <p style="font-weight: 600; color: var(--primary);">📎 Tải lên ảnh CCCD / Giấy chứng nhận tiêm chủng / Kết quả xét nghiệm</p>
                <input type="file" name="documentFile" accept="image/*,.pdf,.doc,.docx"/>
                <p style="font-size: 12px; color: var(--gray-600); margin-top: 6px;">Hỗ trợ ảnh JPG, PNG hoặc tài liệu PDF/DOC (Tối đa 10MB)</p>
            </div>

            <button type="submit" class="btn-submit">
                GỬI TỜ KHAI Y TẾ
            </button>
        </form:form>
    </div>
</div>

</body>
</html>
