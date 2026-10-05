package com.codegym.customermanagementjpa.service;

import com.codegym.customermanagementjpa.exception.BadWordException;
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
    private final BadWordFilter badWordFilter;

    @Autowired
    public FeedbackService(IFeedbackRepository feedbackRepository, BadWordFilter badWordFilter) {
        this.feedbackRepository = feedbackRepository;
        this.badWordFilter = badWordFilter;
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
        // Kiểm tra từ xấu trong nội dung phản hồi (feedback) và tên tác giả (author)
        String badWordInFeedback = badWordFilter.findFirstBadWord(feedback.getFeedback());
        String badWordInAuthor = badWordFilter.findFirstBadWord(feedback.getAuthor());

        String detectedWord = badWordInFeedback != null ? badWordInFeedback : badWordInAuthor;

        if (detectedWord != null) {
            throw new BadWordException(
                    "Nội dung nhận xét chứa từ ngữ không phù hợp: '" + detectedWord + "'",
                    feedback,
                    detectedWord
            );
        }

        return feedbackRepository.save(feedback);
    }

    @Override
    public void incrementLike(Long id) {
        feedbackRepository.incrementLike(id);
    }
}
