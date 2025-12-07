package com.example.orderservice.exception;

import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final String ERROR = "error";
    private static final String MESSAGE = "message";

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ConcurrentHashMap<String, String>> handleEntityNotFound(EntityNotFoundException ex) {
        ConcurrentHashMap<String, String> response = new ConcurrentHashMap<>();
        response.put(ERROR, "Not Found");
        response.put(MESSAGE, ex.getMessage());
        if (log.isWarnEnabled()) {
            log.warn("Entity not found: {}", ex.getMessage());
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ConcurrentHashMap<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        ConcurrentHashMap<String, String> response = new ConcurrentHashMap<>();
        response.put(ERROR, "Bad Request");
        response.put(MESSAGE, ex.getMessage());
        if (log.isWarnEnabled()) {
            log.warn("Illegal argument: {}", ex.getMessage());
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ConcurrentHashMap<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        ConcurrentHashMap<String, String> errors = new ConcurrentHashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        if (log.isWarnEnabled()) {
            log.warn("Validation errors: {}", errors);
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ConcurrentHashMap<String, String>> handleGenericException(Exception ex) {
        ConcurrentHashMap<String, String> response = new ConcurrentHashMap<>();
        response.put(ERROR, "Internal Server Error");
        response.put(MESSAGE, "An unexpected error occurred");

        log.error("Unexpected error occurred: {}", ex.getMessage(), ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ConcurrentHashMap<String, String>> handleIllegalState(IllegalStateException ex) {
        ConcurrentHashMap<String, String> response = new ConcurrentHashMap<>();
        response.put(ERROR, "Bad Request");
        response.put(MESSAGE, ex.getMessage());
        if (log.isWarnEnabled()) {
            log.warn("Illegal state: {}", ex.getMessage());
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
}