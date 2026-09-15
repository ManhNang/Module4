package com.codegym.springgreeting.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.text.DecimalFormat;

@Controller
public class CurrencyConverterController {

    private static final double DEFAULT_RATE = 25450.0;

    @GetMapping({"/", "/converter"})
    public String showConverterForm(Model model) {
        model.addAttribute("rate", DEFAULT_RATE);
        model.addAttribute("usd", 1.0);
        return "index";
    }

    @PostMapping("/convert")
    public String convertCurrency(
            @RequestParam(name = "rate", defaultValue = "25450") double rate,
            @RequestParam(name = "usd", defaultValue = "0") double usd,
            Model model) {

        if (rate < 0 || usd < 0) {
            model.addAttribute("errorMessage", "Tỉ giá và số lượng USD không được âm!");
        } else {
            double vnd = rate * usd;

            DecimalFormat currencyFormat = new DecimalFormat("#,##0.##");
            model.addAttribute("rateFormatted", currencyFormat.format(rate));
            model.addAttribute("usdFormatted", currencyFormat.format(usd));
            model.addAttribute("vndFormatted", currencyFormat.format(vnd));
            model.addAttribute("resultCalculated", true);
        }

        model.addAttribute("rate", rate);
        model.addAttribute("usd", usd);

        return "index";
    }
}
