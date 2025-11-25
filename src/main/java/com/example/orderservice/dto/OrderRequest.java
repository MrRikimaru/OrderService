package com.example.orderservice.dto;

import com.example.orderservice.entity.OrderStatus;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class OrderRequest {
    private Long userId;
    private OrderStatus status;
    private List<OrderItemRequest> items;
}