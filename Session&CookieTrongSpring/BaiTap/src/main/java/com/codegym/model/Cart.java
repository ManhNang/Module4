package com.codegym.model;

import java.util.LinkedHashMap;
import java.util.Map;

public class Cart {
    private Map<Product, Integer> products = new LinkedHashMap<>();

    public Cart() {
    }

    public Cart(Map<Product, Integer> products) {
        this.products = products;
    }

    public Map<Product, Integer> getProducts() {
        return products;
    }

    public void setProducts(Map<Product, Integer> products) {
        this.products = products;
    }

    private boolean checkItemInCart(Product product) {
        for (Map.Entry<Product, Integer> entry : products.entrySet()) {
            if (entry.getKey().getId().equals(product.getId())) {
                return true;
            }
        }
        return false;
    }

    private Map.Entry<Product, Integer> selectItemInCart(Product product) {
        for (Map.Entry<Product, Integer> entry : products.entrySet()) {
            if (entry.getKey().getId().equals(product.getId())) {
                return entry;
            }
        }
        return null;
    }

    public void addProduct(Product product) {
        if (!checkItemInCart(product)) {
            products.put(product, 1);
        } else {
            Map.Entry<Product, Integer> itemEntry = selectItemInCart(product);
            if (itemEntry != null) {
                products.replace(itemEntry.getKey(), itemEntry.getValue() + 1);
            }
        }
    }

    public void changeQuantity(Product product, int quantity) {
        if (quantity <= 0) {
            removeProduct(product);
        } else {
            Map.Entry<Product, Integer> itemEntry = selectItemInCart(product);
            if (itemEntry != null) {
                products.replace(itemEntry.getKey(), quantity);
            } else {
                products.put(product, quantity);
            }
        }
    }

    public void removeProduct(Product product) {
        Map.Entry<Product, Integer> itemEntry = selectItemInCart(product);
        if (itemEntry != null) {
            products.remove(itemEntry.getKey());
        } else {
            products.remove(product);
        }
    }

    public Integer countProductQuantity() {
        int productQuantity = 0;
        for (Map.Entry<Product, Integer> entry : products.entrySet()) {
            productQuantity += entry.getValue();
        }
        return productQuantity;
    }

    public Integer countItemQuantity() {
        return products.size();
    }

    public Double countTotalPayment() {
        double payment = 0;
        for (Map.Entry<Product, Integer> entry : products.entrySet()) {
            payment += entry.getKey().getPrice() * (double) entry.getValue();
        }
        return payment;
    }

    public void clearCart() {
        products.clear();
    }
}
