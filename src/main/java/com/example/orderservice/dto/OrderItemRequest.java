package com.example.orderservice.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderItemRequest {
    @NotNull(message = "Item ID is mandatory")
    @Positive(message = "Item ID must be positive")
    private Long itemId;

    @NotNull(message = "Quantity is mandatory")
    @Positive(message = "Quantity must be positive")
    private Integer quantity;
}