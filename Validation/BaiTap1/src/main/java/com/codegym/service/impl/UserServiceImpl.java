package com.codegym.service.impl;

import com.codegym.model.User;
import com.codegym.service.UserService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    private final List<User> userList = new ArrayList<>();

    @Override
    public void save(User user) {
        userList.add(user);
    }

    @Override
    public List<User> findAll() {
        return new ArrayList<>(userList);
    }
}
