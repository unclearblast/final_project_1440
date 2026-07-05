package com.orbitamarket.payments.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleIllegalArgument(IllegalArgumentException ex) {
        String errorCode = switch (ex.getMessage()) {
            case "INVALID_AMOUNT" -> "INVALID_AMOUNT";
            case "ACCOUNT_NOT_FOUND" -> "ACCOUNT_NOT_FOUND";
            default -> "INTERNAL_ERROR";
        };
        HttpStatus status = errorCode.equals("ACCOUNT_NOT_FOUND") ? HttpStatus.NOT_FOUND : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(Map.of(
                "error_code", errorCode,
                "message", ex.getMessage(),
                "timestamp", Instant.now().toString()
        ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b).orElse("Validation error");
        return ResponseEntity.badRequest().body(Map.of(
                "error_code", "INVALID_AMOUNT",
                "message", message,
                "timestamp", Instant.now().toString()
        ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGeneral(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error_code", "INTERNAL_ERROR",
                "message", "Unexpected error",
                "timestamp", Instant.now().toString()
        ));
    }
}
