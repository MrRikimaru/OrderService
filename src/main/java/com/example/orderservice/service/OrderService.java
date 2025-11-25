package com.example.orderservice.service;

import com.example.orderservice.dto.*;
import com.example.orderservice.entity.*;
import com.example.orderservice.repository.ItemRepository;
import com.example.orderservice.repository.OrderRepository;
import com.example.orderservice.specification.OrderSpecifications;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ItemRepository itemRepository;

    @Transactional(rollbackFor = Exception.class)
    public OrderResponse createOrder(OrderRequest request) {
        Order order = new Order();
        order.setUserId(request.getUserId());
        order.setStatus(request.getStatus() != null ? request.getStatus() : OrderStatus.CREATED);
        order.setDeleted(false);

        processOrderItems(order, request.getItems());

        Order savedOrder = orderRepository.save(order);
        return convertToResponse(savedOrder);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long id) {
        Order order = orderRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new EntityNotFoundException("Order not found with id: " + id));
        return convertToResponse(order);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> getOrdersWithFilter(LocalDateTime startDate, LocalDateTime endDate,
                                                   List<OrderStatus> statuses, Pageable pageable) {
        Specification<Order> spec = OrderSpecifications.buildSpecification(startDate, endDate, statuses);
        Page<Order> orders = orderRepository.findAll(spec, pageable);
        return orders.map(this::convertToResponse);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByUserId(Long userId) {
        List<Order> orders = orderRepository.findByUserIdAndDeletedFalse(userId);
        return orders.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(rollbackFor = Exception.class)
    public OrderResponse updateOrder(Long id, OrderRequest request) {
        Order existingOrder = orderRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new EntityNotFoundException("Order not found with id: " + id));

        existingOrder.setUserId(request.getUserId());
        existingOrder.setStatus(request.getStatus());

        existingOrder.clearOrderItems();

        processOrderItems(existingOrder, request.getItems());

        Order updatedOrder = orderRepository.save(existingOrder);
        return convertToResponse(updatedOrder);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteOrder(Long id) {
        orderRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new EntityNotFoundException("Order not found with id: " + id));
        orderRepository.softDelete(id);
    }

    private void processOrderItems(Order order, List<OrderItemRequest> items) {
        BigDecimal totalPrice = BigDecimal.ZERO;

        for (OrderItemRequest itemRequest : items) {
            Item item = itemRepository.findById(itemRequest.getItemId())
                    .orElseThrow(() -> new EntityNotFoundException("Item not found with id: " + itemRequest.getItemId()));

            OrderItem orderItem = new OrderItem();
            orderItem.setItem(item);
            orderItem.setQuantity(itemRequest.getQuantity());

            order.addOrderItem(orderItem);

            BigDecimal itemTotal = item.getPrice().multiply(BigDecimal.valueOf(itemRequest.getQuantity()));
            totalPrice = totalPrice.add(itemTotal);
        }

        order.setTotalPrice(totalPrice);
    }

    private OrderResponse convertToResponse(Order order) {
        OrderResponse response = new OrderResponse();
        response.setId(order.getId());
        response.setUserId(order.getUserId());
        response.setStatus(order.getStatus());
        response.setTotalPrice(order.getTotalPrice());
        response.setCreatedAt(order.getCreatedAt());
        response.setUpdatedAt(order.getUpdatedAt());

        List<OrderItemResponse> itemResponses = order.getOrderItems().stream()
                .map(this::convertToItemResponse)
                .collect(Collectors.toList());

        response.setItems(itemResponses);
        return response;
    }

    private OrderItemResponse convertToItemResponse(OrderItem orderItem) {
        OrderItemResponse response = new OrderItemResponse();
        response.setId(orderItem.getId());
        response.setItemId(orderItem.getItem().getId());
        response.setItemName(orderItem.getItem().getName());
        response.setItemPrice(orderItem.getItem().getPrice());
        response.setQuantity(orderItem.getQuantity());
        return response;
    }
}