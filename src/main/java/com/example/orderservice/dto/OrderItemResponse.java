package com.example.orderservice.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class OrderItemResponse {
    private Long id;
    private Long itemId;
    private String itemName;
    private BigDecimal itemPrice;
    private Integer quantity;
}