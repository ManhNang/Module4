package com.codegym.repository;

import com.codegym.model.Blog;
import com.codegym.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IBlogRepository extends JpaRepository<Blog, Long> {
    Iterable<Blog> findAllByCategory(Category category);

    Iterable<Blog> findAllByCategoryId(Long categoryId);
}
