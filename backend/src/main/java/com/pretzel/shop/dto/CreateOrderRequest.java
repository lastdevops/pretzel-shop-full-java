package com.pretzel.shop.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record CreateOrderRequest(
        @NotBlank @JsonProperty("fullName") String fullName,
        @NotBlank String email,
        @NotBlank String address,
        @NotBlank String city,
        @NotBlank String zip,
        @NotEmpty @Valid List<CartItemRequest> cartItems,
        String sessionId) {}
