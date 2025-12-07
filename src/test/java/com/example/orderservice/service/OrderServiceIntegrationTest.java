package com.example.orderservice.service;

import com.example.orderservice.dto.OrderItemRequest;
import com.example.orderservice.dto.OrderRequest;
import com.example.orderservice.dto.OrderResponse;
import com.example.orderservice.entity.Item;
import com.example.orderservice.entity.OrderStatus;
import com.example.orderservice.repository.ItemRepository;
import com.example.orderservice.repository.OrderRepository;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@TestPropertySource(properties = {
        "user.service.url=http://localhost:18089",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.liquibase.enabled=false",
        "resilience4j.circuitbreaker.instances.userService.register-health-indicator=false",
        "resilience4j.circuitbreaker.instances.userService.sliding-window-size=5",
        "resilience4j.circuitbreaker.instances.userService.minimum-number-of-calls=1",
        "resilience4j.circuitbreaker.instances.userService.permitted-number-of-calls-in-half-open-state=2",
        "resilience4j.circuitbreaker.instances.userService.wait-duration-in-open-state=1s",
        "resilience4j.circuitbreaker.instances.userService.failure-rate-threshold=50",
        "spring.datasource.driver-class-name=org.postgresql.Driver",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect"
})
class OrderServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testDb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgreSQLContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgreSQLContainer::getUsername);
        registry.add("spring.datasource.password", postgreSQLContainer::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ItemRepository itemRepository;

    private static WireMockServer wireMockServer;
    private Item savedItem;

    @BeforeAll
    static void beforeAll() {
        wireMockServer = new WireMockServer(18_089);
        wireMockServer.start();
        WireMock.configureFor("localhost", 18_089);
    }

    @AfterAll
    static void afterAll() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        itemRepository.deleteAll();

        WireMock.reset();

        Item item = new Item();
        item.setName("Integration Test Item");
        item.setPrice(BigDecimal.valueOf(50.00));
        item.setCreatedAt(LocalDateTime.now());
        item.setUpdatedAt(LocalDateTime.now());
        savedItem = itemRepository.save(item);
    }

    @Test
    void createOrder_ShouldCreateOrder_WhenUserServiceReturnsActiveUser(){
        // Arrange
        String userJson = """
            {
                "id": 1,
                "name": "John",
                "surname": "Doe",
                "birthDate": "1990-01-01",
                "email": "john.doe@example.com",
                "active": true,
                "createdAt": "2024-01-01T00:00:00",
                "updatedAt": "2024-01-01T00:00:00"
            }
            """;

        stubFor(get(urlEqualTo("/api/users/1"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody(userJson)));

        OrderRequest orderRequest = new OrderRequest();
        orderRequest.setUserId(1L);
        orderRequest.setStatus(OrderStatus.CREATED);

        OrderItemRequest orderItemRequest = new OrderItemRequest();
        orderItemRequest.setItemId(savedItem.getId());
        orderItemRequest.setQuantity(3);
        orderRequest.setItems(List.of(orderItemRequest));

        // Act
        OrderResponse orderResponse = orderService.createOrder(orderRequest);

        // Assert
        assertThat(orderResponse).isNotNull();
        assertThat(orderResponse.getId()).isNotNull();
        assertThat(orderResponse.getUserId()).isEqualTo(1L);
        assertThat(orderResponse.getStatus()).isEqualTo(OrderStatus.CREATED);
        assertThat(orderResponse.getTotalPrice()).isEqualByComparingTo(BigDecimal.valueOf(150.00));

        verify(getRequestedFor(urlEqualTo("/api/users/1")));
        assertThat(orderRepository.count()).isEqualTo(1);
    }

    @Test
    void getOrderById_ShouldReturnOrder_WhenOrderExists(){
        // Arrange
        String userJson = """
            {
                "id": 2,
                "name": "Jane",
                "surname": "Smith",
                "birthDate": "1992-02-02",
                "email": "jane.smith@example.com",
                "active": true,
                "createdAt": "2024-01-01T00:00:00",
                "updatedAt": "2024-01-01T00:00:00"
            }
            """;

        stubFor(get(urlEqualTo("/api/users/2"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody(userJson)));

        OrderRequest orderRequest = new OrderRequest();
        orderRequest.setUserId(2L);
        orderRequest.setStatus(OrderStatus.PROCESSING);

        OrderItemRequest orderItemRequest = new OrderItemRequest();
        orderItemRequest.setItemId(savedItem.getId());
        orderItemRequest.setQuantity(2);
        orderRequest.setItems(List.of(orderItemRequest));

        OrderResponse createdOrder = orderService.createOrder(orderRequest);
        Long orderId = createdOrder.getId();

        WireMock.reset();
        stubFor(get(urlEqualTo("/api/users/2"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody(userJson)));

        // Act
        OrderResponse retrievedOrder = orderService.getOrderById(orderId);

        // Assert
        assertThat(retrievedOrder).isNotNull();
        assertThat(retrievedOrder.getId()).isEqualTo(orderId);
        assertThat(retrievedOrder.getUserId()).isEqualTo(2L);
        assertThat(retrievedOrder.getStatus()).isEqualTo(OrderStatus.PROCESSING);
        assertThat(retrievedOrder.getTotalPrice()).isEqualByComparingTo(BigDecimal.valueOf(100.00)); // 50 * 2
    }

    @Test
    void updateOrder_ShouldUpdateOrderSuccessfully(){
        // Arrange
        String userJson = """
            {
                "id": 3,
                "name": "Bob",
                "surname": "Brown",
                "birthDate": "1985-03-03",
                "email": "bob.brown@example.com",
                "active": true,
                "createdAt": "2024-01-01T00:00:00",
                "updatedAt": "2024-01-01T00:00:00"
            }
            """;

        stubFor(get(urlEqualTo("/api/users/3"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody(userJson)));

        OrderRequest createRequest = new OrderRequest();
        createRequest.setUserId(3L);
        createRequest.setStatus(OrderStatus.CREATED);

        OrderItemRequest orderItemRequest = new OrderItemRequest();
        orderItemRequest.setItemId(savedItem.getId());
        orderItemRequest.setQuantity(1);
        createRequest.setItems(List.of(orderItemRequest));

        OrderResponse createdOrder = orderService.createOrder(createRequest);
        Long orderId = createdOrder.getId();

        WireMock.reset();
        stubFor(get(urlEqualTo("/api/users/3"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody(userJson)));

        OrderRequest updateRequest = new OrderRequest();
        updateRequest.setUserId(3L);
        updateRequest.setStatus(OrderStatus.COMPLETED);

        OrderItemRequest updatedItemRequest = new OrderItemRequest();
        updatedItemRequest.setItemId(savedItem.getId());
        updatedItemRequest.setQuantity(5);
        updateRequest.setItems(List.of(updatedItemRequest));

        // Act
        OrderResponse updatedOrder = orderService.updateOrder(orderId, updateRequest);

        // Assert
        assertThat(updatedOrder).isNotNull();
        assertThat(updatedOrder.getId()).isEqualTo(orderId);
        assertThat(updatedOrder.getStatus()).isEqualTo(OrderStatus.COMPLETED);
        assertThat(updatedOrder.getTotalPrice().compareTo(BigDecimal.valueOf(250.00))).isEqualTo(0);
    }

    @Test
    void deleteOrder_ShouldSoftDeleteOrder(){
        // Arrange - Create order
        String userJson = """
            {
                "id": 4,
                "name": "Alice",
                "surname": "Johnson",
                "birthDate": "1995-04-04",
                "email": "alice.johnson@example.com",
                "active": true,
                "createdAt": "2024-01-01T00:00:00",
                "updatedAt": "2024-01-01T00:00:00"
            }
            """;

        stubFor(get(urlEqualTo("/api/users/4"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody(userJson)));

        OrderRequest orderRequest = new OrderRequest();
        orderRequest.setUserId(4L);
        orderRequest.setStatus(OrderStatus.CREATED);

        OrderItemRequest orderItemRequest = new OrderItemRequest();
        orderItemRequest.setItemId(savedItem.getId());
        orderItemRequest.setQuantity(2);
        orderRequest.setItems(List.of(orderItemRequest));

        OrderResponse createdOrder = orderService.createOrder(orderRequest);
        Long orderId = createdOrder.getId();

        // Act
        orderService.deleteOrder(orderId);

        // Assert
        var deletedOrder = orderRepository.findByIdAndDeletedFalse(orderId);
        assertThat(deletedOrder).isEmpty();

        var allOrders = orderRepository.findAll();
        assertThat(allOrders).hasSize(1);
        assertThat(allOrders.get(0).getDeleted()).isTrue();
    }

    @Test
    void getOrdersByUserId_ShouldReturnUserOrders(){
        // Arrange
        String userJson = """
            {
                "id": 5,
                "name": "Charlie",
                "surname": "Wilson",
                "birthDate": "1988-05-05",
                "email": "charlie.wilson@example.com",
                "active": true,
                "createdAt": "2024-01-01T00:00:00",
                "updatedAt": "2024-01-01T00:00:00"
            }
            """;

        stubFor(get(urlEqualTo("/api/users/5"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody(userJson)));

        OrderRequest orderRequest1 = new OrderRequest();
        orderRequest1.setUserId(5L);
        orderRequest1.setStatus(OrderStatus.CREATED);

        OrderItemRequest orderItemRequest1 = new OrderItemRequest();
        orderItemRequest1.setItemId(savedItem.getId());
        orderItemRequest1.setQuantity(1);
        orderRequest1.setItems(List.of(orderItemRequest1));

        orderService.createOrder(orderRequest1);

        OrderRequest orderRequest2 = new OrderRequest();
        orderRequest2.setUserId(5L);
        orderRequest2.setStatus(OrderStatus.PROCESSING);

        OrderItemRequest orderItemRequest2 = new OrderItemRequest();
        orderItemRequest2.setItemId(savedItem.getId());
        orderItemRequest2.setQuantity(2);
        orderRequest2.setItems(List.of(orderItemRequest2));

        orderService.createOrder(orderRequest2);

        WireMock.reset();
        stubFor(get(urlEqualTo("/api/users/5"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody(userJson)));

        // Act
        var orders = orderService.getOrdersByUserId(5L);

        // Assert
        assertThat(orders).hasSize(2);
        assertThat(orders).extracting("userId").containsOnly(5L);
    }

    @Test
    void createOrder_ShouldUseCircuitBreakerFallback_WhenUserServiceFails(){
        // Arrange
        stubFor(get(urlEqualTo("/api/users/6"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.INTERNAL_SERVER_ERROR.value())));

        OrderRequest orderRequest = new OrderRequest();
        orderRequest.setUserId(6L);
        orderRequest.setStatus(OrderStatus.CREATED);

        OrderItemRequest orderItemRequest = new OrderItemRequest();
        orderItemRequest.setItemId(savedItem.getId());
        orderItemRequest.setQuantity(1);
        orderRequest.setItems(List.of(orderItemRequest));

        // Act
        OrderResponse orderResponse = orderService.createOrder(orderRequest);

        // Assert
        assertThat(orderResponse).isNotNull();
        assertThat(orderResponse.getUserInfo()).isNotNull();

        String userName = orderResponse.getUserInfo().getName();
        assertThat(userName).satisfiesAnyOf(
                name -> assertThat(name).isEqualTo("Service Temporarily Unavailable"),
                name -> assertThat(name).isEqualTo("User information unavailable")
        );

        assertThat(orderRepository.count()).isEqualTo(1);
    }
}