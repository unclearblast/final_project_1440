package com.orbitamarket.orders.dto;

import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor @AllArgsConstructor
@Builder
public class OrderPaymentRequestedEvent {
    private UUID eventId;
    private UUID orderId;
    private String userId;
    private Long amount;
    private Instant occurredAt;
}
