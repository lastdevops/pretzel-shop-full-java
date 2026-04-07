package com.pretzel.shop.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pretzel.shop.dto.OrderCreatedResponse;
import com.pretzel.shop.error.OrderCreationException;
import com.pretzel.shop.service.OrderService;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    @Test
    void createReturns201() throws Exception {
        when(orderService.create(any()))
                .thenReturn(
                        new OrderCreatedResponse(
                                10,
                                20,
                                new BigDecimal("14.00"),
                                new BigDecimal("5.00"),
                                new BigDecimal("9.00"),
                                "Order created successfully"));

        String body =
                """
                {
                  "fullName": "Jane",
                  "email": "j@example.com",
                  "address": "1 St",
                  "city": "NYC",
                  "zip": "10001",
                  "cartItems": [{"id": 1, "quantity": 2}],
                  "sessionId": "abc"
                }
                """;

        mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value(10))
                .andExpect(jsonPath("$.subtotal").value(9.0));
    }

    @Test
    void createReturns400WhenValidationFails() throws Exception {
        when(orderService.create(any())).thenThrow(new IllegalArgumentException("Cart is empty"));

        String body =
                """
                {
                  "fullName": "Jane",
                  "email": "j@example.com",
                  "address": "1 St",
                  "city": "NYC",
                  "zip": "10001",
                  "cartItems": [],
                  "sessionId": null
                }
                """;

        mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Cart is empty"));
    }

    @Test
    void createReturns500WithDetailsWhenOrderFails() throws Exception {
        when(orderService.create(any()))
                .thenThrow(new OrderCreationException("Failed to create order", "Product 99 not found", null));

        String body =
                """
                {
                  "fullName": "Jane",
                  "email": "j@example.com",
                  "address": "1 St",
                  "city": "NYC",
                  "zip": "10001",
                  "cartItems": [{"id": 99, "quantity": 1}],
                  "sessionId": null
                }
                """;

        mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Failed to create order"))
                .andExpect(jsonPath("$.details").value("Product 99 not found"));
    }

    @Test
    void getReturns404WhenMissing() throws Exception {
        when(orderService.findById(5)).thenReturn(null);

        mockMvc.perform(get("/api/orders/5"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Order not found"));
    }

    @Test
    void getReturnsOrderPayload() throws Exception {
        when(orderService.findById(1))
                .thenReturn(
                        Map.of(
                                "id",
                                1,
                                "customer_id",
                                2,
                                "total_amount",
                                new BigDecimal("8.00")));

        mockMvc.perform(get("/api/orders/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.customer_id").value(2));
    }
}
