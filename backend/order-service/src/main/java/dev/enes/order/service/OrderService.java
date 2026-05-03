package dev.enes.order.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.enes.common.dto.PageResponse;
import dev.enes.common.event.EventConstants;
import dev.enes.common.event.OrderCreatedEvent;
import dev.enes.common.exception.BusinessException;
import dev.enes.common.exception.ResourceNotFoundException;
import dev.enes.order.dto.CreateOrderRequest;
import dev.enes.order.dto.OrderResponse;
import dev.enes.order.entity.*;
import dev.enes.order.repository.OrderRepository;
import dev.enes.order.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request, UUID userId, String idempotencyKey) {
        if (idempotencyKey != null) {
            var existing = orderRepository.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                log.info("Idempotent order returned: {}", existing.get().getId());
                return toResponse(existing.get());
            }
        }

        BigDecimal totalAmount = request.getItems().stream()
                .map(i -> i.getPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Order order = Order.builder()
                .userId(userId)
                .totalAmount(totalAmount)
                .paymentMethod(request.getPaymentMethod())
                .idempotencyKey(idempotencyKey)
                .status(OrderStatus.PENDING)
                .build();

        request.getItems().forEach(itemDto -> {
            OrderItem item = OrderItem.builder()
                    .productId(itemDto.getProductId())
                    .productName(itemDto.getProductName())
                    .quantity(itemDto.getQuantity())
                    .price(itemDto.getPrice())
                    .sellerId(itemDto.getSellerId())
                    .build();
            order.addItem(item);
        });

        orderRepository.save(order);

        // Publish via Transactional Outbox
        try {
            OrderCreatedEvent event = OrderCreatedEvent.builder()
                    .orderId(order.getId())
                    .userId(userId)
                    .totalAmount(totalAmount)
                    .paymentMethod(request.getPaymentMethod())
                    .idempotencyKey(idempotencyKey)
                    .items(request.getItems().stream()
                            .map(i -> OrderCreatedEvent.OrderItemEvent.builder()
                                    .productId(i.getProductId())
                                    .quantity(i.getQuantity())
                                    .price(i.getPrice())
                                    .sellerId(i.getSellerId())
                                    .build())
                            .toList())
                    .paymentDetails(request.getPaymentDetails() != null ? OrderCreatedEvent.PaymentDetails.builder()
                            .cardHolderName(request.getPaymentDetails().getCardHolderName())
                            .cardNumber(request.getPaymentDetails().getCardNumber())
                            .expireMonth(request.getPaymentDetails().getExpireMonth())
                            .expireYear(request.getPaymentDetails().getExpireYear())
                            .cvc(request.getPaymentDetails().getCvc())
                            .build() : null)
                    .timestamp(LocalDateTime.now())
                    .build();

            OutboxEvent outbox = OutboxEvent.builder()
                    .aggregateType("Order")
                    .aggregateId(order.getId().toString())
                    .eventType("ORDER_CREATED")
                    .payload(objectMapper.writeValueAsString(event))
                    .routingKey(EventConstants.ORDER_CREATED_KEY)
                    .build();
            outboxRepository.save(outbox);
        } catch (Exception e) {
            log.error("Failed to create outbox event for order {}", order.getId(), e);
        }

        order.setStatus(OrderStatus.PAYMENT_PROCESSING);
        orderRepository.save(order);

        log.info("Order created: {}", order.getId());
        return toResponse(order);
    }

    @Transactional(readOnly = true)
    public OrderResponse getById(UUID orderId, UUID userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Access denied");
        }
        return toResponse(order);
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getUserOrders(UUID userId, int page, int size) {
        Page<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(
                userId, PageRequest.of(page, size));
        return PageResponse.of(
                orders.getContent().stream().map(this::toResponse).toList(),
                orders.getNumber(), orders.getSize(),
                orders.getTotalElements(), orders.getTotalPages(), orders.isLast());
    }

    @Transactional
    public void updateStatus(UUID orderId, OrderStatus status, String failureReason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
        order.setStatus(status);
        if (failureReason != null) {
            order.setFailureReason(failureReason);
        }
        orderRepository.save(order);
        log.info("Order {} status updated to {}", orderId, status);
    }

    private OrderResponse toResponse(Order o) {
        return OrderResponse.builder()
                .id(o.getId())
                .userId(o.getUserId())
                .status(o.getStatus().name())
                .totalAmount(o.getTotalAmount())
                .paymentMethod(o.getPaymentMethod())
                .idempotencyKey(o.getIdempotencyKey())
                .failureReason(o.getFailureReason())
                .createdAt(o.getCreatedAt())
                .items(o.getItems().stream()
                        .map(i -> OrderResponse.OrderItemResponse.builder()
                                .productId(i.getProductId())
                                .productName(i.getProductName())
                                .quantity(i.getQuantity())
                                .price(i.getPrice())
                                .sellerId(i.getSellerId())
                                .build())
                        .toList())
                .build();
    }
}
