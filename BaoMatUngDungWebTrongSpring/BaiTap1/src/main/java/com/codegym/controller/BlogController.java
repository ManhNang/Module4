package com.codegym.controller;

import com.codegym.model.Blog;
import com.codegym.service.IBlogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.Optional;

@Controller
@RequestMapping
public class BlogController {

    private final IBlogService blogService;

    @Autowired
    public BlogController(IBlogService blogService) {
        this.blogService = blogService;
    }

    // Xem danh sách bài viết (Công khai - Cho phép cả khách vãng lai và người đã đăng nhập)
    @GetMapping({"/", "/blogs"})
    public String listBlogs(Model model) {
        model.addAttribute("blogs", blogService.findAll());
        return "blogs/list";
    }

    // Xem chi tiết bài viết (Công khai)
    @GetMapping("/blogs/{id}")
    public String viewBlog(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
        Optional<Blog> blogOptional = blogService.findById(id);
        if (blogOptional.isPresent()) {
            model.addAttribute("blog", blogOptional.get());
            return "blogs/detail";
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy bài viết!");
            return "redirect:/blogs";
        }
    }

    // Hiển thị form tạo bài viết mới (Chỉ dành cho người dùng đã đăng nhập)
    @GetMapping("/blogs/create")
    public String showCreateForm(Model model, Principal principal) {
        Blog blog = new Blog();
        if (principal != null) {
            blog.setAuthor(principal.getName());
        }
        model.addAttribute("blog", blog);
        return "blogs/create";
    }

    // Lưu bài viết mới (Chỉ dành cho người dùng đã đăng nhập)
    @PostMapping("/blogs/save")
    public String saveBlog(@ModelAttribute("blog") Blog blog, Principal principal, RedirectAttributes redirectAttributes) {
        if (principal != null) {
            blog.setAuthor(principal.getName());
        }
        blogService.save(blog);
        redirectAttributes.addFlashAttribute("successMessage", "Đăng bài viết mới thành công!");
        return "redirect:/blogs";
    }
}

