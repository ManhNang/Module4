package com.codegym.controller;

import com.codegym.model.Cart;
import com.codegym.model.Product;
import com.codegym.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
@SessionAttributes("cart")
public class ProductController {

    private final ProductService productService;

    @Autowired
    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @ModelAttribute("cart")
    public Cart setUpCart() {
        return new Cart();
    }

    @GetMapping({"/", "/shop"})
    public ModelAndView showShop() {
        ModelAndView modelAndView = new ModelAndView("product/list");
        modelAndView.addObject("products", productService.findAll());
        return modelAndView;
    }

    @GetMapping("/product/{id}")
    public ModelAndView showProductDetail(@PathVariable("id") Long id) {
        Optional<Product> productOptional = productService.findById(id);
        if (productOptional.isEmpty()) {
            return new ModelAndView("redirect:/shop");
        }
        ModelAndView modelAndView = new ModelAndView("product/detail");
        modelAndView.addObject("product", productOptional.get());
        return modelAndView;
    }

    @GetMapping("/add/{id}")
    public String addToCart(
            @PathVariable("id") Long id,
            @ModelAttribute("cart") Cart cart,
            @RequestParam(value = "action", required = false) String action,
            RedirectAttributes redirectAttributes) {
        Optional<Product> productOptional = productService.findById(id);
        if (productOptional.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Sản phẩm không tồn tại!");
            return "redirect:/shop";
        }

        cart.addProduct(productOptional.get());
        redirectAttributes.addFlashAttribute("message", "Đã thêm \"" + productOptional.get().getName() + "\" vào giỏ hàng thành công!");

        if ("show".equalsIgnoreCase(action)) {
            return "redirect:/cart";
        }
        return "redirect:/shop";
    }
}
