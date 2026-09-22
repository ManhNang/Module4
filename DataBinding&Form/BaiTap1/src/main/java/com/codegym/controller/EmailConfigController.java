package com.codegym.controller;

import com.codegym.model.EmailConfig;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class EmailConfigController {

    private EmailConfig currentConfig = new EmailConfig("English", 25, false, "Thor\nKing, Asgard");

    @ModelAttribute("languagesList")
    public String[] getLanguagesList() {
        return new String[]{"English", "Vietnamese", "Japanese", "Chinese"};
    }

    @ModelAttribute("pageSizeList")
    public int[] getPageSizeList() {
        return new int[]{5, 10, 15, 25, 50, 100};
    }

    @GetMapping({"/", "/settings"})
    public String showSettings(Model model) {
        model.addAttribute("emailConfig", currentConfig);
        return "info";
    }

    @GetMapping("/settings/edit")
    public String showEditForm(Model model) {
        model.addAttribute("emailConfig", currentConfig);
        return "edit";
    }

    @PostMapping("/settings/update")
    public String updateSettings(@ModelAttribute("emailConfig") EmailConfig emailConfig,
                                 RedirectAttributes redirectAttributes) {
        this.currentConfig = emailConfig;
        redirectAttributes.addFlashAttribute("message", "Cấu hình hòm thư đã được cập nhật thành công!");
        return "redirect:/settings";
    }
}
