package com.pokemon.blindbox.controller.web;

import com.pokemon.blindbox.entity.User;
import com.pokemon.blindbox.exception.BusinessValidationException;
import com.pokemon.blindbox.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final Environment environment;

    @GetMapping("/login")
    public String loginPage(Model model) {
        boolean isDev = Arrays.asList(environment.getActiveProfiles()).contains("dev");
        model.addAttribute("isDevProfile", isDev);
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerPage() {
        return "auth/register";
    }

    @PostMapping("/register")
    public String handleRegister(
            @RequestParam("username") String username,
            @RequestParam("email") String email,
            @RequestParam("password") String password,
            @RequestParam("confirmPassword") String confirmPassword,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        try {
            User savedUser = authService.registerNewTrainer(username, email, password, confirmPassword);
            redirectAttributes.addFlashAttribute(
                    "registeredMessage",
                    "Đăng ký tài khoản '" + savedUser.getUsername() + "' thành công! Tặng 1.000 Xu khởi đầu vào Ví."
            );
            redirectAttributes.addFlashAttribute("registeredUsername", savedUser.getUsername());
            return "redirect:/login?registered=true";
        } catch (BusinessValidationException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("username", username != null ? username.trim() : "");
            model.addAttribute("email", email != null ? email.trim() : "");
            return "auth/register";
        }
    }

    @GetMapping("/access-denied")
    public String accessDeniedPage() {
        return "auth/access-denied";
    }
}
