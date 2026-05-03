package dev.enes.order.controller;

import dev.enes.common.dto.ApiResponse;
import dev.enes.common.dto.PageResponse;
import dev.enes.order.dto.CreateOrderRequest;
import dev.enes.order.dto.OrderResponse;
import dev.enes.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Order Controller", description = "Sipariş yönetimi için API")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Yeni sipariş oluştur", description = "Sepetteki ürünler için yeni bir sipariş kaydı oluşturur.")
    @ApiResponse(responseCode = "201", description = "Sipariş başarıyla oluşturuldu")
    public dev.enes.common.dto.ApiResponse<OrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return ApiResponse.created(orderService.createOrder(request, userId, idempotencyKey));
    }

    @GetMapping("/{orderId}")
    public ApiResponse<OrderResponse> getById(
            @PathVariable UUID orderId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return ApiResponse.ok(orderService.getById(orderId, userId));
    }

    @GetMapping
    public ApiResponse<PageResponse<OrderResponse>> getUserOrders(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return ApiResponse.ok(orderService.getUserOrders(userId, page, size));
    }
}
