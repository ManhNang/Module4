package com.codegym.controller;

import com.codegym.exception.BookNotFoundException;
import com.codegym.exception.BookOutOfStockException;
import com.codegym.exception.InvalidBorrowCodeException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BookOutOfStockException.class)
    public String handleBookOutOfStockException(BookOutOfStockException ex, HttpServletRequest request, Model model) {
        model.addAttribute("errorTitle", "HẾT SÁCH TRONG THƯ VIỆN");
        model.addAttribute("errorMessage", ex.getMessage());
        model.addAttribute("errorType", "OUT_OF_STOCK");
        String referer = request.getHeader("Referer");
        model.addAttribute("backUrl", (referer != null && !referer.isBlank()) ? referer : "/books");
        return "error";
    }

    @ExceptionHandler(InvalidBorrowCodeException.class)
    public String handleInvalidBorrowCodeException(InvalidBorrowCodeException ex, HttpServletRequest request, Model model) {
        model.addAttribute("errorTitle", "MÃ TRẢ SÁCH KHÔNG HỢP LỆ");
        model.addAttribute("errorMessage", ex.getMessage());
        model.addAttribute("errorType", "INVALID_CODE");
        model.addAttribute("backUrl", "/books/return");
        return "error";
    }

    @ExceptionHandler(BookNotFoundException.class)
    public String handleBookNotFoundException(BookNotFoundException ex, HttpServletRequest request, Model model) {
        model.addAttribute("errorTitle", "KHÔNG TÌM THẤY SÁCH");
        model.addAttribute("errorMessage", ex.getMessage());
        model.addAttribute("errorType", "NOT_FOUND");
        model.addAttribute("backUrl", "/books");
        return "error";
    }

    @ExceptionHandler(Exception.class)
    public String handleGeneralException(Exception ex, HttpServletRequest request, Model model) {
        model.addAttribute("errorTitle", "CÓ LỖI HỆ THỐNG XẢY RA");
        model.addAttribute("errorMessage", ex.getMessage() != null ? ex.getMessage() : "Lỗi không xác định trong quá trình xử lý.");
        model.addAttribute("errorType", "SERVER_ERROR");
        String referer = request.getHeader("Referer");
        model.addAttribute("backUrl", (referer != null && !referer.isBlank()) ? referer : "/books");
        return "error";
    }
}
