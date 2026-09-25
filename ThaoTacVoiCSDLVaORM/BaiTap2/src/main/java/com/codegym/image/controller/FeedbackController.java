package com.codegym.image.controller;

import com.codegym.image.model.Feedback;
import com.codegym.image.service.IFeedbackService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
        model.addAttribute("newFeedback", new Feedback());
        model.addAttribute("feedbacks", feedbackService.findAllToday());
        return "index";
    }

    @PostMapping("/save")
    public String saveFeedback(@ModelAttribute("newFeedback") Feedback feedback, RedirectAttributes redirectAttributes) {
        feedbackService.save(feedback);
        redirectAttributes.addFlashAttribute("message", "Cảm ơn bạn đã gửi đánh giá thành công!");
        return "redirect:/feedback";
    }

    @PostMapping("/like/{id}")
    public String likeFeedbackPost(@PathVariable("id") Long id) {
        feedbackService.like(id);
        return "redirect:/feedback";
    }

    @GetMapping("/like/{id}")
    public String likeFeedbackGet(@PathVariable("id") Long id) {
        feedbackService.like(id);
        return "redirect:/feedback";
    }
}
