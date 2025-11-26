package com.example.orderservice.dto;

import com.example.orderservice.entity.OrderStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class OrderRequest {
    @NotNull(message = "User ID is mandatory")
    @Positive(message = "User ID must be positive")
    private Long userId;

    private OrderStatus status;

    @NotNull(message = "Items are mandatory")
    @Valid
    private List<OrderItemRequest> items;
}