package dev.enes.product.service;

import dev.enes.common.dto.PageResponse;
import dev.enes.product.dto.ProductRequest;
import dev.enes.product.dto.ProductResponse;

import java.util.UUID;

public interface ProductService {
    ProductResponse create(ProductRequest request, UUID sellerId, String sellerName);
    ProductResponse update(UUID id, ProductRequest request, UUID sellerId);
    ProductResponse getById(UUID id);
    PageResponse<ProductResponse> getAll(int page, int size, String sort);
    PageResponse<ProductResponse> getByCategory(UUID categoryId, int page, int size, String sort);
    PageResponse<ProductResponse> getBySeller(UUID sellerId, int page, int size);
    PageResponse<ProductResponse> search(String query, int page, int size, String sort);
    void delete(UUID id, UUID sellerId);
}
