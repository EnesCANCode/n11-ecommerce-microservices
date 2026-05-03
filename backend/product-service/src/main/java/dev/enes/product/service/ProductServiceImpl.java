package dev.enes.product.service;

import dev.enes.common.dto.PageResponse;
import dev.enes.common.exception.BusinessException;
import dev.enes.common.exception.ResourceNotFoundException;
import dev.enes.product.document.ProductDocument;
import dev.enes.product.dto.ProductRequest;
import dev.enes.product.dto.ProductResponse;
import dev.enes.product.entity.Category;
import dev.enes.product.entity.Product;
import dev.enes.product.repository.CategoryRepository;
import dev.enes.product.repository.ProductRepository;
import dev.enes.product.repository.ProductSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductSearchRepository searchRepository;

    @Override
    @Transactional
    public ProductResponse create(ProductRequest request, UUID sellerId, String sellerName) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", request.getCategoryId()));

        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .imageUrl(request.getImageUrl())
                .sellerId(sellerId)
                .sellerName(sellerName)
                .category(category)
                .build();

        product = productRepository.save(product);
        indexProduct(product);

        log.info("Product created: {} by seller: {}", product.getId(), sellerId);
        return toResponse(product);
    }

    @Override
    @Transactional
    public ProductResponse update(UUID id, ProductRequest request, UUID sellerId) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));

        if (!product.getSellerId().equals(sellerId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "You can only update your own products");
        }

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", request.getCategoryId()));

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setImageUrl(request.getImageUrl());
        product.setCategory(category);

        product = productRepository.save(product);
        indexProduct(product);
        return toResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getById(UUID id) {
        return productRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> getAll(int page, int size, String sort) {
        Page<Product> products = productRepository.findByActiveTrue(PageRequest.of(page, size, getSortOrder(sort)));
        return toPageResponse(products);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> getByCategory(UUID categoryId, int page, int size, String sort) {
        Page<Product> products = productRepository.findByCategoryIdAndActiveTrue(
                categoryId, PageRequest.of(page, size, getSortOrder(sort)));
        return toPageResponse(products);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> getBySeller(UUID sellerId, int page, int size) {
        Page<Product> products = productRepository.findBySellerIdAndActiveTrue(
                sellerId, PageRequest.of(page, size, Sort.by("createdAt").descending()));
        return toPageResponse(products);
    }

    @Override
    public PageResponse<ProductResponse> search(String query, int page, int size, String sort) {
        Sort sortOrder = sort != null && sort.equalsIgnoreCase("price_asc")
                ? Sort.by("price").ascending()
                : sort != null && sort.equalsIgnoreCase("price_desc")
                ? Sort.by("price").descending()
                : Sort.unsorted();

        Page<ProductDocument> docs = searchRepository.findByNameContainingOrDescriptionContaining(
                query, query, PageRequest.of(page, size, sortOrder));

        return PageResponse.of(
                docs.getContent().stream().map(this::fromDocument).toList(),
                page, size, docs.getTotalElements(), docs.getTotalPages(), docs.isLast());
    }

    @Override
    @Transactional
    public void delete(UUID id, UUID sellerId) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));

        if (!product.getSellerId().equals(sellerId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "You can only delete your own products");
        }

        product.setActive(false);
        productRepository.save(product);
        searchRepository.deleteById(id.toString());
        log.info("Product soft-deleted: {}", id);
    }

    private Sort getSortOrder(String sort) {
        return sort != null && sort.equalsIgnoreCase("price_asc")
                ? Sort.by("price").ascending()
                : sort != null && sort.equalsIgnoreCase("price_desc")
                ? Sort.by("price").descending()
                : Sort.by("createdAt").descending();
    }

    private void indexProduct(Product product) {
        try {
            searchRepository.save(ProductDocument.builder()
                    .id(product.getId().toString())
                    .name(product.getName())
                    .description(product.getDescription())
                    .price(product.getPrice())
                    .categoryName(product.getCategory().getName())
                    .sellerId(product.getSellerId().toString())
                    .sellerName(product.getSellerName())
                    .imageUrl(product.getImageUrl())
                    .active(product.isActive())
                    .build());
        } catch (Exception e) {
            log.warn("Failed to index product {}: {}", product.getId(), e.getMessage());
        }
    }

    private ProductResponse toResponse(Product p) {
        return ProductResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .description(p.getDescription())
                .price(p.getPrice())
                .imageUrl(p.getImageUrl())
                .sellerId(p.getSellerId())
                .sellerName(p.getSellerName())
                .categoryName(p.getCategory() != null ? p.getCategory().getName() : null)
                .categoryId(p.getCategory() != null ? p.getCategory().getId() : null)
                .active(p.isActive())
                .createdAt(p.getCreatedAt())
                .build();
    }

    private ProductResponse fromDocument(ProductDocument d) {
        return ProductResponse.builder()
                .id(UUID.fromString(d.getId()))
                .name(d.getName())
                .description(d.getDescription())
                .price(d.getPrice())
                .categoryName(d.getCategoryName())
                .sellerId(UUID.fromString(d.getSellerId()))
                .sellerName(d.getSellerName())
                .imageUrl(d.getImageUrl())
                .active(d.isActive())
                .build();
    }

    private PageResponse<ProductResponse> toPageResponse(Page<Product> products) {
        return PageResponse.of(
                products.getContent().stream().map(this::toResponse).toList(),
                products.getNumber(), products.getSize(),
                products.getTotalElements(), products.getTotalPages(), products.isLast());
    }
}
