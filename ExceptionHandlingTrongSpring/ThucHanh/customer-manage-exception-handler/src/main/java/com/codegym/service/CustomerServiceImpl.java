package com.codegym.service;

import com.codegym.exception.DuplicateEmailException;
import com.codegym.model.Customer;
import com.codegym.repository.ICustomerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomerServiceImpl implements ICustomerService {

    @Autowired
    private ICustomerRepository customerRepository;

    @Override
    public List<Customer> findAll() {
        return customerRepository.findAll();
    }

    @Override
    public Customer findById(Long id) {
        return customerRepository.findById(id);
    }

    @Override
    public void save(Customer customer) throws DuplicateEmailException {
        try {
            customerRepository.save(customer);
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateEmailException("Email '" + customer.getEmail() + "' đã được sử dụng!");
        } catch (Exception e) {
            Throwable cause = e;
            while (cause != null) {
                if (cause instanceof java.sql.SQLIntegrityConstraintViolationException
                        || (cause.getMessage() != null && cause.getMessage().toLowerCase().contains("duplicate"))) {
                    throw new DuplicateEmailException("Email '" + customer.getEmail() + "' đã được sử dụng!");
                }
                cause = cause.getCause();
            }
            throw e;
        }
    }

    @Override
    public void remove(Long id) {
        customerRepository.remove(id);
    }
}