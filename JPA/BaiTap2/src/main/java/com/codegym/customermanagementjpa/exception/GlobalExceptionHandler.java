package com.codegym.customermanagementjpa.exception;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.format.DateTimeFormatter;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    @ExceptionHandler(BadWordException.class)
    public String handleBadWordException(BadWordException ex, Model model) {
        model.addAttribute("errorMessage", ex.getMessage());
        model.addAttribute("badWord", ex.getBadWord());
        model.addAttribute("feedback", ex.getFeedback());
        model.addAttribute("author", ex.getFeedback() != null ? ex.getFeedback().getAuthor() : "Ẩn danh");
        model.addAttribute("content", ex.getFeedback() != null ? ex.getFeedback().getFeedback() : "");
        model.addAttribute("timestamp", ex.getTimestamp().format(FORMATTER));

        return "error";
    }
}
