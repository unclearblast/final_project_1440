package com.orbitamarket.payments.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "inbox")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class InboxMessage {

    @Id
    @Column(name = "event_id", columnDefinition = "uuid")
    private UUID eventId;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "event_type", length = 50)
    private String eventType;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(length = 20)
    private String status; // PROCESSED, DUPLICATE

    public static InboxMessage create(UUID eventId, UUID orderId, String eventType) {
        InboxMessage msg = new InboxMessage();
        msg.eventId = eventId;
        msg.orderId = orderId;
        msg.eventType = eventType;
        msg.status = "PROCESSED";
        msg.processedAt = LocalDateTime.now();
        return msg;
    }
}
