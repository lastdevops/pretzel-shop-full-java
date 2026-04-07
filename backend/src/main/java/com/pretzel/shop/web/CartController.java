package com.pretzel.shop.web;

import com.pretzel.shop.dto.AddToCartRequest;
import com.pretzel.shop.dto.CartLine;
import com.pretzel.shop.dto.UpdateCartItemRequest;
import com.pretzel.shop.service.CartService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public List<CartLine> get(HttpServletRequest request, HttpServletResponse response) {
        String sessionId = ensureSession(request, response);
        return cartService.getCart(sessionId);
    }

    @PostMapping
    public ResponseEntity<?> add(
            @RequestBody(required = false) AddToCartRequest body,
            HttpServletRequest request,
            HttpServletResponse response) {
        String sessionId = ensureSession(request, response);
        if (body == null) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", "Product ID is required"));
        }
        try {
            List<CartLine> cart = cartService.addItem(sessionId, body.productId(), body.quantity());
            return ResponseEntity.ok(cart);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(java.util.Map.of("error", e.getMessage() != null ? e.getMessage() : "Failed to add item to cart"));
        }
    }

    @PutMapping("/{productId}")
    public ResponseEntity<?> update(
            @PathVariable Integer productId,
            @RequestBody(required = false) UpdateCartItemRequest body,
            HttpServletRequest request,
            HttpServletResponse response) {
        String sessionId = ensureSession(request, response);
        if (body == null) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", "Valid quantity is required"));
        }
        try {
            List<CartLine> cart = cartService.updateQuantity(sessionId, productId, body.quantity());
            return ResponseEntity.ok(cart);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        } catch (CartService.CartNotFoundException e) {
            return ResponseEntity.status(404).body(java.util.Map.of("error", "Cart not found"));
        } catch (CartService.CartItemNotFoundException e) {
            return ResponseEntity.status(404).body(java.util.Map.of("error", "Item not found in cart"));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(500).body(java.util.Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(java.util.Map.of("error", "Failed to update cart"));
        }
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<?> removeItem(
            @PathVariable Integer productId,
            HttpServletRequest request,
            HttpServletResponse response) {
        String sessionId = ensureSession(request, response);
        try {
            List<CartLine> cart = cartService.removeItem(sessionId, productId);
            return ResponseEntity.ok(cart);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        } catch (CartService.CartNotFoundException e) {
            return ResponseEntity.status(404).body(java.util.Map.of("error", "Cart not found"));
        } catch (CartService.CartItemNotFoundException e) {
            return ResponseEntity.status(404).body(java.util.Map.of("error", "Item not found in cart"));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(500).body(java.util.Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(java.util.Map.of("error", "Failed to remove item from cart"));
        }
    }

    @DeleteMapping
    public ResponseEntity<?> clear(HttpServletRequest request, HttpServletResponse response) {
        String sessionId = ensureSession(request, response);
        try {
            return ResponseEntity.ok(cartService.clear(sessionId));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(java.util.Map.of("error", "Failed to clear cart"));
        }
    }

    private static String ensureSession(HttpServletRequest request, HttpServletResponse response) {
        String id = request.getHeader("X-Session-Id");
        if (id == null || id.isBlank()) {
            id = UUID.randomUUID().toString();
        }
        response.setHeader("X-Session-Id", id);
        return id;
    }
}
