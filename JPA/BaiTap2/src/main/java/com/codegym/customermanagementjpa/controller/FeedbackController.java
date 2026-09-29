package com.codegym.customermanagementjpa.controller;

import com.codegym.customermanagementjpa.model.Feedback;
import com.codegym.customermanagementjpa.service.IFeedbackService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping({"/", "/feedback"})
public class FeedbackController {

    private final IFeedbackService feedbackService;

    @Autowired
    public FeedbackController(IFeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @GetMapping
    public String showIndex(Model model) {
        Feedback newFeedback = new Feedback();
        newFeedback.setRating(5); // Default rating
        model.addAttribute("feedbackForm", newFeedback);

        List<Feedback> todayFeedbacks = feedbackService.findAllToday();
        model.addAttribute("feedbacks", todayFeedbacks);
        model.addAttribute("today", LocalDate.now());

        return "index";
    }

    @PostMapping("/add")
    public String saveFeedback(@ModelAttribute("feedbackForm") Feedback feedback) {
        if (feedback.getDate() == null) {
            feedback.setDate(LocalDate.now());
        }
        feedbackService.save(feedback);
        return "redirect:/";
    }

    @PostMapping("/like/{id}")
    public String likeFeedbackPost(@PathVariable("id") Long id) {
        feedbackService.incrementLike(id);
        return "redirect:/";
    }

    @GetMapping("/like/{id}")
    public String likeFeedbackGet(@PathVariable("id") Long id) {
        feedbackService.incrementLike(id);
        return "redirect:/";
    }
}
