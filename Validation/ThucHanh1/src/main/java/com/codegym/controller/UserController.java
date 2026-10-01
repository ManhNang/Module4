package com.codegym.controller;

import com.codegym.model.User;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class UserController {

    @GetMapping("/")
    public String showForm(Model model) {
        model.addAttribute("user", new User());
        return "index";
    }

    @PostMapping("/validateUser")
    public String checkValidation(@Valid @ModelAttribute("user") User user,
            BindingResult bindingResult,
            Model model) {
        if (bindingResult.hasErrors()) {
            return "index";
        }
        model.addAttribute("user", user);
        return "result";
    }
}