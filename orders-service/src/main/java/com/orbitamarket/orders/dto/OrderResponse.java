package com.orbitamarket.orders.dto;

import com.orbitamarket.orders.entity.Order;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor @AllArgsConstructor
@Builder
public class OrderResponse {
    private UUID orderId;
    private String userId;
    private String productType;
    private String status;
    private Long price;
    private String payload;
    private String failureReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static OrderResponse from(Order order) {
        return OrderResponse.builder()
                .orderId(order.getOrderId())
                .userId(order.getUserId())
                .productType(order.getProductType())
                .status(order.getStatus().name())
                .price(order.getPrice())
                .payload(order.getPayload())
                .failureReason(order.getFailureReason())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}
