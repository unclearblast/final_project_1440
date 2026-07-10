package com.orbitamarket.common.dto;

import lombok.*;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderPaymentFailedEvent {
    private UUID eventId;
    private UUID orderId;
    private String userId;
    private String reason;
}
