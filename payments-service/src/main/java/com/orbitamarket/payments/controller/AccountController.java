package com.orbitamarket.payments.controller;

import com.orbitamarket.payments.dto.AccountResponse;
import com.orbitamarket.payments.service.AccountService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    public ResponseEntity<?> createAccount(@RequestHeader("X-User-Id") String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("MISSING_USER_ID");
        }
        var account = accountService.createAccount(userId);
        return ResponseEntity.ok(new AccountResponse(account.getUserId(), account.getBalance(), "geocredits"));
    }

    @PostMapping("/top-up")
    public ResponseEntity<?> topUp(@RequestHeader("X-User-Id") String userId,
                                   @Valid @RequestBody TopUpRequest request) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("MISSING_USER_ID");
        }
        var account = accountService.topUp(userId, request.amount());
        return ResponseEntity.ok(new AccountResponse(account.getUserId(), account.getBalance(), "geocredits"));
    }

    @GetMapping("/balance")
    public ResponseEntity<?> getBalance(@RequestHeader("X-User-Id") String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("MISSING_USER_ID");
        }
        Long balance = accountService.getBalance(userId);
        return ResponseEntity.ok(new AccountResponse(userId, balance, "geocredits"));
    }

    public record TopUpRequest(@NotNull @Positive Long amount) {}
}
