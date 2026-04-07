package com.pretzel.shop.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AddToCartRequest(
        @JsonProperty("productId") Integer productId, Integer quantity) {}
