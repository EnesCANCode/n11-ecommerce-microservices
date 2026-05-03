package dev.enes.basket.controller;

import dev.enes.basket.dto.AddItemRequest;
import dev.enes.basket.dto.Basket;
import dev.enes.basket.service.BasketService;
import dev.enes.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/baskets")
@RequiredArgsConstructor
public class BasketController {

    private final BasketService basketService;

    @GetMapping
    public ApiResponse<Basket> getBasket(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.ok(basketService.getBasket(jwt.getSubject()));
    }

    @PostMapping("/items")
    public ApiResponse<Basket> addItem(@AuthenticationPrincipal Jwt jwt,
                                       @Valid @RequestBody AddItemRequest request) {
        return ApiResponse.ok(basketService.addItem(jwt.getSubject(), request));
    }

    @PutMapping("/items/{productId}")
    public ApiResponse<Basket> updateQuantity(@AuthenticationPrincipal Jwt jwt,
                                               @PathVariable UUID productId,
                                               @RequestParam int quantity) {
        return ApiResponse.ok(basketService.updateItemQuantity(jwt.getSubject(), productId, quantity));
    }

    @DeleteMapping("/items/{productId}")
    public ApiResponse<Basket> removeItem(@AuthenticationPrincipal Jwt jwt,
                                           @PathVariable UUID productId) {
        return ApiResponse.ok(basketService.removeItem(jwt.getSubject(), productId));
    }

    @DeleteMapping
    public ApiResponse<String> clearBasket(@AuthenticationPrincipal Jwt jwt) {
        basketService.clearBasket(jwt.getSubject());
        return ApiResponse.ok("Basket cleared");
    }
}
