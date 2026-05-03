package dev.enes.product.controller;

import dev.enes.common.dto.ApiResponse;
import dev.enes.product.service.BulkDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Admin seviyesinde toplu veri üretim endpoint'i.
 * Swagger/Postman üzerinden tetiklenerek Keycloak'ta satıcılar oluşturur
 * ve her satıcı için DataFaker ile ürünler üretir.
 */
@RestController
@RequestMapping("/api/v1/products/admin")
@RequiredArgsConstructor
public class ProductAdminController {

    private final BulkDataService bulkDataService;

    /**
     * Toplu veri üretimini başlatır.
     *
     * Örnek: POST /api/v1/products/admin/generate?sellerCount=10&productsPerSeller=100
     * Bu, Keycloak'ta 10 satıcı oluşturur ve her birine 100 ürün ekler (toplam 1000 ürün).
     *
     * Satıcılar seller_1 / 1234, seller_2 / 1234, ... şifreleriyle Keycloak'a giriş yapabilir.
     *
     * @param sellerCount       oluşturulacak satıcı sayısı (varsayılan: 5)
     * @param productsPerSeller her satıcı başına ürün sayısı (varsayılan: 20)
     */
    @PostMapping("/generate")
    public ApiResponse<String> generateBulkData(
            @RequestParam(defaultValue = "5") int sellerCount,
            @RequestParam(defaultValue = "20") int productsPerSeller) {

        bulkDataService.generateBulkData(sellerCount, productsPerSeller);

        return ApiResponse.ok(String.format(
                "Toplu veri üretimi başlatıldı: %d satıcı × %d ürün = %d toplam. " +
                "İlerleme için konsol loglarını takip edin.",
                sellerCount, productsPerSeller, sellerCount * productsPerSeller));
    }
}
