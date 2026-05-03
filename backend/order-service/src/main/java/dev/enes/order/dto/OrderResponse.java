package dev.enes.order.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class OrderResponse {
    private UUID id;
    private UUID userId;
    private String status;
    private BigDecimal totalAmount;
    private String paymentMethod;
    private String idempotencyKey;
    private List<OrderItemResponse> items;
    private String failureReason;
    private LocalDateTime createdAt;

    @Data
    @Builder
    public static class OrderItemResponse {
        private UUID productId;
        private String productName;
        private int quantity;
        private BigDecimal price;
        private UUID sellerId;
    }
}
