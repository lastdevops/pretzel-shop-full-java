package com.pretzel.shop.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pretzel.shop.dto.CartLine;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class CartService {

    public static final int CART_TTL_SECONDS = 86400;

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public CartService(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    public List<CartLine> getCart(String sessionId) {
        String raw = redis.opsForValue().get(cartKey(sessionId));
        return parseCart(raw);
    }

    public List<CartLine> addItem(String sessionId, Integer productId, Integer quantity) {
        if (productId == null) {
            throw new IllegalArgumentException("Product ID is required");
        }
        if (quantity == null || quantity < 1) {
            throw new IllegalArgumentException("Valid quantity is required (must be >= 1)");
        }

        List<CartLine> cart = new ArrayList<>(loadCart(sessionId));
        int idx = indexOf(cart, productId);
        if (idx >= 0) {
            CartLine existing = cart.get(idx);
            cart.set(idx, new CartLine(existing.id(), existing.quantity() + quantity));
        } else {
            cart.add(new CartLine(productId, quantity));
        }
        save(sessionId, cart);
        return cart;
    }

    public List<CartLine> updateQuantity(String sessionId, Integer productId, Integer quantity) {
        if (quantity == null || quantity < 0) {
            throw new IllegalArgumentException("Valid quantity is required");
        }
        if (productId == null) {
            throw new IllegalArgumentException("Product ID must be a valid number");
        }

        String raw = redis.opsForValue().get(cartKey(sessionId));
        if (raw == null) {
            throw new CartNotFoundException();
        }
        List<CartLine> cart = parseCartStrict(raw);
        int idx = indexOf(cart, productId);
        if (idx < 0) {
            throw new CartItemNotFoundException();
        }
        if (quantity == 0) {
            cart.remove(idx);
        } else {
            cart.set(idx, new CartLine(productId, quantity));
        }
        save(sessionId, cart);
        return cart;
    }

    public List<CartLine> removeItem(String sessionId, Integer productId) {
        if (productId == null) {
            throw new IllegalArgumentException("Product ID must be a valid number");
        }
        String raw = redis.opsForValue().get(cartKey(sessionId));
        if (raw == null) {
            throw new CartNotFoundException();
        }
        List<CartLine> cart = parseCartStrict(raw);
        boolean removed = cart.removeIf(line -> line.id().equals(productId));
        if (!removed) {
            throw new CartItemNotFoundException();
        }
        save(sessionId, cart);
        return cart;
    }

    public List<CartLine> clear(String sessionId) {
        redis.delete(cartKey(sessionId));
        return List.of();
    }

    public void deleteCartKey(String sessionId) {
        if (sessionId != null && !sessionId.isBlank()) {
            redis.delete(cartKey(sessionId));
        }
    }

    private List<CartLine> loadCart(String sessionId) {
        String raw = redis.opsForValue().get(cartKey(sessionId));
        return parseCart(raw);
    }

    private void save(String sessionId, List<CartLine> cart) {
        try {
            String json = objectMapper.writeValueAsString(cart);
            redis.opsForValue().set(cartKey(sessionId), json, Duration.ofSeconds(CART_TTL_SECONDS));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to save cart to storage", e);
        }
    }

    private List<CartLine> parseCart(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        try {
            List<CartLine> list = objectMapper.readValue(raw, new TypeReference<>() {});
            return normalize(list);
        } catch (Exception e) {
            return List.of();
        }
    }

    private List<CartLine> parseCartStrict(String raw) {
        try {
            List<CartLine> list = objectMapper.readValue(raw, new TypeReference<>() {});
            return normalize(list);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse cart data");
        }
    }

    private static List<CartLine> normalize(List<CartLine> list) {
        if (list == null) {
            return new ArrayList<>();
        }
        List<CartLine> out = new ArrayList<>();
        for (CartLine line : list) {
            if (line == null || line.id() == null || line.quantity() == null) {
                continue;
            }
            out.add(new CartLine(line.id(), line.quantity()));
        }
        return out;
    }

    private static int indexOf(List<CartLine> cart, int productId) {
        for (int i = 0; i < cart.size(); i++) {
            if (cart.get(i).id() == productId) {
                return i;
            }
        }
        return -1;
    }

    private static String cartKey(String sessionId) {
        return "cart:" + sessionId;
    }

    public static final class CartNotFoundException extends RuntimeException {}

    public static final class CartItemNotFoundException extends RuntimeException {}
}
