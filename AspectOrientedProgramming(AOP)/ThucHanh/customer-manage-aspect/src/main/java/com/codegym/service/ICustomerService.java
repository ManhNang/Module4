package com.codegym.service;

import com.codegym.model.Customer;
import java.util.List;

public interface ICustomerService {
    List<Customer> findAll();

    Customer findOne(Long id) throws Exception;

    void save(Customer customer);

    void remove(Long id);
}