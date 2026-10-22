package com.codegym.controller;

import com.codegym.model.Customer;
import com.codegym.service.CustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/customers")
public class CustomerController {

    @Autowired
    private CustomerService customerService;

    // Hiển thị danh sách khách hàng
    @GetMapping
    public ModelAndView listCustomers() {
        ModelAndView modelAndView = new ModelAndView("customer/list");
        modelAndView.addObject("customers", customerService.findAll());
        return modelAndView;
    }

    // Hiển thị form tạo khách hàng mới
    @GetMapping("/create")
    public String showCreateForm(Model model) {
        model.addAttribute("customer", new Customer());
        return "customer/create";
    }

    // Xử lý tạo khách hàng mới
    @PostMapping("/create")
    public String createCustomer(@ModelAttribute("customer") Customer customer,
                                 RedirectAttributes redirectAttributes) {
        customerService.save(customer);
        redirectAttributes.addFlashAttribute("message", "customer.create.success");
        return "redirect:/customers";
    }

    // Hiển thị form chỉnh sửa khách hàng
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        Customer customer = customerService.findById(id);
        if (customer == null) {
            return "redirect:/customers";
        }
        model.addAttribute("customer", customer);
        return "customer/edit";
    }

    // Xử lý cập nhật khách hàng
    @PostMapping("/edit/{id}")
    public String updateCustomer(@PathVariable Long id,
                                 @ModelAttribute("customer") Customer customer,
                                 RedirectAttributes redirectAttributes) {
        customerService.update(id, customer);
        redirectAttributes.addFlashAttribute("message", "customer.update.success");
        return "redirect:/customers";
    }

    // Hiển thị form xác nhận xóa khách hàng
    @GetMapping("/delete/{id}")
    public String showDeleteForm(@PathVariable Long id, Model model) {
        Customer customer = customerService.findById(id);
        if (customer == null) {
            return "redirect:/customers";
        }
        model.addAttribute("customer", customer);
        return "customer/delete";
    }

    // Xử lý xóa khách hàng
    @PostMapping("/delete/{id}")
    public String deleteCustomer(@PathVariable Long id,
                                 RedirectAttributes redirectAttributes) {
        customerService.remove(id);
        redirectAttributes.addFlashAttribute("message", "customer.delete.success");
        return "redirect:/customers";
    }
}
