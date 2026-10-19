const API_URL = "http://localhost:8080/phone-management-ajax-web-service/api/smartphones";

$(document).ready(function () {
    // Tải danh sách điện thoại ngay khi trang tải xong
    getSmartphones();
});

// 1. Hiển thị danh sách điện thoại (GET)
function getSmartphones() {
    $.ajax({
        type: "GET",
        url: API_URL,
        success: function (data) {
            let content = "";
            for (let i = 0; i < data.length; i++) {
                content += getSmartphoneRow(data[i]);
            }
            $("#smartphone-body").html(content);
        },
        error: function (error) {
            console.error("Lỗi khi tải danh sách:", error);
        }
    });
}

function getSmartphoneRow(phone) {
    return `<tr>
        <td>${phone.id}</td>
        <td>${phone.producer}</td>
        <td>${phone.model}</td>
        <td>$${phone.price}</td>
        <td>
            <button class="btn-edit" onclick="showEditForm(${phone.id})">Sửa</button>
            <button class="btn-delete" onclick="deleteSmartphone(${phone.id})">Xóa</button>
        </td>
    </tr>`;
}

// 2. Thêm mới hoặc Cập nhật điện thoại (POST / PUT)
function saveSmartphone() {
    let id = $('#phone-id').val();
    let producer = $('#producer').val();
    let model = $('#model').val();
    let price = $('#price').val();

    let newSmartphone = {
        producer: producer,
        model: model,
        price: price
    };

    let httpMethod = id ? "PUT" : "POST";
    let requestUrl = id ? `${API_URL}/${id}` : API_URL;

    $.ajax({
        headers: {
            'Accept': 'application/json',
            'Content-Type': 'application/json'
        },
        type: httpMethod,
        data: JSON.stringify(newSmartphone),
        url: requestUrl,
        success: function () {
            getSmartphones();
            resetForm();
        },
        error: function (error) {
            console.error("Lỗi khi lưu dữ liệu:", error);
        }
    });
}

// 3. Xóa điện thoại (DELETE)
function deleteSmartphone(id) {
    if (confirm("Bạn có chắc chắn muốn xóa điện thoại này?")) {
        $.ajax({
            type: "DELETE",
            url: `${API_URL}/${id}`,
            success: function () {
                getSmartphones();
            },
            error: function (error) {
                console.error("Lỗi khi xóa:", error);
            }
        });
    }
}

// 4. Hiển thị form chỉnh sửa
function showEditForm(id) {
    $.ajax({
        type: "GET",
        url: `${API_URL}/${id}`,
        success: function (data) {
            $('#phone-id').val(data.id);
            $('#producer').val(data.producer);
            $('#model').val(data.model);
            $('#price').val(data.price);
            $('#btn-cancel').show();
        }
    });
}

// Reset form
function resetForm() {
    $('#phone-id').val('');
    $('#producer').val('');
    $('#model').val('');
    $('#price').val('');
    $('#btn-cancel').hide();
}