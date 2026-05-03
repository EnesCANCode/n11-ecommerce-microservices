package dev.enes.inventory.service;

import dev.enes.common.exception.ResourceNotFoundException;
import dev.enes.inventory.dto.InventoryRequest;
import dev.enes.inventory.dto.InventoryResponse;
import dev.enes.inventory.entity.Inventory;
import dev.enes.inventory.repository.InventoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("InventoryService Unit Tests — Pessimistic Locking & Stock Safety")
class InventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private InventoryService inventoryService;

    private UUID productId;

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();
    }

    @Nested
    @DisplayName("Stock Reservation (Pessimistic Lock)")
    class ReserveStockTests {

        @Test
        @DisplayName("Should reserve stock when sufficient quantity available")
        void reserveStock_shouldReserveWhenSufficient() {
            // Arrange
            Inventory inventory = Inventory.builder()
                    .productId(productId)
                    .quantity(100)
                    .reservedQuantity(0)
                    .build();
            when(inventoryRepository.findByProductIdForUpdate(productId))
                    .thenReturn(Optional.of(inventory));

            // Act
            inventoryService.reserveStock(productId, 5);

            // Assert
            assertEquals(5, inventory.getReservedQuantity());
            assertEquals(95, inventory.getAvailableQuantity());
            verify(inventoryRepository).save(inventory);
        }

        @Test
        @DisplayName("Should throw when insufficient stock — race condition guard")
        void reserveStock_shouldThrowWhenInsufficient() {
            // Arrange — only 3 available, trying to reserve 5
            Inventory inventory = Inventory.builder()
                    .productId(productId)
                    .quantity(10)
                    .reservedQuantity(7)
                    .build();
            when(inventoryRepository.findByProductIdForUpdate(productId))
                    .thenReturn(Optional.of(inventory));

            // Act & Assert
            assertThrows(IllegalStateException.class,
                    () -> inventoryService.reserveStock(productId, 5));
        }

        @Test
        @DisplayName("Should throw when product inventory not found")
        void reserveStock_shouldThrowWhenNotFound() {
            // Arrange
            when(inventoryRepository.findByProductIdForUpdate(productId))
                    .thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(ResourceNotFoundException.class,
                    () -> inventoryService.reserveStock(productId, 1));
        }
    }

    @Nested
    @DisplayName("Stock Release (Compensation)")
    class ReleaseStockTests {

        @Test
        @DisplayName("Should release reserved stock — Saga compensation flow")
        void releaseStock_shouldReleaseReservation() {
            // Arrange
            Inventory inventory = Inventory.builder()
                    .productId(productId)
                    .quantity(100)
                    .reservedQuantity(10)
                    .build();
            when(inventoryRepository.findByProductIdForUpdate(productId))
                    .thenReturn(Optional.of(inventory));

            // Act
            inventoryService.releaseStock(productId, 5);

            // Assert
            assertEquals(5, inventory.getReservedQuantity());
            assertEquals(95, inventory.getAvailableQuantity());
            verify(inventoryRepository).save(inventory);
        }
    }

    @Nested
    @DisplayName("Stock Confirmation")
    class ConfirmStockTests {

        @Test
        @DisplayName("Should confirm reservation — deduct from total quantity")
        void confirmStock_shouldDeductFromTotal() {
            // Arrange
            Inventory inventory = Inventory.builder()
                    .productId(productId)
                    .quantity(100)
                    .reservedQuantity(10)
                    .build();
            when(inventoryRepository.findByProductIdForUpdate(productId))
                    .thenReturn(Optional.of(inventory));

            // Act
            inventoryService.confirmStock(productId, 5);

            // Assert — quantity reduced, reservation reduced
            assertEquals(95, inventory.getQuantity());
            assertEquals(5, inventory.getReservedQuantity());
            verify(inventoryRepository).save(inventory);
        }
    }

    @Nested
    @DisplayName("Inventory CRUD")
    class CrudTests {

        @Test
        @DisplayName("Should create new inventory for product")
        void createOrUpdate_shouldCreateNew() {
            // Arrange
            InventoryRequest request = new InventoryRequest();
            request.setProductId(productId);
            request.setQuantity(50);

            when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.empty());
            when(inventoryRepository.save(any(Inventory.class))).thenAnswer(inv -> {
                Inventory i = inv.getArgument(0);
                i.setId(UUID.randomUUID());
                return i;
            });

            // Act
            InventoryResponse response = inventoryService.createOrUpdate(request);

            // Assert
            assertNotNull(response);
            assertEquals(productId, response.getProductId());
            assertEquals(50, response.getQuantity());
        }

        @Test
        @DisplayName("Should return inventory by product ID")
        void getByProductId_shouldReturnInventory() {
            // Arrange
            Inventory inventory = Inventory.builder()
                    .productId(productId)
                    .quantity(100)
                    .reservedQuantity(20)
                    .build();
            inventory.setId(UUID.randomUUID());

            when(inventoryRepository.findByProductId(productId))
                    .thenReturn(Optional.of(inventory));

            // Act
            InventoryResponse response = inventoryService.getByProductId(productId);

            // Assert
            assertEquals(100, response.getQuantity());
            assertEquals(20, response.getReservedQuantity());
            assertEquals(80, response.getAvailableQuantity());
        }
    }
}
