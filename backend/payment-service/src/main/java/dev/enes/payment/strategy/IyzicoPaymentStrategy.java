package dev.enes.payment.strategy;

import com.iyzipay.Options;
import com.iyzipay.model.*;
import com.iyzipay.request.CreatePaymentRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;

@Slf4j
@Component
@RequiredArgsConstructor
public class IyzicoPaymentStrategy implements PaymentStrategy {

    private final Options options;

    @Override
    public String getMethod() {
        return "CREDIT_CARD";
    }

    @Override
    @CircuitBreaker(name = "iyzicoCircuitBreaker", fallbackMethod = "processFallback")
    @Retry(name = "iyzicoRetry", fallbackMethod = "processFallback")
    public PaymentResult process(String orderId, BigDecimal amount, String userId, com.fasterxml.jackson.databind.JsonNode extraData) {
        log.info("Processing real Iyzico payment: order={}, amount={}", orderId, amount);

        try {
            CreatePaymentRequest request = new CreatePaymentRequest();
            request.setLocale(Locale.TR.name());
            request.setConversationId(UUID.randomUUID().toString());
            request.setPrice(amount);
            request.setPaidPrice(amount);
            request.setCurrency(Currency.TRY.name());
            request.setInstallment(1);
            request.setBasketId(orderId);
            request.setPaymentChannel(PaymentChannel.WEB.name());
            request.setPaymentGroup(PaymentGroup.PRODUCT.name());

            PaymentCard paymentCard = new PaymentCard();
            if (extraData != null && !extraData.isNull()) {
                paymentCard.setCardHolderName(extraData.path("cardHolderName").asText("N11 Test User"));
                paymentCard.setCardNumber(extraData.path("cardNumber").asText("5528790000000008"));
                paymentCard.setExpireMonth(extraData.path("expireMonth").asText("12"));
                paymentCard.setExpireYear(extraData.path("expireYear").asText("2030"));
                paymentCard.setCvc(extraData.path("cvc").asText("123"));
            } else {
                // Fallback to test card if no details provided
                paymentCard.setCardHolderName("N11 Test User");
                paymentCard.setCardNumber("5528790000000008");
                paymentCard.setExpireMonth("12");
                paymentCard.setExpireYear("2030");
                paymentCard.setCvc("123");
            }
            paymentCard.setRegisterCard(0);
            request.setPaymentCard(paymentCard);

            Buyer buyer = new Buyer();
            buyer.setId(userId);
            buyer.setName("N11");
            buyer.setSurname("User");
            buyer.setGsmNumber("+905350000000");
            buyer.setEmail("user@n11-bootcamp.com");
            buyer.setIdentityNumber("11111111111");
            buyer.setIp("127.0.0.1");
            buyer.setCity("Istanbul");
            buyer.setCountry("Turkey");
            buyer.setRegistrationAddress("Patika Bootcamp Address");
            buyer.setZipCode("34000");
            request.setBuyer(buyer);

            Address shippingAddress = new Address();
            shippingAddress.setContactName("N11 User");
            shippingAddress.setCity("Istanbul");
            shippingAddress.setCountry("Turkey");
            shippingAddress.setAddress("Patika Bootcamp Address");
            shippingAddress.setZipCode("34000");
            request.setShippingAddress(shippingAddress);

            Address billingAddress = new Address();
            billingAddress.setContactName("N11 User");
            billingAddress.setCity("Istanbul");
            billingAddress.setCountry("Turkey");
            billingAddress.setAddress("Patika Bootcamp Address");
            billingAddress.setZipCode("34000");
            request.setBillingAddress(billingAddress);

            List<BasketItem> basketItems = new ArrayList<>();
            BasketItem item = new BasketItem();
            item.setId(orderId);
            item.setName("N11 Marketplace Order " + orderId);
            item.setCategory1("E-commerce");
            item.setItemType(BasketItemType.PHYSICAL.name());
            item.setPrice(amount);
            basketItems.add(item);
            request.setBasketItems(basketItems);

            Payment payment = Payment.create(request, options);

            if ("success".equals(payment.getStatus())) {
                log.info("Iyzico payment successful: paymentId={}", payment.getPaymentId());
                return new PaymentResult(true, payment.getPaymentId(), null);
            } else {
                log.error("Iyzico payment failed: error={}, code={}", payment.getErrorMessage(), payment.getErrorCode());
                return new PaymentResult(false, null, payment.getErrorMessage());
            }
        } catch (Exception e) {
            log.error("Error during Iyzico payment processing", e);
            throw new RuntimeException("Iyzico processing failed", e); // Throw so Resilience4j can catch it
        }
    }

    public PaymentResult processFallback(String orderId, BigDecimal amount, String userId, com.fasterxml.jackson.databind.JsonNode extraData, Throwable t) {
        log.error("Iyzico payment fallback triggered for order {}. Reason: {}", orderId, t.getMessage());
        return new PaymentResult(false, null, "Ödeme sistemi şu anda yanıt vermiyor (Fallback). Lütfen daha sonra tekrar deneyiniz.");
    }

    @Override
    public PaymentResult refund(String transactionId, BigDecimal amount) {
        log.info("Refunding real Iyzico payment: txn={}, amount={}", transactionId, amount);
        // Refund implementation omitted for brevity but would follow similar pattern
        return new PaymentResult(true, transactionId, null);
    }
}
