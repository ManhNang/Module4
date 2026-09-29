package com.codegym.customermanagementjpa.controller;

import com.codegym.customermanagementjpa.model.Blog;
import com.codegym.customermanagementjpa.service.IBlogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping({"/", "/blogs"})
public class BlogController {

    private final IBlogService blogService;

    @Autowired
    public BlogController(IBlogService blogService) {
        this.blogService = blogService;
    }

    // 1. Hiển thị danh sách tóm tắt các blog
    @GetMapping
    public String listBlogs(Model model) {
        List<Blog> blogs = blogService.findAll();
        model.addAttribute("blogs", blogs);
        return "list";
    }

    // 2. Viết một bài blog mới - Giao diện
    @GetMapping("/create")
    public String showCreateForm(Model model) {
        model.addAttribute("blog", new Blog());
        return "create";
    }

    // Viết một bài blog mới - Xử lý lưu
    @PostMapping("/save")
    public String saveBlog(@ModelAttribute("blog") Blog blog, RedirectAttributes redirectAttributes) {
        blogService.save(blog);
        redirectAttributes.addFlashAttribute("message", "Tạo bài viết mới thành công!");
        return "redirect:/blogs";
    }

    // 3. Xem nội dung chi tiết một blog
    @GetMapping("/{id}/view")
    public String viewBlog(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
        Optional<Blog> blogOptional = blogService.findById(id);
        if (blogOptional.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy bài viết!");
            return "redirect:/blogs";
        }
        model.addAttribute("blog", blogOptional.get());
        return "view";
    }

    // 4. Cập nhật nội dung một blog - Giao diện
    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
        Optional<Blog> blogOptional = blogService.findById(id);
        if (blogOptional.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy bài viết!");
            return "redirect:/blogs";
        }
        model.addAttribute("blog", blogOptional.get());
        return "edit";
    }

    // Cập nhật nội dung một blog - Xử lý cập nhật
    @PostMapping("/update")
    public String updateBlog(@ModelAttribute("blog") Blog blog, RedirectAttributes redirectAttributes) {
        Optional<Blog> existingOptional = blogService.findById(blog.getId());
        if (existingOptional.isPresent()) {
            Blog existing = existingOptional.get();
            blog.setCreatedAt(existing.getCreatedAt());
        }
        blogService.save(blog);
        redirectAttributes.addFlashAttribute("message", "Cập nhật bài viết thành công!");
        return "redirect:/blogs";
    }

    // 5. Xóa một blog - Giao diện xác nhận
    @GetMapping("/{id}/delete")
    public String showDeleteConfirm(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
        Optional<Blog> blogOptional = blogService.findById(id);
        if (blogOptional.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy bài viết!");
            return "redirect:/blogs";
        }
        model.addAttribute("blog", blogOptional.get());
        return "delete";
    }

    // Xóa một blog - Xử lý xóa
    @PostMapping("/{id}/delete")
    public String deleteBlog(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        blogService.remove(id);
        redirectAttributes.addFlashAttribute("message", "Đã xóa bài viết thành công!");
        return "redirect:/blogs";
    }
}
