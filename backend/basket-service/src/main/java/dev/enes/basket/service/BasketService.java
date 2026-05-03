package dev.enes.basket.service;

import dev.enes.basket.dto.AddItemRequest;
import dev.enes.basket.dto.Basket;
import dev.enes.basket.dto.BasketItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class BasketService {

    private static final String KEY_PREFIX = "basket:";
    private static final long BASKET_TTL_DAYS = 7;

    private final RedisTemplate<String, Object> redisTemplate;

    public Basket getBasket(String userId) {
        Basket basket = (Basket) redisTemplate.opsForValue().get(KEY_PREFIX + userId);
        if (basket == null) {
            basket = new Basket();
            basket.setUserId(userId);
        }
        return basket;
    }

    public Basket addItem(String userId, AddItemRequest request) {
        Basket basket = getBasket(userId);

        basket.getItems().stream()
                .filter(item -> item.getProductId().equals(request.getProductId()))
                .findFirst()
                .ifPresentOrElse(
                        existing -> existing.setQuantity(existing.getQuantity() + request.getQuantity()),
                        () -> basket.getItems().add(new BasketItem(
                                request.getProductId(),
                                request.getProductName(),
                                request.getPrice(),
                                request.getQuantity(),
                                request.getSellerId(),
                                request.getSellerName(),
                                request.getImageUrl()
                        ))
                );

        saveBasket(basket);
        log.info("Item added to basket: user={}, product={}", userId, request.getProductId());
        return basket;
    }

    public Basket updateItemQuantity(String userId, UUID productId, int quantity) {
        Basket basket = getBasket(userId);

        if (quantity <= 0) {
            basket.getItems().removeIf(item -> item.getProductId().equals(productId));
        } else {
            basket.getItems().stream()
                    .filter(item -> item.getProductId().equals(productId))
                    .findFirst()
                    .ifPresent(item -> item.setQuantity(quantity));
        }

        saveBasket(basket);
        return basket;
    }

    public Basket removeItem(String userId, UUID productId) {
        Basket basket = getBasket(userId);
        basket.getItems().removeIf(item -> item.getProductId().equals(productId));
        saveBasket(basket);
        return basket;
    }

    public void clearBasket(String userId) {
        redisTemplate.delete(KEY_PREFIX + userId);
        log.info("Basket cleared: user={}", userId);
    }

    private void saveBasket(Basket basket) {
        redisTemplate.opsForValue().set(
                KEY_PREFIX + basket.getUserId(),
                basket,
                BASKET_TTL_DAYS,
                TimeUnit.DAYS
        );
    }
}
