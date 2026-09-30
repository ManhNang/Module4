package com.codegym.customermanagementjpa.repository;

import com.codegym.customermanagementjpa.model.Blog;
import com.codegym.customermanagementjpa.model.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IBlogRepository extends JpaRepository<Blog, Long> {

    List<Blog> findAllByOrderByCreatedAtDesc();

    Page<Blog> findAll(Pageable pageable);

    Page<Blog> findByTitleContainingIgnoreCase(String title, Pageable pageable);

    Page<Blog> findAllByCategory(Category category, Pageable pageable);

    Page<Blog> findByTitleContainingIgnoreCaseAndCategory(String title, Category category, Pageable pageable);

    List<Blog> findAllByCategory(Category category);
}
