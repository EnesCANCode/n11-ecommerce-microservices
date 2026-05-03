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
public class PaymentEvent {

    private UUID orderId;
    private UUID paymentId;
    private Status status;
    private String failureReason;
    private LocalDateTime timestamp;

    public enum Status {
        COMPLETED, FAILED
    }
}
