package com.pretzel.shop.service;

import com.pretzel.shop.domain.Customer;
import com.pretzel.shop.domain.OrderItem;
import com.pretzel.shop.domain.Product;
import com.pretzel.shop.domain.ShopOrder;
import com.pretzel.shop.dto.CartItemRequest;
import com.pretzel.shop.dto.CreateOrderRequest;
import com.pretzel.shop.dto.OrderCreatedResponse;
import com.pretzel.shop.error.OrderCreationException;
import com.pretzel.shop.repo.CustomerRepository;
import com.pretzel.shop.repo.OrderItemRepository;
import com.pretzel.shop.repo.ProductRepository;
import com.pretzel.shop.repo.ShopOrderRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private static final BigDecimal SHIPPING = new BigDecimal("5.00");

    private final CustomerRepository customerRepository;
    private final ShopOrderRepository shopOrderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final CartService cartService;

    public OrderService(
            CustomerRepository customerRepository,
            ShopOrderRepository shopOrderRepository,
            OrderItemRepository orderItemRepository,
            ProductRepository productRepository,
            CartService cartService) {
        this.customerRepository = customerRepository;
        this.shopOrderRepository = shopOrderRepository;
        this.orderItemRepository = orderItemRepository;
        this.productRepository = productRepository;
        this.cartService = cartService;
    }

    @Transactional
    public OrderCreatedResponse create(CreateOrderRequest request) {
        validate(request);
        try {
            Customer customer = new Customer();
            customer.setFullName(request.fullName().trim());
            customer.setEmail(request.email().trim());
            customer.setAddress(request.address().trim());
            customer.setCity(request.city().trim());
            customer.setZip(request.zip().trim());
            customer = customerRepository.save(customer);

            List<Integer> productIds =
                    request.cartItems().stream().map(CartItemRequest::id).distinct().toList();
            List<Product> products = productRepository.findAllByIdIn(productIds);
            Map<Integer, BigDecimal> priceMap =
                    products.stream().collect(Collectors.toMap(Product::getId, Product::getPrice));

            BigDecimal subtotal = BigDecimal.ZERO;
            for (CartItemRequest item : request.cartItems()) {
                BigDecimal price = priceMap.get(item.id());
                if (price == null) {
                    throw new OrderCreationException(
                            "Failed to create order", "Product " + item.id() + " not found", null);
                }
                subtotal = subtotal.add(price.multiply(BigDecimal.valueOf(item.quantity())));
            }

            BigDecimal totalAmount = subtotal.add(SHIPPING);

            ShopOrder order = new ShopOrder();
            order.setCustomer(customer);
            order.setTotalAmount(totalAmount);
            order.setShippingCost(SHIPPING);
            order.setStatus("pending");
            order = shopOrderRepository.save(order);

            for (CartItemRequest item : request.cartItems()) {
                BigDecimal unitPrice = priceMap.get(item.id());
                OrderItem line = new OrderItem();
                line.setOrder(order);
                line.setProduct(productRepository.getReferenceById(item.id()));
                line.setQuantity(item.quantity());
                line.setPrice(unitPrice);
                orderItemRepository.save(line);
            }

            cartService.deleteCartKey(request.sessionId());

            return new OrderCreatedResponse(
                    order.getId(),
                    customer.getId(),
                    totalAmount,
                    SHIPPING,
                    subtotal,
                    "Order created successfully");
        } catch (OrderCreationException e) {
            throw e;
        } catch (Exception e) {
            throw new OrderCreationException("Failed to create order", e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Object> findById(Integer id) {
        return shopOrderRepository
                .findById(id)
                .map(order -> toDetailMap(order))
                .orElse(null);
    }

    private static void validate(CreateOrderRequest request) {
        if (request.fullName() == null
                || request.fullName().isBlank()
                || request.email() == null
                || request.email().isBlank()
                || request.address() == null
                || request.address().isBlank()
                || request.city() == null
                || request.city().isBlank()
                || request.zip() == null
                || request.zip().isBlank()) {
            throw new IllegalArgumentException("Missing required customer information");
        }
        if (request.cartItems() == null || request.cartItems().isEmpty()) {
            throw new IllegalArgumentException("Cart is empty");
        }
    }

    private Map<String, Object> toDetailMap(ShopOrder order) {
        Customer c = order.getCustomer();
        List<OrderItem> lines = orderItemRepository.findByOrder_IdOrderById(order.getId());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", order.getId());
        body.put("customer_id", c.getId());
        body.put("total_amount", order.getTotalAmount());
        body.put("shipping_cost", order.getShippingCost());
        body.put("status", order.getStatus());
        body.put("created_at", order.getCreatedAt());
        body.put("full_name", c.getFullName());
        body.put("email", c.getEmail());
        body.put("address", c.getAddress());
        body.put("city", c.getCity());
        body.put("zip", c.getZip());

        List<Map<String, Object>> items = new ArrayList<>();
        for (OrderItem oi : lines) {
            Product p = oi.getProduct();
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", oi.getId());
            row.put("order_id", order.getId());
            row.put("product_id", p.getId());
            row.put("quantity", oi.getQuantity());
            row.put("price", oi.getPrice());
            row.put("created_at", oi.getCreatedAt());
            row.put("name", p.getName());
            row.put("imageThumb", p.getImageThumb());
            items.add(row);
        }
        body.put("items", items);
        return body;
    }
}
