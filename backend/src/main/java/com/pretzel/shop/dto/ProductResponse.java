package com.pretzel.shop.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public record ProductResponse(
        Integer id,
        String name,
        BigDecimal price,
        String image,
        @JsonProperty("imageLarge") String imageLarge,
        @JsonProperty("imageThumb") String imageThumb,
        String description,
        @JsonProperty("fullDescription") String fullDescription) {}
