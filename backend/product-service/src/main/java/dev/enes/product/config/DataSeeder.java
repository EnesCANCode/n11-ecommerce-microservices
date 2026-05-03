package dev.enes.product.config;

import dev.enes.product.service.BulkDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * DEV profili aktifken uygulama başlangıcında küçük çaplı
 * otomatik veri oluşturma (5 satıcı × 10 ürün = 50 ürün).
 *
 * Büyük veri (yüzlerce satıcı, binlerce ürün) için
 * Admin API endpoint'ini kullanın:
 * POST /api/v1/products/admin/generate?sellerCount=50&productsPerSeller=100
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@Profile("dev")
public class DataSeeder implements CommandLineRunner {

    private final BulkDataService bulkDataService;

    @Override
    public void run(String... args) {
        log.info("🌱 DEV profili aktif — Otomatik veri tohumlama (seed) başlıyor...");
        bulkDataService.generateBulkData(5, 10);
    }
}
