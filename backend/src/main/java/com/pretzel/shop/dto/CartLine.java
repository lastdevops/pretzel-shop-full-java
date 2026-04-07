package com.pretzel.shop.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CartLine(Integer id, Integer quantity) {}
