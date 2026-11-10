package com.codegym.productmanagement.controller;

import com.codegym.productmanagement.model.Product;
import com.codegym.productmanagement.service.IProductService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/products")
public class ProductController {

    private final IProductService productService;

    public ProductController(IProductService productService) {
        this.productService = productService;
    }

    // 1. Hiển thị danh sách sản phẩm (có hỗ trợ tìm kiếm theo tên)
    @GetMapping
    public String listProducts(@RequestParam(value = "search", required = false) String search, Model model) {
        List<Product> products;
        if (search != null && !search.trim().isEmpty()) {
            products = productService.searchByName(search);
            model.addAttribute("search", search);
        } else {
            products = productService.findAll();
        }
        model.addAttribute("products", products);
        return "product/index";
    }

    // 2. Chức năng thêm sản phẩm: Hiển thị form tạo mới
    @GetMapping("/create")
    public String showCreateForm(Model model) {
        model.addAttribute("product", new Product());
        return "product/create";
    }

    // Xử lý lưu sản phẩm mới
    @PostMapping("/save")
    public String saveProduct(@Valid @ModelAttribute("product") Product product,
                              BindingResult bindingResult,
                              RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "product/create";
        }
        productService.save(product);
        redirectAttributes.addFlashAttribute("successMessage", "Thêm mới sản phẩm thành công!");
        return "redirect:/products";
    }

    // 3. Chức năng cập nhật: Hiển thị form sửa sản phẩm
    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
        Optional<Product> productOptional = productService.findById(id);
        if (productOptional.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy sản phẩm có ID: " + id);
            return "redirect:/products";
        }
        model.addAttribute("product", productOptional.get());
        return "product/edit";
    }

    // Xử lý cập nhật sản phẩm
    @PostMapping("/update")
    public String updateProduct(@Valid @ModelAttribute("product") Product product,
                                BindingResult bindingResult,
                                RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "product/edit";
        }
        productService.save(product);
        redirectAttributes.addFlashAttribute("successMessage", "Cập nhật thông tin sản phẩm thành công!");
        return "redirect:/products";
    }

    // 4. Chức năng xoá: Hiển thị trang xác nhận xoá
    @GetMapping("/{id}/delete")
    public String showDeleteForm(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
        Optional<Product> productOptional = productService.findById(id);
        if (productOptional.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy sản phẩm có ID: " + id);
            return "redirect:/products";
        }
        model.addAttribute("product", productOptional.get());
        return "product/delete";
    }

    // Xử lý xoá sản phẩm
    @PostMapping("/delete")
    public String deleteProduct(@RequestParam("id") Long id, RedirectAttributes redirectAttributes) {
        productService.remove(id);
        redirectAttributes.addFlashAttribute("successMessage", "Xoá sản phẩm thành công!");
        return "redirect:/products";
    }

    // Chức năng xem chi tiết sản phẩm
    @GetMapping("/{id}/view")
    public String viewProduct(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
        Optional<Product> productOptional = productService.findById(id);
        if (productOptional.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy sản phẩm có ID: " + id);
            return "redirect:/products";
        }
        model.addAttribute("product", productOptional.get());
        return "product/view";
    }
}

