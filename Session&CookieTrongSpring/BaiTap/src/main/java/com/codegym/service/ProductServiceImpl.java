package com.codegym.service;

import com.codegym.model.Product;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ProductServiceImpl implements ProductService {
    private static final List<Product> products = new ArrayList<>();

    static {
        products.add(new Product(
                1L,
                "iPhone 15 Pro Max 256GB",
                29990000.0,
                "Khung viền Titan chuẩn hàng không vũ trụ, chip A17 Pro đột phá hiệu năng đồ họa, camera tiềm vọng zoom 5x sắc nét và cổng sạc USB-C tốc độ cao.",
                "https://images.unsplash.com/photo-1695048133142-1a20484d2569?w=600&auto=format&fit=crop&q=80"
        ));
        products.add(new Product(
                2L,
                "MacBook Pro 14 M3 Pro",
                49990000.0,
                "Trang bị vi xử lý Apple M3 Pro 11-core CPU và 14-core GPU, màn hình Liquid Retina XDR 120Hz siêu sáng, thời lượng pin lên đến 22 giờ liên tục.",
                "https://images.unsplash.com/photo-1517336714731-489689fd1ca8?w=600&auto=format&fit=crop&q=80"
        ));
        products.add(new Product(
                3L,
                "Tai nghe Sony WH-1000XM5",
                6990000.0,
                "Công nghệ chống ồn chủ động đỉnh cao với 2 bộ xử lý âm thanh, hỗ trợ Hi-Res Audio Wireless LDAC, micro thu âm trong trẻo với AI lọc ồn thông minh.",
                "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=600&auto=format&fit=crop&q=80"
        ));
        products.add(new Product(
                4L,
                "iPad Pro 11 M4 256GB",
                24490000.0,
                "Màn hình Ultra Retina XDR công nghệ Tandem OLED đột phá, siêu mỏng nhẹ chỉ 5.3mm, chip Apple M4 thế hệ mới sức mạnh xử lý AI vượt bậc.",
                "https://images.unsplash.com/photo-1544244015-0df4b3ffc6b0?w=600&auto=format&fit=crop&q=80"
        ));
        products.add(new Product(
                5L,
                "Apple Watch Ultra 2",
                18990000.0,
                "Vỏ Titan 49mm siêu bền bỉ, màn hình đạt độ sáng kỷ lục 3000 nits, chuẩn chống nước 100m, hỗ trợ GPS băng tần kép chính xác cho vận động viên.",
                "https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=600&auto=format&fit=crop&q=80"
        ));
        products.add(new Product(
                6L,
                "Loa Marshall Stanmore III",
                8990000.0,
                "Âm thanh nổi vang dội đặc trưng của Marshall, thiết kế cổ điển sang trọng đậm chất Rock 'n' Roll, kết nối Bluetooth 5.2 và cổng AUX 3.5mm.",
                "https://images.unsplash.com/photo-1545454675-3531b543be5d?w=600&auto=format&fit=crop&q=80"
        ));
    }

    @Override
    public List<Product> findAll() {
        return products;
    }

    @Override
    public Optional<Product> findById(Long id) {
        return products.stream()
                .filter(p -> p.getId().equals(id))
                .findFirst();
    }
}
