package dev.enes.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class InventoryRequest {
    @NotNull
    private UUID productId;

    @Min(0)
    private int quantity;
}
