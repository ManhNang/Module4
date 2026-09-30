package com.codegym.customermanagementjpa.service;

import com.codegym.customermanagementjpa.model.Feedback;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface IFeedbackService {
    List<Feedback> findAll();

    List<Feedback> findAllToday();

    Page<Feedback> findAll(Pageable pageable);

    Page<Feedback> findAllToday(Pageable pageable);

    Page<Feedback> getFeedbacks(String filter, Pageable pageable);

    Feedback findById(Long id);

    Feedback save(Feedback feedback);

    void incrementLike(Long id);
}
