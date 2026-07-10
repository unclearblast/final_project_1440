package com.orbitamarket.payments.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AccountResponse {
    private String userId;
    private Long balance;
    private String currency;
}
