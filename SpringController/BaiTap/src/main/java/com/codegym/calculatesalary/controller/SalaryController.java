package com.codegym.calculatesalary.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.text.DecimalFormat;

@Controller
public class SalaryController {

    @GetMapping("/")
    public String showForm() {
        return "index";
    }

    @PostMapping("/calculate")
    public String calculateSalary(
            @RequestParam(value = "shiftHours", required = false) double[] shiftHours,
            @RequestParam(value = "hourlyRate", defaultValue = "0") double hourlyRate,
            Model model) {

        double totalHours = 0.0;
        double morningHours = 0.0;
        double afternoonHours = 0.0;
        double eveningHours = 0.0;

        if (shiftHours != null) {
            if (shiftHours.length > 0) morningHours = shiftHours[0];
            if (shiftHours.length > 1) afternoonHours = shiftHours[1];
            if (shiftHours.length > 2) eveningHours = shiftHours[2];

            for (double hours : shiftHours) {
                totalHours += hours;
            }
        }

        // Tổng tiền lương = Tổng số giờ làm việc * Mức lương mỗi giờ
        double totalSalary = totalHours * hourlyRate;

        DecimalFormat currencyFormatter = new DecimalFormat("#,##0.##");

        model.addAttribute("shiftHours", shiftHours);
        model.addAttribute("morningHours", morningHours);
        model.addAttribute("afternoonHours", afternoonHours);
        model.addAttribute("eveningHours", eveningHours);
        model.addAttribute("totalHours", totalHours);
        model.addAttribute("hourlyRate", hourlyRate);
        model.addAttribute("totalSalary", totalSalary);
        model.addAttribute("formattedHourlyRate", currencyFormatter.format(hourlyRate));
        model.addAttribute("formattedTotalSalary", currencyFormatter.format(totalSalary));

        return "result";
    }
}
