package com.example.orderservice.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class OrderItemDTO {
    private Long id;

    @NotNull(message = "Order ID is mandatory")
    @Positive(message = "Order ID must be positive")
    private Long orderId;

    @NotNull(message = "Item ID is mandatory")
    @Positive(message = "Item ID must be positive")
    private Long itemId;

    @NotNull(message = "Quantity is mandatory")
    @Positive(message = "Quantity must be positive")
    private Integer quantity;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}