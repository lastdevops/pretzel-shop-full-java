package com.pretzel.shop.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pretzel.shop.dto.CartLine;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CartServiceTest {

    @Mock
    private StringRedisTemplate redis;

    @Mock
    private ValueOperations<String, String> valueOps;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private CartService cartService;

    @BeforeEach
    void setUp() {
        when(redis.opsForValue()).thenReturn(valueOps);
        cartService = new CartService(redis, objectMapper);
    }

    @Test
    void getCartReturnsEmptyWhenMissing() {
        when(valueOps.get("cart:s1")).thenReturn(null);
        assertThat(cartService.getCart("s1")).isEmpty();
    }

    @Test
    void addItemMergesQuantity() throws Exception {
        when(valueOps.get("cart:s1")).thenReturn("[{\"id\":1,\"quantity\":2}]");

        List<CartLine> result = cartService.addItem("s1", 1, 3);

        assertThat(result).containsExactly(new CartLine(1, 5));
        ArgumentCaptor<String> json = ArgumentCaptor.forClass(String.class);
        verify(valueOps).set(eq("cart:s1"), json.capture(), eq(Duration.ofSeconds(CartService.CART_TTL_SECONDS)));
        assertThat(json.getValue()).contains("\"id\":1");
        assertThat(json.getValue()).contains("\"quantity\":5");
    }

    @Test
    void addItemAppendsNewLine() throws Exception {
        when(valueOps.get("cart:s1")).thenReturn("[{\"id\":1,\"quantity\":1}]");

        List<CartLine> result = cartService.addItem("s1", 2, 1);

        assertThat(result).hasSize(2);
        verify(valueOps).set(eq("cart:s1"), anyString(), eq(Duration.ofSeconds(CartService.CART_TTL_SECONDS)));
    }

    @Test
    void addItemRequiresProductId() {
        assertThatThrownBy(() -> cartService.addItem("s1", null, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Product ID");
    }

    @Test
    void updateQuantityRemovesWhenZero() throws Exception {
        when(valueOps.get("cart:s1")).thenReturn("[{\"id\":1,\"quantity\":2}]");

        List<CartLine> result = cartService.updateQuantity("s1", 1, 0);

        assertThat(result).isEmpty();
        verify(valueOps).set(eq("cart:s1"), anyString(), eq(Duration.ofSeconds(CartService.CART_TTL_SECONDS)));
    }

    @Test
    void removeItemThrowsWhenMissing() {
        when(valueOps.get("cart:s1")).thenReturn(null);
        assertThatThrownBy(() -> cartService.removeItem("s1", 1)).isInstanceOf(CartService.CartNotFoundException.class);
    }

    @Test
    void clearDeletesKey() {
        cartService.clear("s1");
        verify(redis).delete("cart:s1");
    }

    @Test
    void deleteCartKeyIgnoresBlankSession() {
        cartService.deleteCartKey("  ");
        verify(redis, org.mockito.Mockito.never()).delete(anyString());
    }
}
