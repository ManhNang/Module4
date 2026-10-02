package com.codegym.controller;

import com.codegym.exception.DuplicateEmailException;
import com.codegym.model.Customer;
import com.codegym.service.ICustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/customers")
public class CustomerController {

    @Autowired
    private ICustomerService customerService;

    @GetMapping
    public String listCustomers(Model model) {
        List<Customer> customers = customerService.findAll();
        model.addAttribute("customers", customers);
        return "index";
    }

    @GetMapping("/create")
    public ModelAndView showCreateForm() {
        ModelAndView modelAndView = new ModelAndView("create");
        modelAndView.addObject("customer", new Customer());
        return modelAndView;
    }

    @PostMapping("/save")
    public String saveCustomer(@ModelAttribute("customer") Customer customer, RedirectAttributes redirect)
            throws DuplicateEmailException {
        customerService.save(customer);
        redirect.addFlashAttribute("message", "Lưu thông tin khách hàng thành công!");
        return "redirect:/customers";
    }

    @GetMapping("/update/{id}")
    public ModelAndView showUpdateForm(@PathVariable Long id) {
        Customer customer = customerService.findById(id);
        ModelAndView modelAndView = new ModelAndView("update");
        modelAndView.addObject("customer", customer);
        return modelAndView;
    }

    @PostMapping("/update")
    public String updateCustomer(@ModelAttribute("customer") Customer customer, RedirectAttributes redirect)
            throws DuplicateEmailException {
        customerService.save(customer);
        redirect.addFlashAttribute("message", "Cập nhật khách hàng thành công!");
        return "redirect:/customers";
    }

    @GetMapping("/delete/{id}")
    public ModelAndView showDeleteForm(@PathVariable Long id) {
        Customer customer = customerService.findById(id);
        ModelAndView modelAndView = new ModelAndView("delete");
        modelAndView.addObject("customer", customer);
        return modelAndView;
    }

    @PostMapping("/delete")
    public String deleteCustomer(@ModelAttribute("customer") Customer customer, RedirectAttributes redirect) {
        customerService.remove(customer.getId());
        redirect.addFlashAttribute("message", "Xóa khách hàng thành công!");
        return "redirect:/customers";
    }

    @GetMapping("/view/{id}")
    public ModelAndView viewCustomer(@PathVariable Long id) {
        Customer customer = customerService.findById(id);
        ModelAndView modelAndView = new ModelAndView("view");
        modelAndView.addObject("customer", customer);
        return modelAndView;
    }

    // Xử lý ngoại lệ DuplicateEmailException cục bộ cho Controller này
    @ExceptionHandler({DuplicateEmailException.class, org.springframework.dao.DataIntegrityViolationException.class})
    public ModelAndView showInputsNotAcceptable(Exception e) {
        ModelAndView modelAndView = new ModelAndView("inputs-not-acceptable");
        String message = (e instanceof DuplicateEmailException) ? e.getMessage() : "Email đã tồn tại trong hệ thống!";
        modelAndView.addObject("errorMessage", message);
        return modelAndView;
    }
}