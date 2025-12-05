package com.example.orderservice.service;

import com.example.orderservice.client.UserServiceClient;
import com.example.orderservice.dto.*;
import com.example.orderservice.entity.*;
import com.example.orderservice.mapper.OrderMapper;
import com.example.orderservice.mapper.OrderItemMapper;
import com.example.orderservice.repository.ItemRepository;
import com.example.orderservice.repository.OrderRepository;
import com.example.orderservice.specification.OrderSpecifications;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ItemRepository itemRepository;
    private final UserServiceClient userServiceClient;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;

    @Transactional(rollbackFor = Exception.class)
    public OrderResponse createOrder(OrderRequest request) {
        log.info("Creating order for user: {}", request.getUserId());

        UserResponseDTO userInfo;
        try {
            userInfo = getUserInfoWithFallback(request.getUserId());
        } catch (Exception e) {
            log.warn("Using fallback user info due to: {}", e.getMessage());
            userInfo = new UserResponseDTO();
            userInfo.setId(request.getUserId());
            userInfo.setName("Fallback User");
            userInfo.setActive(true);
        }

        if (!Boolean.TRUE.equals(userInfo.getActive())) {
            throw new IllegalArgumentException("User is inactive");
        }

        Order order = orderMapper.toEntity(request);
        order.setUserId(request.getUserId());
        order.setStatus(request.getStatus() != null ? request.getStatus() : OrderStatus.CREATED);
        order.setDeleted(false);

        processOrderItems(order, request.getItems());

        Order savedOrder = orderRepository.save(order);
        log.info("Order created with id: {}", savedOrder.getId());

        return convertToResponse(savedOrder);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long id) {
        log.debug("Fetching order by id: {}", id);
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
        log.debug("Fetching orders for user: {}", userId);
        List<Order> orders = orderRepository.findByUserIdAndDeletedFalse(userId);
        return orders.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByUserEmail(String email) {
        log.debug("Fetching orders for user email: {}", email);

        UserResponseDTO userInfo = getUserByEmailWithFallback(email);
        if (userInfo.getId() == null) {
            throw new EntityNotFoundException("User not found with email: " + email);
        }

        return getOrdersByUserId(userInfo.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public OrderResponse updateOrder(Long id, OrderRequest request) {
        log.info("Updating order with id: {}", id);

        Order existingOrder = orderRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new EntityNotFoundException("Order not found with id: " + id));

        UserResponseDTO userInfo = getUserInfoWithFallback(request.getUserId());
        if (!Boolean.TRUE.equals(userInfo.getActive())) {
            throw new IllegalArgumentException("User is inactive");
        }

        existingOrder.setUserId(request.getUserId());
        existingOrder.setStatus(request.getStatus());

        existingOrder.clearOrderItems();
        processOrderItems(existingOrder, request.getItems());

        Order updatedOrder = orderRepository.save(existingOrder);
        log.info("Order updated with id: {}", id);

        return convertToResponse(updatedOrder);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteOrder(Long id) {
        log.info("Soft deleting order with id: {}", id);
        orderRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new EntityNotFoundException("Order not found with id: " + id));
        orderRepository.softDelete(id);
        log.info("Order soft deleted with id: {}", id);
    }

    private void processOrderItems(Order order, List<OrderItemRequest> items) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Order must have at least one item");
        }

        BigDecimal totalPrice = BigDecimal.ZERO;

        for (OrderItemRequest itemRequest : items) {
            Item item = itemRepository.findById(itemRequest.getItemId())
                    .orElseThrow(() -> new EntityNotFoundException("Item not found with id: " + itemRequest.getItemId()));

            if (itemRequest.getQuantity() <= 0) {
                throw new IllegalArgumentException("Quantity must be positive for item: " + item.getId());
            }

            OrderItem orderItem = orderItemMapper.toEntity(itemRequest);
            orderItem.setItem(item);
            orderItem.setQuantity(itemRequest.getQuantity());

            order.addOrderItem(orderItem);

            BigDecimal itemTotal = item.getPrice().multiply(BigDecimal.valueOf(itemRequest.getQuantity()));
            totalPrice = totalPrice.add(itemTotal);
        }

        order.setTotalPrice(totalPrice);
    }

    private OrderResponse convertToResponse(Order order) {
        OrderResponse response = orderMapper.toResponse(order);
        response.setUserId(order.getUserId());
        response.setStatus(order.getStatus());
        response.setTotalPrice(order.getTotalPrice());
        response.setCreatedAt(order.getCreatedAt());
        response.setUpdatedAt(order.getUpdatedAt());

        try {
            UserResponseDTO userInfo = getUserInfoWithFallback(order.getUserId());
            response.setUserInfo(userInfo);
        } catch (Exception e) {
            log.warn("Failed to fetch user info for order response, userId: {}, error: {}",
                    order.getUserId(), e.getMessage());
            UserResponseDTO fallbackUser = new UserResponseDTO();
            fallbackUser.setId(order.getUserId());
            fallbackUser.setName("User information unavailable");
            fallbackUser.setActive(true);
            response.setUserInfo(fallbackUser);
        }

        List<OrderItemResponse> itemResponses = order.getOrderItems().stream()
                .map(orderItemMapper::toResponse)
                .collect(Collectors.toList());

        response.setItems(itemResponses);
        return response;
    }

    @CircuitBreaker(name = "userService", fallbackMethod = "getUserInfoFallback")
    private UserResponseDTO getUserInfoWithFallback(Long userId) {
        try {
            UserResponseDTO userInfo = userServiceClient.getUserById(userId);
            if (userInfo == null || userInfo.getId() == null) {
                throw new IllegalArgumentException("User not found with id: " + userId);
            }
            return userInfo;
        } catch (FeignException.NotFound e) {
            log.warn("User not found for userId: {}, error: {}", userId, e.getMessage());
            throw new IllegalArgumentException("User not found with id: " + userId);
        } catch (Exception e) {
            log.warn("Failed to fetch user info for userId: {}, error: {}", userId, e.getMessage());
            throw new IllegalArgumentException("Unable to fetch user info for id: " + userId);
        }
    }

    @CircuitBreaker(name = "userService", fallbackMethod = "getUserByEmailFallback")
    private UserResponseDTO getUserByEmailWithFallback(String email) {
        try {
            UserResponseDTO userInfo = userServiceClient.getUserByEmail(email);
            if (userInfo == null) {
                throw new IllegalArgumentException("User not found with email: " + email);
            }
            return userInfo;
        } catch (Exception e) {
            log.warn("Failed to fetch user info for email: {}, error: {}", email, e.getMessage());
            throw new IllegalArgumentException("User not found with email: " + email);
        }
    }
}