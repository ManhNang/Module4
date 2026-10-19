package com.codegym.jqueryeffects;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping({ "/", "/slide-demo" })
    public String showSlideDemo() {
        return "slide";
    }

    @GetMapping("/animate-demo")
    public String showAnimateDemo() {
        return "animate";
    }
}
