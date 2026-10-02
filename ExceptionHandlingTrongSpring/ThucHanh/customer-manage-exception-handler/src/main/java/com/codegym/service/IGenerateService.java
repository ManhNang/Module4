package com.codegym.service;

import java.util.List;

public interface IGenerateService<T> {
    List<T> findAll();

    T findById(Long id);

    void save(T t) throws Exception;

    void remove(Long id);
}