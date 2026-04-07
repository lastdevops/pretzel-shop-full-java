package com.pretzel.shop.service;

import com.pretzel.shop.domain.Product;
import com.pretzel.shop.dto.ProductResponse;
import com.pretzel.shop.repo.ProductRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> findAll() {
        return productRepository.findAllByOrderByIdAsc().stream().map(ProductService::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ProductResponse findById(Integer id) {
        return productRepository.findById(id).map(ProductService::toResponse).orElse(null);
    }

    private static ProductResponse toResponse(Product p) {
        return new ProductResponse(
                p.getId(),
                p.getName(),
                p.getPrice(),
                p.getImage(),
                p.getImageLarge(),
                p.getImageThumb(),
                p.getDescription(),
                p.getFullDescription());
    }
}
