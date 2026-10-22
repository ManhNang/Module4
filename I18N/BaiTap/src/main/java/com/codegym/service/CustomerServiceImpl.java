package com.codegym.service;

import com.codegym.model.Customer;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CustomerServiceImpl implements CustomerService {
    private static final Map<Long, Customer> customers = new HashMap<>();
    private static Long nextId = 1L;

    static {
        customers.put(nextId, new Customer(nextId++, "Nguyễn Văn A", "nguyenvana@gmail.com", "Hà Nội"));
        customers.put(nextId, new Customer(nextId++, "Trần Thị B", "tranthib@gmail.com", "Hồ Chí Minh"));
        customers.put(nextId, new Customer(nextId++, "Lê Văn C", "levanc@gmail.com", "Đà Nẵng"));
    }

    @Override
    public List<Customer> findAll() {
        return new ArrayList<>(customers.values());
    }

    @Override
    public Customer findById(Long id) {
        return customers.get(id);
    }

    @Override
    public void save(Customer customer) {
        customer.setId(nextId);
        customers.put(nextId++, customer);
    }

    @Override
    public void update(Long id, Customer customer) {
        customer.setId(id);
        customers.put(id, customer);
    }

    @Override
    public void remove(Long id) {
        customers.remove(id);
    }
}
