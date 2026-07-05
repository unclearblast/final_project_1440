package com.orbitamarket.payments.service;

import com.orbitamarket.payments.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class KafkaEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendPaymentCompleted(OrderPaymentRequestedEvent original, Long newBalance) {
        OrderPaymentCompletedEvent event = OrderPaymentCompletedEvent.builder()
                .eventId(UUID.randomUUID())
                .orderId(original.getOrderId())
                .userId(original.getUserId())
                .amount(original.getAmount())
                .newBalance(newBalance)
                .build();
        kafkaTemplate.send("order.payment.completed", event.getOrderId().toString(), event);
    }

    public void sendPaymentFailed(OrderPaymentRequestedEvent original, String reason) {
        OrderPaymentFailedEvent event = OrderPaymentFailedEvent.builder()
                .eventId(UUID.randomUUID())
                .orderId(original.getOrderId())
                .userId(original.getUserId())
                .reason(reason)
                .build();
        kafkaTemplate.send("order.payment.failed", event.getOrderId().toString(), event);
    }
}
