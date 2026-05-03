package dev.enes.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResultEvent {
    private UUID orderId;
    private String status;
    private String transactionId;
    private String failureReason;
    private LocalDateTime timestamp;
}
