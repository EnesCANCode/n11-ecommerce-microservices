package dev.enes.basket.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BasketItem implements Serializable {
    private UUID productId;
    private String productName;
    private BigDecimal price;
    private int quantity;
    private UUID sellerId;
    private String sellerName;
    private String imageUrl;

    @com.fasterxml.jackson.annotation.JsonIgnore
    public BigDecimal getSubtotal() {
        return price.multiply(BigDecimal.valueOf(quantity));
    }
}
