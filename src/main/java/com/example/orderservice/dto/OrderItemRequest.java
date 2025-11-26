package com.example.orderservice.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderItemRequest {
    private Long itemId;
    private Integer quantity;
}