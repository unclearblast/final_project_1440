package com.orbitamarket.payments.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orbitamarket.payments.dto.*;
import com.orbitamarket.payments.entity.*;
import com.orbitamarket.payments.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentProcessor {

    private final AccountRepository accountRepository;
    private final InboxRepository inboxRepository;
    private final KafkaEventPublisher publisher;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final int MAX_RETRIES = 3;

    @KafkaListener(topics = "order.payment.requested", groupId = "payments-group")
    @Transactional
    public void handlePaymentRequest(String message) {   // принимаем строку
        OrderPaymentRequestedEvent event;
        try {
            event = objectMapper.readValue(message, OrderPaymentRequestedEvent.class);
        } catch (Exception e) {
            log.error("Failed to parse message: {}", message, e);
            return;
        }

        UUID eventId = event.getEventId();
        log.info("Processing payment request: eventId={}, orderId={}", eventId, event.getOrderId());

        if (inboxRepository.existsByEventId(eventId)) {
            log.info("Event already processed: {}", eventId);
            return;
        }

        inboxRepository.save(InboxMessage.create(eventId, event.getOrderId(), "OrderPaymentRequested"));

        Optional<Account> optAccount = accountRepository.findById(event.getUserId());
        if (optAccount.isEmpty()) {
            publisher.sendPaymentFailed(event, "ACCOUNT_NOT_FOUND");
            return;
        }

        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            Account account = accountRepository.findById(event.getUserId()).orElseThrow();
            if (account.getBalance() < event.getAmount()) {
                publisher.sendPaymentFailed(event, "INSUFFICIENT_BALANCE");
                return;
            }
            int rows = accountRepository.debit(account.getUserId(), event.getAmount(), account.getVersion());
            if (rows == 1) {
                Long newBalance = account.getBalance() - event.getAmount();
                publisher.sendPaymentCompleted(event, newBalance);
                return;
            }
            log.warn("Optimistic lock failure, attempt {}", attempt + 1);
        }
        publisher.sendPaymentFailed(event, "CONCURRENT_MODIFICATION_RETRY_EXHAUSTED");
    }
}
