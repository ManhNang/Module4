package com.codegym.controller;

import com.codegym.model.User;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@SessionAttributes("user")
public class LoginController {

    @ModelAttribute("user")
    public User setUpUserForm() {
        return new User();
    }

    @GetMapping("/login")
    public String Index(@CookieValue(value = "rememberEmail", defaultValue = "") String rememberEmail, Model model) {
        Cookie cookie = new Cookie("rememberEmail", rememberEmail);
        model.addAttribute("cookieValue", cookie);
        return "login";
    }

    @PostMapping("/doLogin")
    public String doLogin(@ModelAttribute("user") User user, Model model,
            @CookieValue(value = "rememberEmail", defaultValue = "") String rememberEmail,
            HttpServletResponse response) {
        // Kiểm tra thông tin đăng nhập mẫu
        if ("admin@gmail.com".equals(user.getEmail()) && "123456".equals(user.getPassword())) {
            rememberEmail = user.getEmail();

            // Tạo và lưu Cookie trong 24 giờ
            Cookie cookie = new Cookie("rememberEmail", rememberEmail);
            cookie.setMaxAge(24 * 60 * 60);
            response.addCookie(cookie);

            model.addAttribute("message", "Đăng nhập thành công! Chào mừng " + user.getEmail());
        } else {
            user.setEmail("");
            model.addAttribute("message", "Đăng nhập thất bại. Email hoặc mật khẩu không đúng!");
        }
        return "login";
    }
}