package com.pokemon.blindbox.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;

/**
 * Phục vụ trực tiếp các file ảnh PNG/WebP do Admin tải lên từ thư mục ./uploads/
 * tại đường dẫn /uploads/** ngay lập tức mà không cần khởi động lại ứng dụng.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploadDir = Paths.get("uploads").toAbsolutePath().normalize();
        String fileLocation = uploadDir.toUri().toString();
        if (!fileLocation.endsWith("/")) {
            fileLocation += "/";
        }
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(fileLocation, "classpath:/static/uploads/")
                .setCacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic());
    }
}
