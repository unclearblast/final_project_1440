package com.orbitamarket.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
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
