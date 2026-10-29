package com.codegym.controller;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;

@Controller
public class UserController {

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/user")
    public String user(Principal principal, Model model) {
        // Cách 1: Lấy thông tin username thông qua đối tượng Principal truyền từ
        // Controller
        String username = principal.getName();
        model.addAttribute("username", username);
        return "user";
    }

    @GetMapping("/admin")
    public String admin(Model model) {
        // Cách 2: Lấy thông tin username tĩnh thông qua SecurityContextHolder
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        model.addAttribute("username", username);
        return "admin";
    }
}