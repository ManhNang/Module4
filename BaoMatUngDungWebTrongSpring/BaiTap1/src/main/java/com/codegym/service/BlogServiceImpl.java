package com.codegym.service;

import com.codegym.model.Blog;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class BlogServiceImpl implements IBlogService {

    private static final List<Blog> blogList = Collections.synchronizedList(new ArrayList<>());
    private static final AtomicLong idGenerator = new AtomicLong(1);

    static {
        blogList.add(new Blog(
                idGenerator.getAndIncrement(),
                "Giới thiệu Spring Security 6",
                "Tìm hiểu các tính năng và cấu hình mới nhất trong Spring Security 6 trên nền tảng Spring 6.",
                "Spring Security 6 yêu cầu tối thiểu Java 17, Servlet 6.0 (Tomcat 10.1+) và loại bỏ hoàn toàn WebSecurityConfigurerAdapter để chuyển sang cơ chế component-based với SecurityFilterChain Bean.",
                "admin"
        ));
        blogList.add(new Blog(
                idGenerator.getAndIncrement(),
                "Bảo mật ứng dụng Web trong Spring MVC",
                "Cách phân quyền và kiểm soát truy cập cho người dùng đã xác thực và khách vãng lai.",
                "Người dùng chưa đăng nhập chỉ có quyền đọc danh sách bài viết blog và chi tiết bài viết. Chỉ những người dùng đã xác thực thành công qua Form Login mới có quyền tạo và đăng tải các bài viết mới.",
                "user"
        ));
    }

    @Override
    public List<Blog> findAll() {
        synchronized (blogList) {
            return new ArrayList<>(blogList);
        }
    }

    @Override
    public Optional<Blog> findById(Long id) {
        synchronized (blogList) {
            return blogList.stream()
                    .filter(b -> b.getId().equals(id))
                    .findFirst();
        }
    }

    @Override
    public void save(Blog blog) {
        if (blog.getId() == null) {
            blog.setId(idGenerator.getAndIncrement());
            blogList.add(0, blog); // Thêm lên đầu danh sách
        } else {
            synchronized (blogList) {
                for (int i = 0; i < blogList.size(); i++) {
                    if (blogList.get(i).getId().equals(blog.getId())) {
                        blogList.set(i, blog);
                        return;
                    }
                }
            }
        }
    }

    @Override
    public void delete(Long id) {
        blogList.removeIf(b -> b.getId().equals(id));
    }
}

