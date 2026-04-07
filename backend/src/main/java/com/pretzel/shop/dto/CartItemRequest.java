package com.pretzel.shop.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CartItemRequest(Integer id, Integer quantity) {}
