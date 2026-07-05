package com.orbitamarket.orders.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orbitamarket.orders.dto.OrderPaymentRequestedEvent;
import com.orbitamarket.orders.entity.OutboxEvent;
import com.orbitamarket.orders.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisher {

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> events = outboxRepository.findByPublishedFalse();
        for (OutboxEvent event : events) {
            try {
                // Десериализуем строку обратно в объект события
                OrderPaymentRequestedEvent requestEvent =
                        objectMapper.readValue(event.getPayload(), OrderPaymentRequestedEvent.class);
                // Отправляем объект – он будет сериализован в JSON с заголовками типа
                kafkaTemplate.send("order.payment.requested",
                        requestEvent.getOrderId().toString(), requestEvent);
                event.setPublished(true);
                outboxRepository.save(event);
                log.info("Published event for order {}", requestEvent.getOrderId());
            } catch (Exception e) {
                log.error("Failed to publish event: {}", event.getId(), e);
            }
        }
    }
}
