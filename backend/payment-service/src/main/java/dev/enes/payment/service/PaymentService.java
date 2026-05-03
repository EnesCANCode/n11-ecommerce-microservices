package dev.enes.payment.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.enes.common.event.EventConstants;
import dev.enes.common.event.PaymentResultEvent;
import dev.enes.payment.entity.Payment;
import dev.enes.payment.entity.PaymentStatus;
import dev.enes.payment.repository.PaymentRepository;
import dev.enes.payment.strategy.PaymentStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final Map<String, PaymentStrategy> strategyMap;

    public PaymentService(PaymentRepository paymentRepository,
                          RabbitTemplate rabbitTemplate,
                          ObjectMapper objectMapper,
                          List<PaymentStrategy> strategies) {
        this.paymentRepository = paymentRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
        this.strategyMap = strategies.stream()
                .collect(Collectors.toMap(PaymentStrategy::getMethod, Function.identity()));
    }

    @RabbitListener(queues = EventConstants.ORDER_CREATED_QUEUE)
    @Transactional
    public void onOrderCreated(String message) {
        try {
            JsonNode event = objectMapper.readTree(message);
            UUID orderId = UUID.fromString(event.get("orderId").asText());
            UUID userId = UUID.fromString(event.get("userId").asText());
            BigDecimal totalAmount = new BigDecimal(event.get("totalAmount").asText());
            String paymentMethod = event.get("paymentMethod").asText();

            log.info("Processing payment: order={}, method={}, amount={}", orderId, paymentMethod, totalAmount);

            Payment payment = Payment.builder()
                    .orderId(orderId)
                    .userId(userId)
                    .amount(totalAmount)
                    .paymentMethod(paymentMethod)
                    .build();

            PaymentStrategy strategy = strategyMap.getOrDefault(paymentMethod,
                    strategyMap.get("CREDIT_CARD"));

            PaymentStrategy.PaymentResult result = strategy.process(
                    orderId.toString(), totalAmount, userId.toString(), event.get("paymentDetails"));

            if (result.success()) {
                payment.setStatus(PaymentStatus.COMPLETED);
                payment.setTransactionId(result.transactionId());
            } else {
                payment.setStatus(PaymentStatus.FAILED);
                payment.setFailureReason(result.failureReason());
            }

            paymentRepository.save(payment);

            PaymentResultEvent resultEvent = PaymentResultEvent.builder()
                    .orderId(orderId)
                    .status(result.success() ? "COMPLETED" : "FAILED")
                    .transactionId(result.transactionId())
                    .failureReason(result.failureReason())
                    .timestamp(LocalDateTime.now())
                    .build();

            rabbitTemplate.convertAndSend(
                    EventConstants.EXCHANGE,
                    EventConstants.PAYMENT_RESULT_KEY,
                    resultEvent
            );

            log.info("Payment {} for order {}", result.success() ? "completed" : "failed", orderId);
        } catch (Exception e) {
            log.error("Failed to process order created event", e);
        }
    }
}
