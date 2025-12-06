package com.example.orderservice.service;

import com.example.orderservice.client.UserServiceClient;
import com.example.orderservice.dto.OrderRequest;
import com.example.orderservice.dto.OrderResponse;
import com.example.orderservice.dto.OrderItemRequest;
import com.example.orderservice.dto.OrderItemResponse;
import com.example.orderservice.dto.UserResponseDTO;
import com.example.orderservice.entity.Order;
import com.example.orderservice.entity.Item;
import com.example.orderservice.entity.OrderItem;
import com.example.orderservice.entity.OrderStatus;
import com.example.orderservice.mapper.OrderItemMapper;
import com.example.orderservice.mapper.OrderMapper;
import com.example.orderservice.repository.ItemRepository;
import com.example.orderservice.repository.OrderRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.doNothing;


@ExtendWith(MockitoExtension.class)
class OrderServiceUnitTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private UserServiceClient userServiceClient;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private OrderItemMapper orderItemMapper;

    @InjectMocks
    private OrderService orderService;

    private Order order;
    private OrderRequest orderRequest;
    private OrderResponse orderResponse;
    private Item item;
    private OrderItem orderItem;
    private OrderItemResponse orderItemResponse;
    private UserResponseDTO userResponseDTO;

    @BeforeEach
    void setUp() {
        item = new Item();
        item.setId(1L);
        item.setName("Test Item");
        item.setPrice(BigDecimal.valueOf(100.00));

        orderItem = new OrderItem();
        orderItem.setId(1L);
        orderItem.setItem(item);
        orderItem.setQuantity(2);

        order = new Order();
        order.setId(1L);
        order.setUserId(1L);
        order.setStatus(OrderStatus.CREATED);
        order.setTotalPrice(BigDecimal.valueOf(200.00));
        order.setDeleted(false);
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        order.setOrderItems(new ArrayList<>(List.of(orderItem)));

        OrderItemRequest orderItemRequest = new OrderItemRequest();
        orderItemRequest.setItemId(1L);
        orderItemRequest.setQuantity(2);

        orderItemResponse = new OrderItemResponse();
        orderItemResponse.setId(1L);
        orderItemResponse.setItemId(1L);
        orderItemResponse.setItemName("Test Item");
        orderItemResponse.setItemPrice(BigDecimal.valueOf(100.00));
        orderItemResponse.setQuantity(2);

        orderRequest = new OrderRequest();
        orderRequest.setUserId(1L);
        orderRequest.setStatus(OrderStatus.CREATED);
        orderRequest.setItems(new ArrayList<>(List.of(orderItemRequest)));

        userResponseDTO = new UserResponseDTO();
        userResponseDTO.setId(1L);
        userResponseDTO.setName("John");
        userResponseDTO.setSurname("Doe");
        userResponseDTO.setEmail("john.doe@example.com");
        userResponseDTO.setActive(true);
        userResponseDTO.setCreatedAt(LocalDateTime.now());
        userResponseDTO.setUpdatedAt(LocalDateTime.now());

        orderResponse = new OrderResponse();
        orderResponse.setId(1L);
        orderResponse.setUserId(1L);
        orderResponse.setStatus(OrderStatus.CREATED);
        orderResponse.setTotalPrice(BigDecimal.valueOf(200.00));
        orderResponse.setCreatedAt(LocalDateTime.now());
        orderResponse.setUpdatedAt(LocalDateTime.now());
        orderResponse.setItems(List.of(orderItemResponse));
        orderResponse.setUserInfo(userResponseDTO);
    }

    @Test
    void createOrder_ShouldCreateOrderSuccessfully_WhenUserIsActive() {
        // Arrange
        when(userServiceClient.getUserById(anyLong())).thenReturn(userResponseDTO);
        when(orderMapper.toEntity(any(OrderRequest.class))).thenReturn(order);
        when(itemRepository.findById(anyLong())).thenReturn(Optional.of(item));
        when(orderItemMapper.toEntity(any(OrderItemRequest.class))).thenReturn(orderItem);
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(orderMapper.toResponse(any(Order.class))).thenReturn(orderResponse);
        when(orderItemMapper.toResponse(any(OrderItem.class))).thenReturn(orderItemResponse);

        // Act
        OrderResponse result = orderService.createOrder(orderRequest);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getUserId()).isEqualTo(1L);
        assertThat(result.getStatus()).isEqualTo(OrderStatus.CREATED);
        assertThat(result.getTotalPrice()).isEqualTo(BigDecimal.valueOf(200.00));

        verify(userServiceClient, times(2)).getUserById(1L);
        verify(orderRepository).save(any(Order.class));
        verify(itemRepository).findById(1L);
    }

    @Test
    void createOrder_ShouldThrowException_WhenUserIsInactive() {
        // Arrange
        userResponseDTO.setActive(false);
        when(userServiceClient.getUserById(anyLong())).thenReturn(userResponseDTO);

        // Act & Assert
        assertThatThrownBy(() -> orderService.createOrder(orderRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("User is inactive");

        verify(userServiceClient).getUserById(1L);
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void createOrder_ShouldThrowException_WhenItemsAreEmpty() {
        // Arrange
        orderRequest.setItems(new ArrayList<>());
        when(userServiceClient.getUserById(anyLong())).thenReturn(userResponseDTO);
        when(orderMapper.toEntity(any(OrderRequest.class))).thenReturn(order); // Добавлен мок

        // Act & Assert
        assertThatThrownBy(() -> orderService.createOrder(orderRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Order must have at least one item");

        verify(userServiceClient).getUserById(1L);
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void createOrder_ShouldThrowException_WhenItemNotFound() {
        // Arrange
        when(userServiceClient.getUserById(anyLong())).thenReturn(userResponseDTO);
        when(orderMapper.toEntity(any(OrderRequest.class))).thenReturn(order);
        when(itemRepository.findById(anyLong())).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> orderService.createOrder(orderRequest))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Item not found");

        verify(userServiceClient).getUserById(1L);
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void getOrderById_ShouldReturnOrder_WhenOrderExists() {
        // Arrange
        when(orderRepository.findByIdAndDeletedFalse(anyLong())).thenReturn(Optional.of(order));
        when(userServiceClient.getUserById(anyLong())).thenReturn(userResponseDTO);
        when(orderMapper.toResponse(any(Order.class))).thenReturn(orderResponse);
        when(orderItemMapper.toResponse(any(OrderItem.class))).thenReturn(orderItemResponse);

        // Act
        OrderResponse result = orderService.getOrderById(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        verify(orderRepository).findByIdAndDeletedFalse(1L);
    }

    @Test
    void getOrdersWithFilter_ShouldReturnPaginatedOrders() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());
        Page<Order> orderPage = new PageImpl<>(List.of(order));

        when(orderRepository.findAll(Mockito.<Specification<Order>>any(), eq(pageable))).thenReturn(orderPage);
        when(userServiceClient.getUserById(anyLong())).thenReturn(userResponseDTO);
        when(orderMapper.toResponse(any(Order.class))).thenReturn(orderResponse);
        when(orderItemMapper.toResponse(any(OrderItem.class))).thenReturn(orderItemResponse);

        // Act
        Page<OrderResponse> result = orderService.getOrdersWithFilter(
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now(),
                List.of(OrderStatus.CREATED),
                pageable
        );

        // Assert
        assertThat(result).isNotEmpty();
        assertThat(result.getContent()).hasSize(1);
        verify(orderRepository).findAll(Mockito.<Specification<Order>>any(), eq(pageable));
    }

    @Test
    void getOrdersByUserId_ShouldReturnUserOrders() {
        // Arrange
        when(orderRepository.findByUserIdAndDeletedFalse(anyLong())).thenReturn(List.of(order));
        when(userServiceClient.getUserById(anyLong())).thenReturn(userResponseDTO);
        when(orderMapper.toResponse(any(Order.class))).thenReturn(orderResponse);
        when(orderItemMapper.toResponse(any(OrderItem.class))).thenReturn(orderItemResponse);

        // Act
        List<OrderResponse> result = orderService.getOrdersByUserId(1L);

        // Assert
        assertThat(result).isNotEmpty();
        assertThat(result).hasSize(1);
        verify(orderRepository).findByUserIdAndDeletedFalse(1L);
    }

    @Test
    void updateOrder_ShouldUpdateOrderSuccessfully() {
        // Arrange
        Order testOrder = new Order();
        testOrder.setId(1L);
        testOrder.setUserId(1L);
        testOrder.setStatus(OrderStatus.CREATED);
        testOrder.setTotalPrice(BigDecimal.valueOf(200.00));
        testOrder.setDeleted(false);
        testOrder.setCreatedAt(LocalDateTime.now());
        testOrder.setUpdatedAt(LocalDateTime.now());
        testOrder.setOrderItems(new ArrayList<>());

        when(orderRepository.findByIdAndDeletedFalse(anyLong())).thenReturn(Optional.of(testOrder));
        when(userServiceClient.getUserById(anyLong())).thenReturn(userResponseDTO);
        when(itemRepository.findById(anyLong())).thenReturn(Optional.of(item));
        when(orderItemMapper.toEntity(any(OrderItemRequest.class))).thenReturn(orderItem);
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        when(orderMapper.toResponse(any(Order.class))).thenReturn(orderResponse);
        when(orderItemMapper.toResponse(any(OrderItem.class))).thenReturn(orderItemResponse);

        // Act
        OrderResponse result = orderService.updateOrder(1L, orderRequest);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        verify(orderRepository).findByIdAndDeletedFalse(1L);
        verify(orderRepository).save(testOrder);
    }

    @Test
    void deleteOrder_ShouldSoftDeleteOrder() {
        // Arrange
        when(orderRepository.findByIdAndDeletedFalse(anyLong())).thenReturn(Optional.of(order));
        doNothing().when(orderRepository).softDelete(anyLong());

        // Act
        orderService.deleteOrder(1L);

        // Assert
        verify(orderRepository).findByIdAndDeletedFalse(1L);
        verify(orderRepository).softDelete(1L);
    }

    @Test
    void convertToResponse_ShouldHandleUserServiceFailure() {
        // Arrange
        when(orderRepository.findByIdAndDeletedFalse(anyLong())).thenReturn(Optional.of(order));
        when(userServiceClient.getUserById(anyLong())).thenThrow(new RuntimeException("Service unavailable"));
        when(orderMapper.toResponse(any(Order.class))).thenReturn(orderResponse);
        when(orderItemMapper.toResponse(any(OrderItem.class))).thenReturn(orderItemResponse);

        // Act
        OrderResponse result = orderService.getOrderById(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getUserInfo()).isNotNull();
        assertThat(result.getUserInfo().getName()).isEqualTo("User information unavailable");
    }
}