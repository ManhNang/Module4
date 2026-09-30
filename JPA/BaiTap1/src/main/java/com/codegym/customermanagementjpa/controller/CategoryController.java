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
@RequestMapping("/categories")
public class CategoryController {

    private final ICategoryService categoryService;
    private final IBlogService blogService;

    @Autowired
    public CategoryController(ICategoryService categoryService, IBlogService blogService) {
        this.categoryService = categoryService;
        this.blogService = blogService;
    }

    @ModelAttribute("categories")
    public List<Category> populateCategories() {
        return categoryService.findAll();
    }

    // 1. Xem danh sách danh mục
    @GetMapping
    public String listCategories(Model model) {
        List<Category> categories = categoryService.findAll();
        model.addAttribute("categoryList", categories);
        return "category/list";
    }

    // 2. Tạo danh mục mới - Giao diện
    @GetMapping("/create")
    public String showCreateForm(Model model) {
        model.addAttribute("category", new Category());
        return "category/create";
    }

    // Tạo danh mục mới - Xử lý lưu
    @PostMapping("/save")
    public String saveCategory(@ModelAttribute("category") Category category, RedirectAttributes redirectAttributes) {
        if (category.getName() == null || category.getName().trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Tên danh mục không được để trống!");
            return "redirect:/categories/create";
        }
        categoryService.save(category);
        redirectAttributes.addFlashAttribute("message", "Thêm danh mục mới thành công!");
        return "redirect:/categories";
    }

    // 3. Chỉnh sửa danh mục - Giao diện
    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
        Optional<Category> categoryOptional = categoryService.findById(id);
        if (categoryOptional.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy danh mục!");
            return "redirect:/categories";
        }
        model.addAttribute("category", categoryOptional.get());
        return "category/edit";
    }

    // Chỉnh sửa danh mục - Xử lý cập nhật
    @PostMapping("/update")
    public String updateCategory(@ModelAttribute("category") Category category, RedirectAttributes redirectAttributes) {
        if (category.getName() == null || category.getName().trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Tên danh mục không được để trống!");
            return "redirect:/categories/" + category.getId() + "/edit";
        }
        categoryService.save(category);
        redirectAttributes.addFlashAttribute("message", "Cập nhật danh mục thành công!");
        return "redirect:/categories";
    }

    // 4. Xóa danh mục - Giao diện xác nhận
    @GetMapping("/{id}/delete")
    public String showDeleteForm(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
        Optional<Category> categoryOptional = categoryService.findById(id);
        if (categoryOptional.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy danh mục!");
            return "redirect:/categories";
        }
        model.addAttribute("category", categoryOptional.get());
        return "category/delete";
    }

    // Xóa danh mục - Xử lý xóa
    @PostMapping("/{id}/delete")
    public String deleteCategory(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        categoryService.remove(id);
        redirectAttributes.addFlashAttribute("message", "Đã xóa danh mục thành công!");
        return "redirect:/categories";
    }

    // 5. Hiển thị danh sách bài viết của một danh mục
    @GetMapping("/{id}/blogs")
    public String listCategoryBlogs(
            @PathVariable("id") Long id,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "sort", defaultValue = "createdAt,desc") String sortParam,
            @PageableDefault(size = 6) Pageable pageable,
            Model model,
            RedirectAttributes redirectAttributes) {

        Optional<Category> categoryOptional = categoryService.findById(id);
        if (categoryOptional.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy danh mục!");
            return "redirect:/categories";
        }

        Category category = categoryOptional.get();

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
        if (search != null && !search.trim().isEmpty()) {
            blogsPage = blogService.findAllByTitleAndCategory(search.trim(), category, sortedPageable);
            model.addAttribute("search", search.trim());
        } else {
            blogsPage = blogService.findAllByCategory(category, sortedPageable);
        }

        model.addAttribute("category", category);
        model.addAttribute("blogsPage", blogsPage);
        model.addAttribute("blogs", blogsPage.getContent());
        model.addAttribute("sort", sortParam);

        return "category/blogs";
    }
}
