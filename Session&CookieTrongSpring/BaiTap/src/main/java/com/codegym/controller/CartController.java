package com.codegym.controller;

import com.codegym.model.Cart;
import com.codegym.model.Product;
import com.codegym.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;
import java.util.Optional;

@Controller
@SessionAttributes("cart")
public class CartController {

    private final ProductService productService;

    @Autowired
    public CartController(ProductService productService) {
        this.productService = productService;
    }

    @ModelAttribute("cart")
    public Cart setUpCart() {
        return new Cart();
    }

    @GetMapping("/cart")
    public ModelAndView showCart(@ModelAttribute("cart") Cart cart) {
        ModelAndView modelAndView = new ModelAndView("cart/index");
        modelAndView.addObject("cart", cart);
        return modelAndView;
    }

    @GetMapping("/cart/increase/{id}")
    public String increaseQuantity(
            @PathVariable("id") Long id,
            @ModelAttribute("cart") Cart cart,
            RedirectAttributes redirectAttributes) {
        Optional<Product> productOptional = productService.findById(id);
        if (productOptional.isPresent()) {
            cart.addProduct(productOptional.get());
            redirectAttributes.addFlashAttribute("message", "Đã tăng số lượng sản phẩm: " + productOptional.get().getName());
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy sản phẩm!");
        }
        return "redirect:/cart";
    }

    @GetMapping("/cart/decrease/{id}")
    public String decreaseQuantity(
            @PathVariable("id") Long id,
            @ModelAttribute("cart") Cart cart,
            RedirectAttributes redirectAttributes) {
        Optional<Product> productOptional = productService.findById(id);
        if (productOptional.isPresent()) {
            Product product = productOptional.get();
            for (Map.Entry<Product, Integer> entry : cart.getProducts().entrySet()) {
                if (entry.getKey().getId().equals(id)) {
                    int newQty = entry.getValue() - 1;
                    cart.changeQuantity(product, newQty);
                    if (newQty <= 0) {
                        redirectAttributes.addFlashAttribute("message", "Đã xóa sản phẩm \"" + product.getName() + "\" khỏi giỏ hàng!");
                    } else {
                        redirectAttributes.addFlashAttribute("message", "Đã giảm số lượng sản phẩm: " + product.getName());
                    }
                    break;
                }
            }
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy sản phẩm!");
        }
        return "redirect:/cart";
    }

    @PostMapping("/cart/update")
    public String updateQuantity(
            @RequestParam("id") Long id,
            @RequestParam("quantity") int quantity,
            @ModelAttribute("cart") Cart cart,
            RedirectAttributes redirectAttributes) {
        Optional<Product> productOptional = productService.findById(id);
        if (productOptional.isPresent()) {
            Product product = productOptional.get();
            cart.changeQuantity(product, quantity);
            if (quantity <= 0) {
                redirectAttributes.addFlashAttribute("message", "Đã xóa sản phẩm \"" + product.getName() + "\" khỏi giỏ hàng!");
            } else {
                redirectAttributes.addFlashAttribute("message", "Đã cập nhật số lượng cho \"" + product.getName() + "\" thành " + quantity + "!");
            }
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy sản phẩm!");
        }
        return "redirect:/cart";
    }

    @GetMapping("/cart/remove/{id}")
    public String removeProduct(
            @PathVariable("id") Long id,
            @ModelAttribute("cart") Cart cart,
            RedirectAttributes redirectAttributes) {
        Optional<Product> productOptional = productService.findById(id);
        if (productOptional.isPresent()) {
            Product product = productOptional.get();
            cart.removeProduct(product);
            redirectAttributes.addFlashAttribute("message", "Đã xóa \"" + product.getName() + "\" khỏi giỏ hàng!");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy sản phẩm!");
        }
        return "redirect:/cart";
    }

    @GetMapping("/cart/checkout")
    public ModelAndView checkout(
            @ModelAttribute("cart") Cart cart,
            RedirectAttributes redirectAttributes) {
        if (cart.getProducts().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Giỏ hàng của bạn đang trống, không thể thanh toán!");
            return new ModelAndView("redirect:/cart");
        }

        ModelAndView modelAndView = new ModelAndView("cart/checkout-success");
        String orderCode = "ORD-" + System.currentTimeMillis();
        double totalPayment = cart.countTotalPayment();
        int totalItems = cart.countProductQuantity();

        modelAndView.addObject("orderCode", orderCode);
        modelAndView.addObject("totalPayment", totalPayment);
        modelAndView.addObject("totalItems", totalItems);

        // Clear cart after checkout
        cart.clearCart();

        return modelAndView;
    }
}
