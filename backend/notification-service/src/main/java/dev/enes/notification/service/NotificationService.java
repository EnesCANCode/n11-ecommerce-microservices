package dev.enes.notification.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.enes.common.event.EventConstants;
import dev.enes.notification.entity.Notification;
import dev.enes.notification.entity.NotificationType;
import dev.enes.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = EventConstants.NOTIFICATION_QUEUE)
    public void onEvent(String message) {
        try {
            JsonNode event = objectMapper.readTree(message);
            String eventType = event.has("eventType") ? event.get("eventType").asText() : "SYSTEM";
            UUID userId = event.has("userId") ? UUID.fromString(event.get("userId").asText()) : null;
            UUID orderId = event.has("orderId") ? UUID.fromString(event.get("orderId").asText()) : null;

            if (userId == null) return;

            String title;
            String body;
            NotificationType type;

            switch (eventType) {
                case "ORDER_CREATED" -> {
                    title = "Sipariş Oluşturuldu";
                    body = "Siparişiniz #" + orderId + " başarıyla oluşturuldu.";
                    type = NotificationType.ORDER;
                }
                case "PAYMENT_COMPLETED" -> {
                    title = "Ödeme Başarılı";
                    body = "Siparişiniz #" + orderId + " için ödeme alındı.";
                    type = NotificationType.PAYMENT;
                }
                case "PAYMENT_FAILED" -> {
                    title = "Ödeme Başarısız";
                    body = "Siparişiniz #" + orderId + " için ödeme başarısız oldu.";
                    type = NotificationType.PAYMENT;
                }
                case "STOCK_RESERVED" -> {
                    title = "Stok Ayrıldı";
                    body = "Siparişiniz #" + orderId + " onaylandı, kargoya hazırlanıyor.";
                    type = NotificationType.STOCK;
                }
                default -> {
                    title = "Bildirim";
                    body = message;
                    type = NotificationType.SYSTEM;
                }
            }

            Notification notification = Notification.builder()
                    .userId(userId)
                    .title(title)
                    .message(body)
                    .type(type)
                    .build();

            notificationRepository.save(notification);

            // WebSocket üzerinden arayüze gerçek zamanlı bildirim gönder
            messagingTemplate.convertAndSendToUser(
                    userId.toString(),
                    "/queue/notifications",
                    notification
            );

            log.info("Notification sent: user={}, type={}", userId, type);
        } catch (Exception e) {
            log.error("Failed to process notification event", e);
        }
    }

    @Transactional(readOnly = true)
    public Page<Notification> getUserNotifications(UUID userId, int page, int size) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size));
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(UUID userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    @Transactional
    public void markAsRead(UUID notificationId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            n.setRead(true);
            notificationRepository.save(n);
        });
    }
}
