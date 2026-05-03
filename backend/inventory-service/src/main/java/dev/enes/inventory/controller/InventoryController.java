package dev.enes.inventory.controller;

import dev.enes.common.dto.ApiResponse;
import dev.enes.inventory.dto.InventoryRequest;
import dev.enes.inventory.dto.InventoryResponse;
import dev.enes.inventory.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<InventoryResponse> createOrUpdate(@Valid @RequestBody InventoryRequest request) {
        return ApiResponse.created(inventoryService.createOrUpdate(request));
    }

    @GetMapping("/{productId}")
    public ApiResponse<InventoryResponse> getByProductId(@PathVariable UUID productId) {
        return ApiResponse.ok(inventoryService.getByProductId(productId));
    }

    @PostMapping("/{productId}/reserve")
    public ApiResponse<String> reserve(@PathVariable UUID productId, @RequestParam int quantity) {
        inventoryService.reserveStock(productId, quantity);
        return ApiResponse.ok("Stock reserved");
    }

    @PostMapping("/{productId}/release")
    public ApiResponse<String> release(@PathVariable UUID productId, @RequestParam int quantity) {
        inventoryService.releaseStock(productId, quantity);
        return ApiResponse.ok("Stock released");
    }
}
