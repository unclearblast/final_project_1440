package com.orbitamarket.common.dto;

import lombok.*;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderPaymentCompletedEvent {
    private UUID eventId;
    private UUID orderId;
    private String userId;
    private Long amount;
    private Long newBalance;
}
