package com.codegym.customermanagementjpa.service;

import com.codegym.customermanagementjpa.model.Blog;
import com.codegym.customermanagementjpa.model.Category;
import com.codegym.customermanagementjpa.repository.IBlogRepository;
import com.codegym.customermanagementjpa.repository.ICategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CategoryService implements ICategoryService {

    private final ICategoryRepository categoryRepository;
    private final IBlogRepository blogRepository;

    @Autowired
    public CategoryService(ICategoryRepository categoryRepository, IBlogRepository blogRepository) {
        this.categoryRepository = categoryRepository;
        this.blogRepository = blogRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Category> findAll() {
        return categoryRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Category> findById(Long id) {
        return categoryRepository.findById(id);
    }

    @Override
    public void save(Category category) {
        categoryRepository.save(category);
    }

    @Override
    public void remove(Long id) {
        Optional<Category> categoryOptional = categoryRepository.findById(id);
        if (categoryOptional.isPresent()) {
            Category category = categoryOptional.get();
            List<Blog> blogs = blogRepository.findAllByCategory(category);
            for (Blog blog : blogs) {
                blog.setCategory(null);
                blogRepository.save(blog);
            }
            categoryRepository.deleteById(id);
        }
    }
}
