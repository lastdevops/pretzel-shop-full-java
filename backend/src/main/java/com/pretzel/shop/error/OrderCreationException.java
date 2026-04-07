package com.pretzel.shop.error;

public class OrderCreationException extends RuntimeException {

    private final String details;

    public OrderCreationException(String message, String details, Throwable cause) {
        super(message, cause);
        this.details = details;
    }

    public String getDetails() {
        return details;
    }
}
