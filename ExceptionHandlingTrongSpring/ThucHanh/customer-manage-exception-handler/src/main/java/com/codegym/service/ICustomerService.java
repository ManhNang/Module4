package com.codegym.service;

import com.codegym.model.Customer;
import com.codegym.exception.DuplicateEmailException;

public interface ICustomerService extends IGenerateService<Customer> {
    @Override
    void save(Customer customer) throws DuplicateEmailException;
}