package com.example.orderservice.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentEvent {
    private String eventId;
    private String eventType;
    private String paymentId;
    private Long orderId;
    private Long userId;
    private String status;
    private BigDecimal amount;
    private Instant timestamp;
}