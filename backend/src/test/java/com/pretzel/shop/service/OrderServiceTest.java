package com.pretzel.shop.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pretzel.shop.domain.Customer;
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
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private ShopOrderRepository shopOrderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CartService cartService;

    @InjectMocks
    private OrderService orderService;

    @Test
    void createComputesTotalsAndClearsCart() {
        CreateOrderRequest req =
                new CreateOrderRequest(
                        "Jane Doe",
                        "j@example.com",
                        "1 Main",
                        "NYC",
                        "10001",
                        List.of(new CartItemRequest(1, 2), new CartItemRequest(2, 1)),
                        "sess-1");

        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> {
            Customer c = inv.getArgument(0);
            c.setId(9);
            return c;
        });

        Product p1 = new Product();
        p1.setId(1);
        p1.setPrice(new BigDecimal("3.00"));
        Product p2 = new Product();
        p2.setId(2);
        p2.setPrice(new BigDecimal("3.50"));
        when(productRepository.findAllByIdIn(anyList())).thenReturn(List.of(p1, p2));

        when(shopOrderRepository.save(any(ShopOrder.class))).thenAnswer(inv -> {
            ShopOrder o = inv.getArgument(0);
            o.setId(100);
            return o;
        });

        when(productRepository.getReferenceById(1)).thenReturn(p1);
        when(productRepository.getReferenceById(2)).thenReturn(p2);

        OrderCreatedResponse res = orderService.create(req);

        assertThat(res.orderId()).isEqualTo(100);
        assertThat(res.customerId()).isEqualTo(9);
        assertThat(res.shippingCost()).isEqualByComparingTo(new BigDecimal("5.00"));
        assertThat(res.subtotal()).isEqualByComparingTo(new BigDecimal("9.50"));
        assertThat(res.totalAmount()).isEqualByComparingTo(new BigDecimal("14.50"));

        verify(orderItemRepository, org.mockito.Mockito.times(2)).save(any());
        verify(cartService).deleteCartKey("sess-1");
    }

    @Test
    void createThrowsWhenProductMissing() {
        CreateOrderRequest req =
                new CreateOrderRequest(
                        "Jane Doe",
                        "j@example.com",
                        "1 Main",
                        "NYC",
                        "10001",
                        List.of(new CartItemRequest(99, 1)),
                        null);

        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> {
            Customer c = inv.getArgument(0);
            c.setId(1);
            return c;
        });
        when(productRepository.findAllByIdIn(anyList())).thenReturn(List.of());

        assertThatThrownBy(() -> orderService.create(req))
                .isInstanceOf(OrderCreationException.class)
                .hasMessage("Failed to create order");

        verify(shopOrderRepository, never()).save(any());
    }

    @Test
    void validateRejectsEmptyCart() {
        CreateOrderRequest req =
                new CreateOrderRequest("A", "b@c.d", "addr", "city", "zip", List.of(), null);

        assertThatThrownBy(() -> orderService.create(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cart is empty");
    }

    @Test
    void findByIdReturnsNullWhenMissing() {
        when(shopOrderRepository.findById(5)).thenReturn(java.util.Optional.empty());
        assertThat(orderService.findById(5)).isNull();
    }

    @Test
    void findByIdBuildsDetailMap() {
        Customer c = new Customer();
        c.setId(2);
        c.setFullName("Jane");
        c.setEmail("j@e.com");
        c.setAddress("a");
        c.setCity("c");
        c.setZip("z");

        ShopOrder o = new ShopOrder();
        o.setId(10);
        o.setCustomer(c);
        o.setTotalAmount(new BigDecimal("11.00"));
        o.setShippingCost(new BigDecimal("5.00"));
        o.setStatus("pending");
        o.setCreatedAt(java.time.Instant.parse("2024-01-01T00:00:00Z"));

        when(shopOrderRepository.findById(10)).thenReturn(java.util.Optional.of(o));
        when(orderItemRepository.findByOrder_IdOrderById(10)).thenReturn(List.of());

        Map<String, Object> map = orderService.findById(10);

        assertThat(map).containsEntry("id", 10).containsEntry("full_name", "Jane");
    }
}
