package com.codegym.customermanagementjpa.repository;

import com.codegym.customermanagementjpa.model.Feedback;
import java.util.List;

public interface IFeedbackRepository {
    List<Feedback> findAll();

    List<Feedback> findAllToday();

    Feedback findById(Long id);

    void save(Feedback feedback);

    void incrementLike(Long id);
}
