package com.pretzel.shop.web;

import com.pretzel.shop.dto.CreateOrderRequest;
import com.pretzel.shop.dto.OrderCreatedResponse;
import com.pretzel.shop.error.OrderCreationException;
import com.pretzel.shop.service.OrderService;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody CreateOrderRequest request) {
        try {
            OrderCreatedResponse created = orderService.create(request);
            return ResponseEntity.status(201).body(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (OrderCreationException e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to create order", "details", e.getDetails() != null ? e.getDetails() : ""));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable Integer id) {
        try {
            Map<String, Object> order = orderService.findById(id);
            if (order == null) {
                return ResponseEntity.status(404).body(Map.of("error", "Order not found"));
            }
            return ResponseEntity.ok(order);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to fetch order"));
        }
    }
}
