package com.codegym.customermanagementjpa.controller;

import com.codegym.customermanagementjpa.model.Blog;
import com.codegym.customermanagementjpa.model.Category;
import com.codegym.customermanagementjpa.service.IBlogService;
import com.codegym.customermanagementjpa.service.ICategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping({"/", "/blogs"})
public class BlogController {

    private final IBlogService blogService;
    private final ICategoryService categoryService;

    @Autowired
    public BlogController(IBlogService blogService, ICategoryService categoryService) {
        this.blogService = blogService;
        this.categoryService = categoryService;
    }

    @ModelAttribute("categories")
    public List<Category> populateCategories() {
        return categoryService.findAll();
    }

    // 1. Hiển thị danh sách bài viết với Phân trang (Pageable), Sắp xếp (Sort) theo thời gian tạo, và Tìm kiếm (Search)
    @GetMapping
    public String listBlogs(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            @RequestParam(value = "sort", defaultValue = "createdAt,desc") String sortParam,
            @PageableDefault(size = 6) Pageable pageable,
            Model model) {

        // Sắp xếp theo thời gian được tạo (mặc định createdAt giảm dần - bài mới nhất lên đầu)
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        if ("createdAt,asc".equalsIgnoreCase(sortParam)) {
            sort = Sort.by(Sort.Direction.ASC, "createdAt");
        } else if ("title,asc".equalsIgnoreCase(sortParam)) {
            sort = Sort.by(Sort.Direction.ASC, "title");
        } else if ("title,desc".equalsIgnoreCase(sortParam)) {
            sort = Sort.by(Sort.Direction.DESC, "title");
        }

        Pageable sortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);

        Page<Blog> blogsPage;
        Category selectedCategory = null;

        if (categoryId != null) {
            Optional<Category> categoryOptional = categoryService.findById(categoryId);
            if (categoryOptional.isPresent()) {
                selectedCategory = categoryOptional.get();
                if (search != null && !search.trim().isEmpty()) {
                    blogsPage = blogService.findAllByTitleAndCategory(search.trim(), selectedCategory, sortedPageable);
                } else {
                    blogsPage = blogService.findAllByCategory(selectedCategory, sortedPageable);
                }
            } else {
                if (search != null && !search.trim().isEmpty()) {
                    blogsPage = blogService.findAllByTitle(search.trim(), sortedPageable);
                } else {
                    blogsPage = blogService.findAll(sortedPageable);
                }
            }
        } else {
            if (search != null && !search.trim().isEmpty()) {
                blogsPage = blogService.findAllByTitle(search.trim(), sortedPageable);
            } else {
                blogsPage = blogService.findAll(sortedPageable);
            }
        }

        model.addAttribute("blogsPage", blogsPage);
        model.addAttribute("blogs", blogsPage.getContent());
        model.addAttribute("search", search != null ? search.trim() : null);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("selectedCategory", selectedCategory);
        model.addAttribute("sort", sortParam);

        return "list";
    }

    // 2. Viết một bài blog mới - Giao diện có chọn danh mục
    @GetMapping("/create")
    public String showCreateForm(Model model) {
        model.addAttribute("blog", new Blog());
        return "create";
    }

    // Viết một bài blog mới - Xử lý lưu
    @PostMapping("/save")
    public String saveBlog(@ModelAttribute("blog") Blog blog, RedirectAttributes redirectAttributes) {
        if (blog.getCategory() != null && blog.getCategory().getId() != null) {
            categoryService.findById(blog.getCategory().getId()).ifPresent(blog::setCategory);
        } else {
            blog.setCategory(null);
        }

        blogService.save(blog);
        redirectAttributes.addFlashAttribute("message", "Tạo bài viết mới thành công!");
        return "redirect:/blogs";
    }

    // 3. Xem nội dung chi tiết một blog (Hiển thị đầy đủ tên danh mục)
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

    // 4. Cập nhật nội dung một blog - Giao diện có chọn danh mục
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

        if (blog.getCategory() != null && blog.getCategory().getId() != null) {
            categoryService.findById(blog.getCategory().getId()).ifPresent(blog::setCategory);
        } else {
            blog.setCategory(null);
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
