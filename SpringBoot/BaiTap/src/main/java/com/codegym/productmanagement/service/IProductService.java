package com.codegym.productmanagement.service;

import com.codegym.productmanagement.model.Product;

import java.util.List;
import java.util.Optional;

public interface IProductService {

    List<Product> findAll();

    Optional<Product> findById(Long id);

    Product save(Product product);

    void remove(Long id);

    List<Product> searchByName(String keyword);
}

