package dev.enes.order.service;

import dev.enes.common.event.EventConstants;
import dev.enes.order.entity.OutboxEvent;
import dev.enes.order.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxRelayScheduler {

    private final OutboxEventRepository outboxRepository;
    private final RabbitTemplate rabbitTemplate;

    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void processOutbox() {
        List<OutboxEvent> events = outboxRepository.findByProcessedFalseOrderByCreatedAtAsc();
        for (OutboxEvent event : events) {
            try {
                rabbitTemplate.convertAndSend(
                        EventConstants.EXCHANGE,
                        event.getRoutingKey(),
                        event.getPayload()
                );
                event.setProcessed(true);
                event.setProcessedAt(LocalDateTime.now());
                outboxRepository.save(event);
                log.debug("Outbox event relayed: {} -> {}", event.getId(), event.getRoutingKey());
            } catch (Exception e) {
                log.error("Failed to relay outbox event: {}", event.getId(), e);
                break;
            }
        }
    }
}
