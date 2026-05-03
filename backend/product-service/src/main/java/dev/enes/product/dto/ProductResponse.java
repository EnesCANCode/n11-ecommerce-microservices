package dev.enes.product.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class ProductResponse {

    private UUID id;
    private String name;
    private String description;
    private BigDecimal price;
    private String imageUrl;
    private UUID sellerId;
    private String sellerName;
    private String categoryName;
    private UUID categoryId;
    private boolean active;
    private LocalDateTime createdAt;
}
