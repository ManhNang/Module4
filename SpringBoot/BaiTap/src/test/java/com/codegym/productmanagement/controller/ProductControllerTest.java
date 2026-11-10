package com.codegym.productmanagement.controller;

import com.codegym.productmanagement.model.Product;
import com.codegym.productmanagement.service.IProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class ProductControllerTest {

    @Autowired
    private ProductController productController;

    @Autowired
    private IProductService productService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.standaloneSetup(productController).build();
    }

    @Test
    void testListProducts() throws Exception {
        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(view().name("product/index"))
                .andExpect(model().attributeExists("products"));
    }

    @Test
    void testShowCreateForm() throws Exception {
        mockMvc.perform(get("/products/create"))
                .andExpect(status().isOk())
                .andExpect(view().name("product/create"))
                .andExpect(model().attributeExists("product"));
    }

    @Test
    void testSaveProductValid() throws Exception {
        mockMvc.perform(post("/products/save")
                        .param("name", "Bàn phím cơ Test")
                        .param("price", "1500000")
                        .param("manufacturer", "Keychron")
                        .param("description", "Switch red gõ êm"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/products"))
                .andExpect(flash().attributeExists("successMessage"));
    }

    @Test
    void testSaveProductValidationFailure() throws Exception {
        mockMvc.perform(post("/products/save")
                        .param("name", "") // blank name
                        .param("price", "-500") // negative price
                        .param("manufacturer", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("product/create"))
                .andExpect(model().hasErrors());
    }

    @Test
    void testShowEditForm() throws Exception {
        Product product = productService.save(new Product("Sản phẩm Test Edit", 2000000.0, "Mô tả", "Hãng X"));

        mockMvc.perform(get("/products/" + product.getId() + "/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("product/edit"))
                .andExpect(model().attributeExists("product"));
    }

    @Test
    void testUpdateProduct() throws Exception {
        Product product = productService.save(new Product("Trước cập nhật", 1000000.0, "Mô tả cũ", "Hãng A"));

        mockMvc.perform(post("/products/update")
                        .param("id", String.valueOf(product.getId()))
                        .param("name", "Sau cập nhật")
                        .param("price", "1200000")
                        .param("manufacturer", "Hãng B")
                        .param("description", "Mô tả mới"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/products"))
                .andExpect(flash().attributeExists("successMessage"));
    }

    @Test
    void testDeleteProduct() throws Exception {
        Product product = productService.save(new Product("Sản phẩm cần xoá", 500000.0, "Xoá", "Hãng C"));

        mockMvc.perform(post("/products/delete")
                        .param("id", String.valueOf(product.getId())))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/products"))
                .andExpect(flash().attributeExists("successMessage"));

        assertFalse(productService.findById(product.getId()).isPresent());
    }

    @Test
    void testViewProduct() throws Exception {
        Product product = productService.save(new Product("Xem chi tiết", 3000000.0, "Chi tiết", "Hãng D"));

        mockMvc.perform(get("/products/" + product.getId() + "/view"))
                .andExpect(status().isOk())
                .andExpect(view().name("product/view"))
                .andExpect(model().attributeExists("product"));
    }
}
