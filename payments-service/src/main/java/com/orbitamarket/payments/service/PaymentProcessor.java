package com.orbitamarket.payments.service;

import com.orbitamarket.common.dto.OrderPaymentRequestedEvent;
import com.orbitamarket.payments.entity.Account;
import com.orbitamarket.payments.entity.InboxMessage;
import com.orbitamarket.payments.repository.AccountRepository;
import com.orbitamarket.payments.repository.InboxRepository;
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

    private static final int MAX_RETRIES = 3;

    @KafkaListener(topics = "order.payment.requested", groupId = "payments-group")
    @Transactional
    public void handlePaymentRequest(OrderPaymentRequestedEvent event) {
        UUID eventId = event.getEventId();
        log.info("Processing payment request: eventId={}, orderId={}", eventId, event.getOrderId());

        // Идемпотентность: если событие уже обработано — выходим
        if (inboxRepository.existsByEventId(eventId)) {
            log.info("Event already processed: {}", eventId);
            return;
        }

        // Сохраняем в inbox перед обработкой
        inboxRepository.save(InboxMessage.create(eventId, event.getOrderId(), "OrderPaymentRequested"));

        Optional<Account> optAccount = accountRepository.findById(event.getUserId());
        if (optAccount.isEmpty()) {
            publisher.sendPaymentFailed(event, "ACCOUNT_NOT_FOUND");
            return;
        }

        // Попытки списания с optimistic lock
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
