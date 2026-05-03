package dev.enes.payment.strategy;

import java.math.BigDecimal;

public interface PaymentStrategy {
    String getMethod();
    PaymentResult process(String orderId, BigDecimal amount, String userId, com.fasterxml.jackson.databind.JsonNode extraData);
    PaymentResult refund(String transactionId, BigDecimal amount);

    record PaymentResult(boolean success, String transactionId, String failureReason) {}
}
