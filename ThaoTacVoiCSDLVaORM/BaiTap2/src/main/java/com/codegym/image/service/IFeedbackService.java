package com.codegym.image.service;

import com.codegym.image.model.Feedback;
import java.util.List;

public interface IFeedbackService {
    List<Feedback> findAllToday();
    Feedback findById(Long id);
    void save(Feedback feedback);
    void like(Long id);
}
