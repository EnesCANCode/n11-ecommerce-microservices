package dev.enes.order.service;

import dev.enes.common.event.EventConstants;
import dev.enes.order.entity.OrderStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class SagaEventListener {

    private final OrderService orderService;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = EventConstants.PAYMENT_RESULT_QUEUE + ".order")
    public void onPaymentResult(dev.enes.common.event.PaymentResultEvent event) {
        try {
            UUID orderId = event.getOrderId();
            String status = event.getStatus();

            if ("COMPLETED".equals(status)) {
                log.info("Payment completed for order {}", orderId);
                orderService.updateStatus(orderId, OrderStatus.PAYMENT_COMPLETED, null);
            } else {
                String reason = event.getFailureReason() != null ? event.getFailureReason() : "Payment failed";
                log.info("Payment failed for order {}: {}", orderId, reason);
                orderService.updateStatus(orderId, OrderStatus.CANCELLED, reason);
            }
        } catch (Exception e) {
            log.error("Failed to process payment result", e);
        }
    }

    @RabbitListener(queues = EventConstants.STOCK_RESULT_QUEUE + ".order")
    public void onStockResult(dev.enes.common.event.StockEvent event) {
        try {
            UUID orderId = event.getOrderId();
            dev.enes.common.event.StockEvent.Status status = event.getStatus();

            if (dev.enes.common.event.StockEvent.Status.RESERVED.equals(status)) {
                log.info("Stock reserved for order {}", orderId);
                orderService.updateStatus(orderId, OrderStatus.CONFIRMED, null);
            } else {
                String reason = event.getFailureReason() != null ? event.getFailureReason() : "Stock unavailable";
                log.info("Stock failed for order {}: {}", orderId, reason);
                orderService.updateStatus(orderId, OrderStatus.CANCELLED, reason);
            }
        } catch (Exception e) {
            log.error("Failed to process stock result", e);
        }
    }
}
