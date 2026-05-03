package dev.enes.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.enes.common.event.EventConstants;
import dev.enes.common.event.PaymentResultEvent;
import dev.enes.payment.entity.Payment;
import dev.enes.payment.entity.PaymentStatus;
import dev.enes.payment.repository.PaymentRepository;
import dev.enes.payment.strategy.PaymentStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private PaymentStrategy creditCardStrategy;

    private PaymentService paymentService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        when(creditCardStrategy.getMethod()).thenReturn("CREDIT_CARD");
        
        paymentService = new PaymentService(
                paymentRepository,
                rabbitTemplate,
                objectMapper,
                List.of(creditCardStrategy)
        );
    }

    @Test
    void onOrderCreated_whenPaymentSucceeds_shouldSavePaymentAndSendEvent() throws Exception {
        // Arrange
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String message = String.format("{\"orderId\":\"%s\",\"userId\":\"%s\",\"totalAmount\":\"100.00\",\"paymentMethod\":\"CREDIT_CARD\",\"paymentDetails\":null}", orderId, userId);
        
        PaymentStrategy.PaymentResult successResult = new PaymentStrategy.PaymentResult(true, "txn-123", null);
        when(creditCardStrategy.process(eq(orderId.toString()), eq(new BigDecimal("100.00")), eq(userId.toString()), any()))
                .thenReturn(successResult);

        // Act
        paymentService.onOrderCreated(message);

        // Assert
        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(paymentCaptor.capture());
        Payment savedPayment = paymentCaptor.getValue();
        assertEquals(PaymentStatus.COMPLETED, savedPayment.getStatus());
        assertEquals("txn-123", savedPayment.getTransactionId());

        ArgumentCaptor<PaymentResultEvent> eventCaptor = ArgumentCaptor.forClass(PaymentResultEvent.class);
        verify(rabbitTemplate).convertAndSend(eq(EventConstants.EXCHANGE), eq(EventConstants.PAYMENT_RESULT_KEY), eventCaptor.capture());
        PaymentResultEvent sentEvent = eventCaptor.getValue();
        assertEquals("COMPLETED", sentEvent.getStatus());
        assertEquals(orderId, sentEvent.getOrderId());
        assertEquals("txn-123", sentEvent.getTransactionId());
    }

    @Test
    void onOrderCreated_whenPaymentFails_shouldSaveFailedPaymentAndSendEvent() throws Exception {
        // Arrange
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String message = String.format("{\"orderId\":\"%s\",\"userId\":\"%s\",\"totalAmount\":\"100.00\",\"paymentMethod\":\"CREDIT_CARD\",\"paymentDetails\":null}", orderId, userId);
        
        PaymentStrategy.PaymentResult failResult = new PaymentStrategy.PaymentResult(false, null, "Insufficient funds");
        when(creditCardStrategy.process(eq(orderId.toString()), eq(new BigDecimal("100.00")), eq(userId.toString()), any()))
                .thenReturn(failResult);

        // Act
        paymentService.onOrderCreated(message);

        // Assert
        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(paymentCaptor.capture());
        Payment savedPayment = paymentCaptor.getValue();
        assertEquals(PaymentStatus.FAILED, savedPayment.getStatus());
        assertEquals("Insufficient funds", savedPayment.getFailureReason());

        ArgumentCaptor<PaymentResultEvent> eventCaptor = ArgumentCaptor.forClass(PaymentResultEvent.class);
        verify(rabbitTemplate).convertAndSend(eq(EventConstants.EXCHANGE), eq(EventConstants.PAYMENT_RESULT_KEY), eventCaptor.capture());
        PaymentResultEvent sentEvent = eventCaptor.getValue();
        assertEquals("FAILED", sentEvent.getStatus());
        assertEquals(orderId, sentEvent.getOrderId());
        assertEquals("Insufficient funds", sentEvent.getFailureReason());
    }
}
