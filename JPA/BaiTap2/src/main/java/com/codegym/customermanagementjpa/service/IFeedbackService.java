package com.codegym.customermanagementjpa.service;

import com.codegym.customermanagementjpa.model.Feedback;
import java.util.List;

public interface IFeedbackService {
    List<Feedback> findAll();

    List<Feedback> findAllToday();

    Feedback findById(Long id);

    void save(Feedback feedback);

    void incrementLike(Long id);
}
