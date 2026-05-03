package dev.enes.inventory.entity;

import dev.enes.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "inventory")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Inventory extends BaseEntity {

    @Column(nullable = false)
    private UUID productId;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    @Builder.Default
    private int reservedQuantity = 0;

    public int getAvailableQuantity() {
        return quantity - reservedQuantity;
    }

    public void reserve(int amount) {
        if (getAvailableQuantity() < amount) {
            throw new IllegalStateException("Insufficient stock for product: " + productId);
        }
        this.reservedQuantity += amount;
    }

    public void confirmReservation(int amount) {
        this.quantity -= amount;
        this.reservedQuantity -= amount;
    }

    public void releaseReservation(int amount) {
        this.reservedQuantity -= amount;
    }
}
