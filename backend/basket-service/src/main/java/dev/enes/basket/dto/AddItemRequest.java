package dev.enes.basket.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class AddItemRequest {
    @NotNull
    private UUID productId;
    private String productName;
    @NotNull
    private BigDecimal price;
    @Min(1)
    private int quantity = 1;
    private UUID sellerId;
    private String sellerName;
    private String imageUrl;
}
