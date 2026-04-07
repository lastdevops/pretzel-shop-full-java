package com.pretzel.shop.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public record OrderCreatedResponse(
        @JsonProperty("orderId") Integer orderId,
        @JsonProperty("customerId") Integer customerId,
        @JsonProperty("totalAmount") BigDecimal totalAmount,
        @JsonProperty("shippingCost") BigDecimal shippingCost,
        BigDecimal subtotal,
        String message) {}
