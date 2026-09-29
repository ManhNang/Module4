package com.codegym.customermanagementjpa.service;

import com.codegym.customermanagementjpa.model.Blog;

import java.util.List;
import java.util.Optional;

public interface IBlogService {
    List<Blog> findAll();

    Optional<Blog> findById(Long id);

    void save(Blog blog);

    void remove(Long id);
}
