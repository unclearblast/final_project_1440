package com.orbitamarket.payments.repository;

import com.orbitamarket.payments.entity.InboxMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface InboxRepository extends JpaRepository<InboxMessage, UUID> {
    boolean existsByEventId(UUID eventId);
}
