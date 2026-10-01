package com.codegym.service;

import com.codegym.model.User;

import java.util.List;

public interface UserService {
    void save(User user);
    List<User> findAll();
}
