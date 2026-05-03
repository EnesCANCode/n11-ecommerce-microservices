package dev.enes.inventory.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class InventoryResponse {
    private UUID id;
    private UUID productId;
    private int quantity;
    private int reservedQuantity;
    private int availableQuantity;
}
