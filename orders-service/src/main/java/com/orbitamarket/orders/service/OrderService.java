package com.orbitamarket.orders.service;

import com.orbitamarket.orders.dto.*;
import com.orbitamarket.orders.entity.Order;
import com.orbitamarket.orders.entity.OutboxEvent;
import com.orbitamarket.orders.repository.OrderRepository;
import com.orbitamarket.orders.repository.OutboxRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public OrderResponse createOrder(String userId, CreateOrderRequest request) {
        // Валидация типа продукта
        if (!List.of("ARCHIVE", "TASKING", "MONITORING").contains(request.getProductType().toUpperCase())) {
            throw new IllegalArgumentException("UNKNOWN_PRODUCT_TYPE");
        }

        Order order = Order.builder()
                .userId(userId)
                .productType(request.getProductType().toUpperCase())
                .status(Order.OrderStatus.PAYMENT_PENDING)
                .price(request.getPrice())
                .payload(request.getPayload())
                .build();
        order = orderRepository.save(order);

        // Создание события Outbox
        OrderPaymentRequestedEvent event = OrderPaymentRequestedEvent.builder()
                .eventId(UUID.randomUUID())
                .orderId(order.getOrderId())
                .userId(userId)
                .amount(request.getPrice())
                .occurredAt(Instant.now())
                .build();

        try {
            String payload = objectMapper.writeValueAsString(event);
            OutboxEvent outbox = OutboxEvent.builder()
                    .aggregateId(order.getOrderId())
                    .eventType("OrderPaymentRequested")
                    .payload(payload)
                    .published(false)
                    .build();
            outboxRepository.save(outbox);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize event", e);
        }

        return OrderResponse.from(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getUserOrders(String userId) {
        return orderRepository.findByUserId(userId).stream()
                .map(OrderResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(UUID orderId, String userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("ORDER_NOT_FOUND"));
        if (!order.getUserId().equals(userId)) {
            throw new SecurityException("Access denied");
        }
        return OrderResponse.from(order);
    }
}
