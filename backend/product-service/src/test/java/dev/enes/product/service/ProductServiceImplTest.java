package dev.enes.product.service;

import dev.enes.common.exception.BusinessException;
import dev.enes.common.exception.ResourceNotFoundException;
import dev.enes.product.dto.ProductRequest;
import dev.enes.product.dto.ProductResponse;
import dev.enes.product.entity.Category;
import dev.enes.product.entity.Product;
import dev.enes.product.repository.CategoryRepository;
import dev.enes.product.repository.ProductRepository;
import dev.enes.product.repository.ProductSearchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductService Unit Tests")
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductSearchRepository searchRepository;

    @InjectMocks
    private ProductServiceImpl productService;

    private UUID sellerId;
    private UUID categoryId;
    private UUID productId;
    private Category category;
    private ProductRequest productRequest;

    @BeforeEach
    void setUp() {
        sellerId = UUID.randomUUID();
        categoryId = UUID.randomUUID();
        productId = UUID.randomUUID();

        category = new Category();
        category.setId(categoryId);
        category.setName("Electronics");

        productRequest = new ProductRequest();
        productRequest.setName("Test Laptop");
        productRequest.setDescription("High performance laptop");
        productRequest.setPrice(new BigDecimal("15000.00"));
        productRequest.setImageUrl("https://example.com/laptop.jpg");
        productRequest.setCategoryId(categoryId);
    }

    @Nested
    @DisplayName("Create Product")
    class CreateProductTests {

        @Test
        @DisplayName("Should create product successfully and index to Elasticsearch")
        void create_shouldSaveProductAndIndex() {
            // Arrange
            when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
            when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
                Product p = invocation.getArgument(0);
                p.setId(productId);
                return p;
            });

            // Act
            ProductResponse response = productService.create(productRequest, sellerId, "Test Seller");

            // Assert
            assertNotNull(response);
            assertEquals("Test Laptop", response.getName());
            assertEquals(new BigDecimal("15000.00"), response.getPrice());
            assertEquals("Electronics", response.getCategoryName());
            assertEquals(sellerId, response.getSellerId());
            assertTrue(response.isActive());

            // Verify Elasticsearch indexing
            verify(searchRepository).save(any());
            verify(productRepository).save(any(Product.class));
        }

        @Test
        @DisplayName("Should throw exception when category not found")
        void create_shouldThrowWhenCategoryNotFound() {
            // Arrange
            when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(ResourceNotFoundException.class,
                    () -> productService.create(productRequest, sellerId, "Seller"));
            verify(productRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Update Product")
    class UpdateProductTests {

        @Test
        @DisplayName("Should update product successfully")
        void update_shouldUpdateProduct() {
            // Arrange
            Product existingProduct = Product.builder()
                    .name("Old Name")
                    .price(new BigDecimal("10000.00"))
                    .sellerId(sellerId)
                    .category(category)
                    .active(true)
                    .build();
            existingProduct.setId(productId);

            when(productRepository.findById(productId)).thenReturn(Optional.of(existingProduct));
            when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
            when(productRepository.save(any(Product.class))).thenReturn(existingProduct);

            // Act
            ProductResponse response = productService.update(productId, productRequest, sellerId);

            // Assert
            assertNotNull(response);
            assertEquals("Test Laptop", response.getName());

            ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
            verify(productRepository).save(captor.capture());
            assertEquals(new BigDecimal("15000.00"), captor.getValue().getPrice());
        }

        @Test
        @DisplayName("Should throw FORBIDDEN when seller tries to update another seller's product")
        void update_shouldThrowWhenNotOwner() {
            // Arrange
            UUID differentSellerId = UUID.randomUUID();
            Product existingProduct = Product.builder()
                    .sellerId(differentSellerId) // different seller
                    .category(category)
                    .build();
            existingProduct.setId(productId);

            when(productRepository.findById(productId)).thenReturn(Optional.of(existingProduct));

            // Act & Assert
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> productService.update(productId, productRequest, sellerId));
            assertTrue(exception.getMessage().contains("own products"));
        }
    }

    @Nested
    @DisplayName("Get Product")
    class GetProductTests {

        @Test
        @DisplayName("Should return product by ID")
        void getById_shouldReturnProduct() {
            // Arrange
            Product product = Product.builder()
                    .name("Test Product")
                    .price(new BigDecimal("100.00"))
                    .sellerId(sellerId)
                    .category(category)
                    .active(true)
                    .build();
            product.setId(productId);

            when(productRepository.findById(productId)).thenReturn(Optional.of(product));

            // Act
            ProductResponse response = productService.getById(productId);

            // Assert
            assertNotNull(response);
            assertEquals(productId, response.getId());
            assertEquals("Test Product", response.getName());
        }

        @Test
        @DisplayName("Should throw when product not found")
        void getById_shouldThrowWhenNotFound() {
            // Arrange
            when(productRepository.findById(productId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(ResourceNotFoundException.class,
                    () -> productService.getById(productId));
        }
    }

    @Nested
    @DisplayName("Delete Product (Soft Delete)")
    class DeleteProductTests {

        @Test
        @DisplayName("Should soft-delete product and remove from Elasticsearch")
        void delete_shouldSoftDeleteProduct() {
            // Arrange
            Product product = Product.builder()
                    .sellerId(sellerId)
                    .category(category)
                    .active(true)
                    .build();
            product.setId(productId);

            when(productRepository.findById(productId)).thenReturn(Optional.of(product));

            // Act
            productService.delete(productId, sellerId);

            // Assert
            assertFalse(product.isActive()); // soft-deleted
            verify(productRepository).save(product);
            verify(searchRepository).deleteById(productId.toString());
        }

        @Test
        @DisplayName("Should throw FORBIDDEN when non-owner tries to delete")
        void delete_shouldThrowWhenNotOwner() {
            // Arrange
            UUID otherSellerId = UUID.randomUUID();
            Product product = Product.builder()
                    .sellerId(otherSellerId)
                    .build();
            product.setId(productId);

            when(productRepository.findById(productId)).thenReturn(Optional.of(product));

            // Act & Assert
            assertThrows(BusinessException.class,
                    () -> productService.delete(productId, sellerId));
            verify(productRepository, never()).save(any());
        }
    }
}
