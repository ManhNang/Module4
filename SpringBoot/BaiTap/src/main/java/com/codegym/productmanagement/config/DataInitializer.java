package com.codegym.productmanagement.config;

import com.codegym.productmanagement.model.Product;
import com.codegym.productmanagement.repository.IProductRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner initData(IProductRepository productRepository) {
        return args -> {
            if (productRepository.count() == 0) {
                productRepository.saveAll(List.of(
                        new Product("iPhone 15 Pro Max 256GB", 29990000.0, "Điện thoại cao cấp vỏ Titan, chip A17 Pro siêu mạnh mẽ", "Apple"),
                        new Product("Samsung Galaxy S24 Ultra", 27490000.0, "Camera 200MP, tích hợp AI thông minh, bút S-Pen", "Samsung"),
                        new Product("MacBook Pro 14 M3 Pro", 49990000.0, "Màn hình Liquid Retina XDR, hiệu năng chuyên nghiệp", "Apple"),
                        new Product("Tai nghe chống ồn Sony WH-1000XM5", 7990000.0, "Công nghệ chống ồn đỉnh cao, thời lượng pin 30 giờ", "Sony"),
                        new Product("Bàn phím cơ Logitech MX Mechanical", 3490000.0, "Switch cơ học xúc giác, gõ êm, kết nối đa thiết bị", "Logitech")
                ));
            }
        };
    }
}

