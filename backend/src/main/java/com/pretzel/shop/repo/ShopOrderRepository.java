package com.pretzel.shop.repo;

import com.pretzel.shop.domain.ShopOrder;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShopOrderRepository extends JpaRepository<ShopOrder, Integer> {}
