package com.codegym.customermanagementjpa.controller;

import com.codegym.customermanagementjpa.model.Feedback;
import com.codegym.customermanagementjpa.service.IFeedbackService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Controller
@RequestMapping({"/", "/feedback"})
public class FeedbackController {

    private final IFeedbackService feedbackService;

    @Autowired
    public FeedbackController(IFeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @GetMapping
    public String showIndex(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "5") int size,
            @RequestParam(name = "filter", defaultValue = "all") String filter,
            Model model) {

        Feedback newFeedback = new Feedback();
        newFeedback.setRating(5); // Default rating 5 sao
        model.addAttribute("feedbackForm", newFeedback);

        if (page < 0) {
            page = 0;
        }
        if (size <= 0) {
            size = 5;
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<Feedback> feedbackPage = feedbackService.getFeedbacks(filter, pageable);

        // Nếu page vượt quá số trang hiện có (sau khi xóa hoặc lọc)
        if (page >= feedbackPage.getTotalPages() && feedbackPage.getTotalPages() > 0) {
            page = feedbackPage.getTotalPages() - 1;
            pageable = PageRequest.of(page, size);
            feedbackPage = feedbackService.getFeedbacks(filter, pageable);
        }

        model.addAttribute("feedbackPage", feedbackPage);
        model.addAttribute("feedbacks", feedbackPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("pageSize", size);
        model.addAttribute("totalPages", feedbackPage.getTotalPages());
        model.addAttribute("totalElements", feedbackPage.getTotalElements());
        model.addAttribute("filter", filter);
        model.addAttribute("today", LocalDate.now());

        return "index";
    }

    @PostMapping("/add")
    public String saveFeedback(
            @ModelAttribute("feedbackForm") Feedback feedback,
            @RequestParam(name = "filter", defaultValue = "all") String filter,
            @RequestParam(name = "size", defaultValue = "5") int size) {
        if (feedback.getDate() == null) {
            feedback.setDate(LocalDate.now());
        }
        feedbackService.save(feedback);
        return "redirect:/?page=0&size=" + size + "&filter=" + filter;
    }

    @PostMapping("/like/{id}")
    public String likeFeedbackPost(
            @PathVariable("id") Long id,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "5") int size,
            @RequestParam(name = "filter", defaultValue = "all") String filter) {
        feedbackService.incrementLike(id);
        return "redirect:/?page=" + page + "&size=" + size + "&filter=" + filter;
    }

    @GetMapping("/like/{id}")
    public String likeFeedbackGet(
            @PathVariable("id") Long id,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "5") int size,
            @RequestParam(name = "filter", defaultValue = "all") String filter) {
        feedbackService.incrementLike(id);
        return "redirect:/?page=" + page + "&size=" + size + "&filter=" + filter;
    }
}
