package com.codegym.model;

import java.util.ArrayList;
import java.util.List;

public class UserDao {
    private static final List<User> users = new ArrayList<>();

    static {
        users.add(new User("john", "123456", "John Doe", "john@codegym.vn", 25));
        users.add(new User("bill", "123456", "Bill Gates", "bill@codegym.vn", 30));
        users.add(new User("alex", "123456", "Alex Ferguson", "alex@codegym.vn", 28));
    }

    public static User checkLogin(Login login) {
        for (User u : users) {
            if (u.getAccount().equals(login.getAccount()) && u.getPassword().equals(login.getPassword())) {
                return u;
            }
        }
        return null;
    }
}