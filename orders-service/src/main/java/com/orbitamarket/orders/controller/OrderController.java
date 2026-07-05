package com.orbitamarket.orders.controller;

import com.orbitamarket.orders.dto.*;
import com.orbitamarket.orders.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<?> createOrder(@RequestHeader("X-User-Id") String userId,
                                         @Valid @RequestBody CreateOrderRequest request) {
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error_code", "MISSING_USER_ID",
                    "message", "X-User-Id header is required"
            ));
        }
        try {
            OrderResponse response = orderService.createOrder(userId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            return switch (e.getMessage()) {
                case "UNKNOWN_PRODUCT_TYPE" -> ResponseEntity.badRequest().body(Map.of(
                        "error_code", "UNKNOWN_PRODUCT_TYPE",
                        "message", "Unsupported product type"
                ));
                default -> ResponseEntity.internalServerError().build();
            };
        }
    }

    @GetMapping
    public ResponseEntity<List<OrderResponse>> getUserOrders(@RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(orderService.getUserOrders(userId));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<?> getOrder(@RequestHeader("X-User-Id") String userId,
                                      @PathVariable UUID orderId) {
        try {
            OrderResponse order = orderService.getOrder(orderId, userId);
            return ResponseEntity.ok(order);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of(
                    "error_code", "ORDER_NOT_FOUND",
                    "message", "Order not found"
            ));
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(Map.of(
                    "error_code", "ACCESS_DENIED",
                    "message", "Access denied"
            ));
        }
    }
}
