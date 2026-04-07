package com.pretzel.shop.web;

import com.pretzel.shop.dto.ProductResponse;
import com.pretzel.shop.service.ProductService;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public List<ProductResponse> list() {
        try {
            return productService.findAll();
        } catch (Exception e) {
            throw new ProductFetchException("Failed to fetch products", e);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getOne(@PathVariable Integer id) {
        try {
            ProductResponse p = productService.findById(id);
            if (p == null) {
                return ResponseEntity.status(404).body(Map.of("error", "Product not found"));
            }
            return ResponseEntity.ok(p);
        } catch (Exception e) {
            throw new ProductFetchException("Failed to fetch product", e);
        }
    }

    static final class ProductFetchException extends RuntimeException {
        ProductFetchException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
