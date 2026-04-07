package com.pretzel.shop.repo;

import com.pretzel.shop.domain.OrderItem;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, Integer> {

    @EntityGraph(attributePaths = "product")
    List<OrderItem> findByOrder_IdOrderById(Integer orderId);
}
