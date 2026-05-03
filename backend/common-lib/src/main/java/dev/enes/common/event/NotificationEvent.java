package dev.enes.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEvent {

    private UUID userId;
    private UUID orderId;
    private Type type;
    private String message;
    private LocalDateTime timestamp;

    public enum Type {
        ORDER_CONFIRMED, ORDER_CANCELLED, PAYMENT_RECEIVED, STOCK_LOW
    }
}
