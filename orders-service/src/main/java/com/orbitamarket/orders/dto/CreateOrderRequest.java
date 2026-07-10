package com.orbitamarket.orders.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CreateOrderRequest {
    @NotBlank
    private String productType;

    @NotNull @Positive
    private Long price;

    @NotBlank
    private String payload;  // JSON-строка с AOI, capture_date, sensor_type и т.д.
}
