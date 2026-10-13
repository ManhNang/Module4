package com.codegym.service;

import com.codegym.model.Blog;
import com.codegym.model.Category;

import java.util.Optional;

public interface IBlogService {
    Iterable<Blog> findAll();

    Optional<Blog> findById(Long id);

    Iterable<Blog> findAllByCategory(Category category);
}
