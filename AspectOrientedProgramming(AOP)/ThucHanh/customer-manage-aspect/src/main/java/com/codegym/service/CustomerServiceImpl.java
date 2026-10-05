package com.codegym.service;

import com.codegym.model.Customer;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class CustomerServiceImpl implements ICustomerService {
    private static final Map<Long, Customer> customers = new HashMap<>();

    static {
        customers.put(1L, new Customer(1L, "Nguyen Van A", "a@codegym.vn", "Ha Noi"));
        customers.put(2L, new Customer(2L, "Tran Van B", "b@codegym.vn", "Da Nang"));
    }

    @Override
    public List<Customer> findAll() {
        return new ArrayList<>(customers.values());
    }

    @Override
    public Customer findOne(Long id) throws Exception {
        Customer customer = customers.get(id);
        if (customer == null) {
            throw new Exception("Không tìm thấy khách hàng với ID: " + id);
        }
        return customer;
    }

    @Override
    public void save(Customer customer) {
        customers.put(customer.getId(), customer);
    }

    @Override
    public void remove(Long id) {
        customers.remove(id);
    }
}