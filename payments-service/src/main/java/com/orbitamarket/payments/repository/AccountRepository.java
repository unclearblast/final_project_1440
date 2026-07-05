package com.orbitamarket.payments.repository;

import com.orbitamarket.payments.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, String> {

    Optional<Account> findByUserId(String userId);

    @Modifying
    @Query("UPDATE Account a SET a.balance = a.balance - :amount, a.version = a.version + 1 " +
           "WHERE a.userId = :userId AND a.version = :version AND a.balance >= :amount")
    int debit(@Param("userId") String userId,
              @Param("amount") Long amount,
              @Param("version") Long version);
}
