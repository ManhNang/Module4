package com.codegym.customermanagementjpa.service;

import com.codegym.customermanagementjpa.model.Blog;
import com.codegym.customermanagementjpa.model.Category;
import com.codegym.customermanagementjpa.repository.IBlogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class BlogService implements IBlogService {

    private final IBlogRepository blogRepository;

    @Autowired
    public BlogService(IBlogRepository blogRepository) {
        this.blogRepository = blogRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Blog> findAll() {
        return blogRepository.findAllByOrderByCreatedAtDesc();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Blog> findAll(Pageable pageable) {
        return blogRepository.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Blog> findAllByTitle(String title, Pageable pageable) {
        return blogRepository.findByTitleContainingIgnoreCase(title, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Blog> findAllByCategory(Category category, Pageable pageable) {
        return blogRepository.findAllByCategory(category, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Blog> findAllByTitleAndCategory(String title, Category category, Pageable pageable) {
        return blogRepository.findByTitleContainingIgnoreCaseAndCategory(title, category, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Blog> findById(Long id) {
        return blogRepository.findById(id);
    }

    @Override
    public void save(Blog blog) {
        blogRepository.save(blog);
    }

    @Override
    public void remove(Long id) {
        blogRepository.deleteById(id);
    }
}
