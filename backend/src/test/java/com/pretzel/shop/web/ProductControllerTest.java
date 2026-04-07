package com.pretzel.shop.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pretzel.shop.dto.ProductResponse;
import com.pretzel.shop.service.ProductService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ProductController.class)
@Import(GlobalExceptionHandler.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductService productService;

    @Test
    void listReturnsProducts() throws Exception {
        when(productService.findAll())
                .thenReturn(
                        List.of(new ProductResponse(
                                1,
                                "Classic",
                                new BigDecimal("3.00"),
                                "img",
                                "lg",
                                "th",
                                "short",
                                "full")));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].imageLarge").value("lg"))
                .andExpect(jsonPath("$[0].fullDescription").value("full"));
    }

    @Test
    void getOneReturns404WhenMissing() throws Exception {
        when(productService.findById(99)).thenReturn(null);

        mockMvc.perform(get("/api/products/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Product not found"));
    }

    @Test
    void listReturns500WhenServiceFails() throws Exception {
        when(productService.findAll()).thenThrow(new RuntimeException("db down"));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Failed to fetch products"));
    }

    @Test
    void getOneReturnsProduct() throws Exception {
        when(productService.findById(1))
                .thenReturn(
                        new ProductResponse(
                                1,
                                "Classic",
                                new BigDecimal("3.00"),
                                "img",
                                "lg",
                                "th",
                                "short",
                                "full"));

        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Classic"));
    }
}
