package com.codegym.service;

import com.codegym.model.Blog;
import java.util.List;
import java.util.Optional;

public interface IBlogService {
    List<Blog> findAll();
    Optional<Blog> findById(Long id);
    void save(Blog blog);
    void delete(Long id);
}

