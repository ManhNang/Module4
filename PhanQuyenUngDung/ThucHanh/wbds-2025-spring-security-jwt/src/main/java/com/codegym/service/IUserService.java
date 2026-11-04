package com.codegym.service;

import com.codegym.model.User;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.List;
import java.util.Optional;

public interface IUserService extends UserDetailsService {
    List<User> findAll();

    Optional<User> findById(Long id);

    User save(User user);

    Optional<User> findByUsername(String username);

    Boolean existsByUsername(String username);
}