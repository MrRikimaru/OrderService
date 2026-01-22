package com.example.orderservice.entity;

public enum OrderStatus {
    CREATED,
    PROCESSING,
    PAID,
    PAYMENT_FAILED,
    COMPLETED,
    CANCELLED
}