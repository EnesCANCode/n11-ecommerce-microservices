package dev.enes.payment.strategy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Component
public class WalletPaymentStrategy implements PaymentStrategy {

    @Override
    public String getMethod() {
        return "WALLET";
    }

    @Override
    public PaymentResult process(String orderId, BigDecimal amount, String userId, com.fasterxml.jackson.databind.JsonNode extraData) {
        log.info("Processing wallet payment: order={}, amount={}", orderId, amount);
        String transactionId = "WLT-" + UUID.randomUUID().toString().substring(0, 8);
        return new PaymentResult(true, transactionId, null);
    }

    @Override
    public PaymentResult refund(String transactionId, BigDecimal amount) {
        log.info("Refunding wallet payment: txn={}", transactionId);
        return new PaymentResult(true, transactionId, null);
    }
}
