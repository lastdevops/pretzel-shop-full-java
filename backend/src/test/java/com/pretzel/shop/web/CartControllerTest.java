package com.pretzel.shop.web;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pretzel.shop.dto.CartLine;
import com.pretzel.shop.service.CartService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = CartController.class)
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CartService cartService;

    @Test
    void getCartSetsSessionHeader() throws Exception {
        when(cartService.getCart(anyString())).thenReturn(List.of());

        mockMvc.perform(get("/api/cart"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Session-Id"));
    }

    @Test
    void addToCartUsesExistingSessionHeader() throws Exception {
        when(cartService.addItem(eq("sess-1"), eq(1), eq(2)))
                .thenReturn(List.of(new CartLine(1, 2)));

        mockMvc.perform(
                        post("/api/cart")
                                .header("X-Session-Id", "sess-1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"productId\":1,\"quantity\":2}"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Session-Id", "sess-1"))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].quantity").value(2));

        verify(cartService).addItem("sess-1", 1, 2);
    }

    @Test
    void addReturns400WhenInvalid() throws Exception {
        when(cartService.addItem(anyString(), isNull(), eq(1)))
                .thenThrow(new IllegalArgumentException("Product ID is required"));

        mockMvc.perform(
                        post("/api/cart")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"productId\":null,\"quantity\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Product ID is required"));
    }

    @Test
    void updateReturns404WhenCartMissing() throws Exception {
        when(cartService.updateQuantity(anyString(), eq(1), eq(1)))
                .thenThrow(new CartService.CartNotFoundException());

        mockMvc.perform(
                        put("/api/cart/1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"quantity\":1}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Cart not found"));
    }

    @Test
    void deleteAllClearsCart() throws Exception {
        when(cartService.clear(anyString())).thenReturn(List.of());

        mockMvc.perform(delete("/api/cart")).andExpect(status().isOk()).andExpect(jsonPath("$").isArray());
    }
}
