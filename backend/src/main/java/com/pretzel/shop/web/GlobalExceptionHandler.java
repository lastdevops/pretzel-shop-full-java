package com.pretzel.shop.web;

import com.pretzel.shop.web.ProductController.ProductFetchException;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.NoHandlerFoundException;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<Map<String, String>> notFoundRoute(NoHandlerFoundException ex) {
        return ResponseEntity.status(404).body(Map.of("error", "Route not found"));
    }

    @ExceptionHandler(ProductFetchException.class)
    public ResponseEntity<Map<String, String>> productFetch(ProductFetchException ex) {
        return ResponseEntity.internalServerError().body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> fallback(Exception ex) {
        return ResponseEntity.internalServerError().body(Map.of("error", "Internal server error"));
    }
}
