package com.codegym.productmanagement.service;

import com.codegym.productmanagement.model.Product;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ProductService implements IProductService {
    private static final Map<Integer, Product> products = new HashMap<>();

    static {
        products.put(1, new Product(1, "Máy tính", 25000000, "không", "ASUS"));
        products.put(2, new Product(2, "Điện thoại", 12000000, "không", "Iphone"));
        products.put(3, new Product(3, "Búp bê", 123000, "không", "Thắng"));
        products.put(4, new Product(4, "Máy giặt", 12000000, "không", "Samsung"));
        products.put(5, new Product(5, "Quần đùi", 50000, "không", "Soopi"));
    }

    @Override
    public List<Product> findAll() {
        return new ArrayList<>(products.values());
    }

    @Override
    public void save(Product product) {
        products.put(product.getId(), product);
    }

    @Override
    public Product findById(int id) {
        return products.get(id);
    }

    @Override
    public void update(int id, Product product) {
        products.put(id, product);
    }

    @Override
    public void remove(int id) {
        products.remove(id);
    }

    @Override
    public List<Product> findByName(String name) {
        List<Product> result = new ArrayList<>();
        if (name == null || name.trim().isEmpty()) {
            return findAll();
        }
        String keyword = name.toLowerCase().trim();
        for (Product product : products.values()) {
            if (product.getName() != null && product.getName().toLowerCase().contains(keyword)) {
                result.add(product);
            }
        }
        return result;
    }
}
