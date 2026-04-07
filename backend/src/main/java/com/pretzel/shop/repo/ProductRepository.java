package com.pretzel.shop.repo;

import com.pretzel.shop.domain.Product;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Integer> {

    List<Product> findAllByOrderByIdAsc();

    List<Product> findAllByIdIn(Collection<Integer> ids);
}
