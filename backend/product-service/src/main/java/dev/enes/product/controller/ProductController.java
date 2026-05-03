package dev.enes.product.controller;

import dev.enes.common.dto.ApiResponse;
import dev.enes.common.dto.PageResponse;
import dev.enes.product.dto.ProductRequest;
import dev.enes.product.dto.ProductResponse;
import dev.enes.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Tag(name = "Product Controller", description = "Ürün yönetimi ve arama API'ları")
public class ProductController {

    private final ProductService productService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ProductResponse> create(
            @Valid @RequestBody ProductRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID sellerId = UUID.fromString(jwt.getSubject());
        String sellerName = jwt.getClaimAsString("preferred_username");
        return ApiResponse.created(productService.create(request, sellerId, sellerName));
    }

    @PutMapping("/{id}")
    public ApiResponse<ProductResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody ProductRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID sellerId = UUID.fromString(jwt.getSubject());
        return ApiResponse.ok(productService.update(id, request, sellerId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "ID ile ürün getir", description = "Verilen UUID'ye sahip ürünün detaylarını döner.")
    public ApiResponse<ProductResponse> getById(@PathVariable UUID id) {
        return ApiResponse.ok(productService.getById(id));
    }

    @GetMapping
    @Operation(summary = "Tüm ürünleri listele", description = "Sayfalamalı ve sıralamalı olarak tüm ürünleri döner.")
    public ApiResponse<PageResponse<ProductResponse>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort) {
        return ApiResponse.ok(productService.getAll(page, size, sort));
    }

    @GetMapping("/category/{categoryId}")
    public ApiResponse<PageResponse<ProductResponse>> getByCategory(
            @PathVariable UUID categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort) {
        return ApiResponse.ok(productService.getByCategory(categoryId, page, size, sort));
    }

    @GetMapping("/seller/{sellerId}")
    public ApiResponse<PageResponse<ProductResponse>> getBySeller(
            @PathVariable UUID sellerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(productService.getBySeller(sellerId, page, size));
    }

    @GetMapping("/search")
    @Operation(summary = "Ürün ara", description = "Elasticsearch kullanarak ürünler içinde metin araması yapar.")
    public ApiResponse<PageResponse<ProductResponse>> search(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort) {
        return ApiResponse.ok(productService.search(q, page, size, sort));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        UUID sellerId = UUID.fromString(jwt.getSubject());
        productService.delete(id, sellerId);
    }
}
