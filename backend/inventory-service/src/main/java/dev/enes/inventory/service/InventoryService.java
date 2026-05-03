package dev.enes.inventory.service;

import dev.enes.common.event.EventConstants;
import dev.enes.common.event.OrderCreatedEvent;
import dev.enes.common.event.StockEvent;
import dev.enes.common.exception.ResourceNotFoundException;
import dev.enes.inventory.dto.InventoryRequest;
import dev.enes.inventory.dto.InventoryResponse;
import dev.enes.inventory.entity.Inventory;
import dev.enes.inventory.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final RabbitTemplate rabbitTemplate;

    @Transactional
    public InventoryResponse createOrUpdate(InventoryRequest request) {
        Inventory inventory = inventoryRepository.findByProductId(request.getProductId())
                .orElse(Inventory.builder()
                        .productId(request.getProductId())
                        .build());

        inventory.setQuantity(request.getQuantity());
        inventory = inventoryRepository.save(inventory);
        log.info("Inventory set: product={}, qty={}", request.getProductId(), request.getQuantity());
        return toResponse(inventory);
    }

    @Transactional(readOnly = true)
    public InventoryResponse getByProductId(UUID productId) {
        return inventoryRepository.findByProductId(productId)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory", productId));
    }

    /**
     * Yeni ürün oluşturulduğunda RabbitMQ üzerinden gelen event'i dinler
     * ve otomatik olarak stok kaydı oluşturur.
     */
    @RabbitListener(queues = "q.product.created")
    @Transactional
    public void onProductCreated(String message) {
        try {
            com.fasterxml.jackson.databind.JsonNode event =
                    new com.fasterxml.jackson.databind.ObjectMapper().readTree(message);
            UUID productId = UUID.fromString(event.get("productId").asText());
            int quantity = event.has("quantity") ? event.get("quantity").asInt() : 100;

            // Aynı ürün için stok kaydı varsa atla (idempotent)
            if (inventoryRepository.findByProductId(productId).isPresent()) {
                return;
            }

            Inventory inventory = Inventory.builder()
                    .productId(productId)
                    .quantity(quantity)
                    .build();
            inventoryRepository.save(inventory);
            log.info("Ürün için stok kaydı oluşturuldu: productId={}, adet={}", productId, quantity);
        } catch (Exception e) {
            log.error("product.created event işlenemedi", e);
        }
    }

    @RabbitListener(queues = EventConstants.PAYMENT_RESULT_QUEUE)
    @Transactional
    public void onPaymentResult(com.fasterxml.jackson.databind.JsonNode event) {
        try {
            String status = event.get("status").asText();
            UUID orderId = UUID.fromString(event.get("orderId").asText());

            if ("COMPLETED".equals(status)) {
                log.info("Payment completed for order {}, reserving stock", orderId);
                // Stock reservation would be based on order items stored in the event
                // For now, publish stock reserved
                StockEvent stockEvent = StockEvent.builder()
                        .orderId(orderId)
                        .status(StockEvent.Status.RESERVED)
                        .timestamp(LocalDateTime.now())
                        .build();
                rabbitTemplate.convertAndSend(EventConstants.EXCHANGE, EventConstants.STOCK_RESULT_KEY, stockEvent);
            }
        } catch (Exception e) {
            log.error("Failed to process payment result event", e);
        }
    }

    @Transactional
    public void reserveStock(UUID productId, int quantity) {
        Inventory inventory = inventoryRepository.findByProductIdForUpdate(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory", productId));
        inventory.reserve(quantity);
        inventoryRepository.save(inventory);
        log.info("Stock reserved: product={}, qty={}", productId, quantity);
    }

    @Transactional
    public void releaseStock(UUID productId, int quantity) {
        Inventory inventory = inventoryRepository.findByProductIdForUpdate(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory", productId));
        inventory.releaseReservation(quantity);
        inventoryRepository.save(inventory);
        log.info("Stock released: product={}, qty={}", productId, quantity);
    }

    @Transactional
    public void confirmStock(UUID productId, int quantity) {
        Inventory inventory = inventoryRepository.findByProductIdForUpdate(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory", productId));
        inventory.confirmReservation(quantity);
        inventoryRepository.save(inventory);
        log.info("Stock confirmed: product={}, qty={}", productId, quantity);
    }

    private InventoryResponse toResponse(Inventory i) {
        return InventoryResponse.builder()
                .id(i.getId())
                .productId(i.getProductId())
                .quantity(i.getQuantity())
                .reservedQuantity(i.getReservedQuantity())
                .availableQuantity(i.getAvailableQuantity())
                .build();
    }
}
