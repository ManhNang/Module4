package com.codegym.customermanagementjpa.service;

import com.codegym.customermanagementjpa.model.Blog;
import com.codegym.customermanagementjpa.model.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface IBlogService {

    List<Blog> findAll();

    Page<Blog> findAll(Pageable pageable);

    Page<Blog> findAllByTitle(String title, Pageable pageable);

    Page<Blog> findAllByCategory(Category category, Pageable pageable);

    Page<Blog> findAllByTitleAndCategory(String title, Category category, Pageable pageable);

    Optional<Blog> findById(Long id);

    void save(Blog blog);

    void remove(Long id);
}
