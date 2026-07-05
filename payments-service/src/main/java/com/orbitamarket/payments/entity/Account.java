package com.orbitamarket.payments.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "accounts")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Account {

    @Id
    @Column(name = "user_id", length = 64)
    private String userId;

    @Column(nullable = false)
    private Long balance;

    @Version
    private Long version;

    public Account(String userId, Long balance) {
        this.userId = userId;
        this.balance = balance;
    }
}
