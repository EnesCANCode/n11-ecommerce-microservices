package dev.enes.order.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.enes.order.dto.CreateOrderRequest;
import dev.enes.order.dto.OrderResponse;
import dev.enes.order.entity.Order;
import dev.enes.order.entity.OrderStatus;
import dev.enes.order.entity.OutboxEvent;
import dev.enes.order.repository.OrderRepository;
import dev.enes.order.repository.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OutboxEventRepository outboxRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private OrderService orderService;

    private UUID userId;
    private CreateOrderRequest createOrderRequest;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        
        CreateOrderRequest.OrderItemDto itemRequest = CreateOrderRequest.OrderItemDto.builder()
                .productId(UUID.randomUUID())
                .productName("Test Product")
                .quantity(2)
                .price(new BigDecimal("50.00"))
                .sellerId(UUID.randomUUID())
                .build();

        createOrderRequest = CreateOrderRequest.builder()
                .paymentMethod("CREDIT_CARD")
                .items(List.of(itemRequest))
                .build();
    }

    @Test
    void createOrder_shouldSaveOrderAndOutboxEvent() throws Exception {
        // Arrange
        when(orderRepository.findByIdempotencyKey(any())).thenReturn(Optional.empty());
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        
        doAnswer((Answer<Order>) invocation -> {
            Order order = invocation.getArgument(0);
            if (order.getId() == null) {
                order.setId(UUID.randomUUID());
            }
            return order;
        }).when(orderRepository).save(any(Order.class));

        // Act
        OrderResponse response = orderService.createOrder(createOrderRequest, userId, "key-123");

        // Assert
        assertNotNull(response);
        assertEquals("PAYMENT_PROCESSING", response.getStatus());
        assertEquals(new BigDecimal("100.00"), response.getTotalAmount());

        verify(orderRepository, times(2)).save(any(Order.class));
        verify(outboxRepository).save(any(OutboxEvent.class));
        
        ArgumentCaptor<OutboxEvent> outboxCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxRepository).save(outboxCaptor.capture());
        assertEquals("ORDER_CREATED", outboxCaptor.getValue().getEventType());
    }

    @Test
    void createOrder_shouldReturnExistingOrder_whenIdempotencyKeyExists() {
        // Arrange
        Order existingOrder = Order.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .status(OrderStatus.PAYMENT_PROCESSING)
                .totalAmount(new BigDecimal("100.00"))
                .build();
        
        when(orderRepository.findByIdempotencyKey("key-123")).thenReturn(Optional.of(existingOrder));

        // Act
        OrderResponse response = orderService.createOrder(createOrderRequest, userId, "key-123");

        // Assert
        assertNotNull(response);
        assertEquals(existingOrder.getId(), response.getId());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void updateStatus_shouldUpdateOrderStatus() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        Order order = Order.builder().id(orderId).status(OrderStatus.PENDING).build();
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        // Act
        orderService.updateStatus(orderId, OrderStatus.CONFIRMED, null);

        // Assert
        assertEquals(OrderStatus.CONFIRMED, order.getStatus());
        verify(orderRepository).save(order);
    }
}
