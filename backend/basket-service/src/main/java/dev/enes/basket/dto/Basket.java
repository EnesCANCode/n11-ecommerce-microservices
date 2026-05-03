package dev.enes.basket.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Basket implements Serializable {
    private String userId;
    private List<BasketItem> items = new ArrayList<>();

    @com.fasterxml.jackson.annotation.JsonIgnore
    public BigDecimal getTotalAmount() {
        return items.stream()
                .map(BasketItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @com.fasterxml.jackson.annotation.JsonIgnore
    public int getTotalItems() {
        return items.stream().mapToInt(BasketItem::getQuantity).sum();
    }
}
