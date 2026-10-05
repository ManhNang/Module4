package com.codegym.controller;

import com.codegym.model.Customer;
import com.codegym.service.ICustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping("/customers")
public class CustomerController {

    @Autowired
    private ICustomerService customerService;

    @GetMapping
    public String showList(Model model) {
        model.addAttribute("customers", customerService.findAll());
        return "index";
    }

    @GetMapping("/info/{id}")
    public ModelAndView showInfo(@PathVariable Long id) {
        try {
            Customer customer = customerService.findOne(id);
            return new ModelAndView("info", "customer", customer);
        } catch (Exception e) {
            return new ModelAndView("error");
        }
    }
}