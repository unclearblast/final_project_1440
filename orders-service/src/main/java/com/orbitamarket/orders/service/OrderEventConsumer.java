package com.orbitamarket.orders.service;

import com.orbitamarket.orders.dto.OrderPaymentCompletedEvent;
import com.orbitamarket.orders.dto.OrderPaymentFailedEvent;
import com.orbitamarket.orders.entity.Order;
import com.orbitamarket.orders.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {

    private final OrderRepository orderRepository;

    @KafkaListener(topics = "order.payment.completed", groupId = "orders-group")
    @Transactional
    public void handlePaymentCompleted(OrderPaymentCompletedEvent event, Acknowledgment ack) {
        log.info("Payment completed for order {}", event.getOrderId());
        Optional<Order> optOrder = orderRepository.findById(event.getOrderId());
        if (optOrder.isPresent()) {
            Order order = optOrder.get();
            if (order.getStatus() == Order.OrderStatus.PAYMENT_PENDING) {
                order.setStatus(Order.OrderStatus.PAID);
                orderRepository.save(order);
            }
        }
        ack.acknowledge();
    }

    @KafkaListener(topics = "order.payment.failed", groupId = "orders-group")
    @Transactional
    public void handlePaymentFailed(OrderPaymentFailedEvent event, Acknowledgment ack) {
        log.info("Payment failed for order {}", event.getOrderId());
        Optional<Order> optOrder = orderRepository.findById(event.getOrderId());
        if (optOrder.isPresent()) {
            Order order = optOrder.get();
            if (order.getStatus() == Order.OrderStatus.PAYMENT_PENDING) {
                order.setStatus(Order.OrderStatus.PAYMENT_FAILED);
                order.setFailureReason(event.getReason());
                orderRepository.save(order);
            }
        }
        ack.acknowledge();
    }
}
