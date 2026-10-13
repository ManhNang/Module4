package com.codegym.service;

import com.codegym.model.Category;

import java.util.Optional;

public interface ICategoryService {
    Iterable<Category> findAll();

    Optional<Category> findById(Long id);
}
