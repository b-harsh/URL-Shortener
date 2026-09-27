
package com.harsh.shortener.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleStatusException(
            ResponseStatusException exception) {

        return ResponseEntity
                .status(exception.getStatusCode())
                .body(Map.of(
                        "timestamp", Instant.now().toString(),
                        "status", exception.getStatusCode().value(),
                        "error", exception.getReason() == null
                                ? "Request failed"
                                : exception.getReason()
                ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(
            MethodArgumentNotValidException exception) {

        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getDefaultMessage())
                .findFirst()
                .orElse("Invalid request");

        return ResponseEntity
                .badRequest()
                .body(Map.of(
                        "timestamp", Instant.now().toString(),
                        "status", 400,
                        "error", message
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpectedException(
            Exception exception) {

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                        "timestamp", Instant.now().toString(),
                        "status", 500,
                        "error", "An unexpected error occurred"
                ));
    }
}