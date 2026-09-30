package com.codegym.customermanagementjpa.service;

import com.codegym.customermanagementjpa.model.Feedback;
import com.codegym.customermanagementjpa.repository.IFeedbackRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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
        return feedbackRepository.findAllByDate(LocalDate.now());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Feedback> findAll(Pageable pageable) {
        return feedbackRepository.findAllByOrderByDateDescIdDesc(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Feedback> findAllToday(Pageable pageable) {
        return feedbackRepository.findAllByDateOrderByDateDescIdDesc(LocalDate.now(), pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Feedback> getFeedbacks(String filter, Pageable pageable) {
        if ("today".equalsIgnoreCase(filter)) {
            return findAllToday(pageable);
        }
        return findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Feedback findById(Long id) {
        return feedbackRepository.findById(id).orElse(null);
    }

    @Override
    public Feedback save(Feedback feedback) {
        return feedbackRepository.save(feedback);
    }

    @Override
    public void incrementLike(Long id) {
        feedbackRepository.incrementLike(id);
    }
}
