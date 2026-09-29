package com.codegym.customermanagementjpa.service;

import com.codegym.customermanagementjpa.model.Feedback;
import com.codegym.customermanagementjpa.repository.IFeedbackRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class FeedbackService implements IFeedbackService {

    private final IFeedbackRepository feedbackRepository;

    @Autowired
    public FeedbackService(IFeedbackRepository feedbackRepository) {
        this.feedbackRepository = feedbackRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Feedback> findAll() {
        return feedbackRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Feedback> findAllToday() {
        return feedbackRepository.findAllToday();
    }

    @Override
    @Transactional(readOnly = true)
    public Feedback findById(Long id) {
        return feedbackRepository.findById(id);
    }

    @Override
    public void save(Feedback feedback) {
        feedbackRepository.save(feedback);
    }

    @Override
    public void incrementLike(Long id) {
        feedbackRepository.incrementLike(id);
    }
}
