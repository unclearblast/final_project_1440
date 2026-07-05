package com.orbitamarket.payments.service;

import com.orbitamarket.payments.entity.Account;
import com.orbitamarket.payments.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;

    @Transactional
    public Account createAccount(String userId) {
        return accountRepository.findById(userId)
                .orElseGet(() -> accountRepository.save(new Account(userId, 0L)));
    }

    @Transactional
    public Account topUp(String userId, Long amount) {
        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("INVALID_AMOUNT");
        }
        Account account = accountRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("ACCOUNT_NOT_FOUND"));
        account.setBalance(account.getBalance() + amount);
        return accountRepository.save(account);
    }

    @Transactional(readOnly = true)
    public Long getBalance(String userId) {
        Account account = accountRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("ACCOUNT_NOT_FOUND"));
        return account.getBalance();
    }
}
