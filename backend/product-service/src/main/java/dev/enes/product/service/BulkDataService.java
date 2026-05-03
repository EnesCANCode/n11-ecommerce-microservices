package dev.enes.product.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.enes.common.event.EventConstants;
import dev.enes.product.document.ProductDocument;
import dev.enes.product.entity.Category;
import dev.enes.product.entity.Product;
import dev.enes.product.repository.CategoryRepository;
import dev.enes.product.repository.ProductRepository;
import dev.enes.product.repository.ProductSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.datafaker.Faker;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * Keycloak üzerinden oluşturulan satıcılara DataFaker ile
 * yüksek hacimli (bulk) ürün üreten ve Dual-Write (PostgreSQL + Elasticsearch)
 * yapan servis. Ayrıca her ürün için inventory-service'e stok event'i gönderir.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BulkDataService {

    private final KeycloakAdminService keycloakAdminService;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final ProductSearchRepository searchRepository;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    // 10 zengin Türkçe kategori tanımı
    private static final List<String[]> CATEGORY_DEFS = List.of(
            new String[]{"Elektronik", "Bilgisayar, telefon, tablet ve teknoloji ürünleri", "💻"},
            new String[]{"Giyim & Moda", "Kadın, erkek ve çocuk giyim ürünleri", "👗"},
            new String[]{"Ev & Yaşam", "Mobilya, dekorasyon ve ev gereçleri", "🏠"},
            new String[]{"Spor & Outdoor", "Spor ekipmanları ve outdoor ürünleri", "⚽"},
            new String[]{"Kozmetik & Kişisel Bakım", "Cilt bakımı, makyaj ve parfüm", "💄"},
            new String[]{"Kitap & Hobi", "Kitaplar, müzik aletleri ve hobi malzemeleri", "📚"},
            new String[]{"Oyuncak & Bebek", "Çocuk oyuncakları ve bebek ürünleri", "🧸"},
            new String[]{"Bahçe & Yapı Market", "Bahçe ekipmanları ve yapı malzemeleri", "🌿"},
            new String[]{"Otomotiv & Aksesuar", "Araç aksesuarları ve yedek parçalar", "🚗"},
            new String[]{"Süpermarket & Gıda", "Gıda, içecek ve market ürünleri", "🛒"}
    );

    // Her kategoriye özel ürün isimleri (DataFaker ile kombine edilir)
    private static final Map<String, List<String>> PRODUCT_PREFIXES = Map.of(
            "Elektronik", List.of("Akıllı Telefon", "Laptop", "Tablet", "Kablosuz Kulaklık", "Akıllı Saat", "Monitör", "Mekanik Klavye", "Webcam", "SSD Disk", "Powerbank"),
            "Giyim & Moda", List.of("Slim Fit Gömlek", "Deri Ceket", "Spor Ayakkabı", "Kot Pantolon", "Trençkot", "Sneaker", "Kazak", "Elbise", "Çanta", "Güneş Gözlüğü"),
            "Ev & Yaşam", List.of("Çekyat", "Robot Süpürge", "Airfryer", "Kahve Makinesi", "Masa Lambası", "Halı", "Perde", "Tencere Seti", "Yastık", "Nevresim Takımı"),
            "Spor & Outdoor", List.of("Koşu Bandı", "Yoga Matı", "Dambıl Seti", "Bisiklet", "Kamp Çadırı", "Trekking Ayakkabı", "Uyku Tulumu", "Termos", "Spor Çanta", "Pilates Topu"),
            "Kozmetik & Kişisel Bakım", List.of("Nemlendirici Krem", "Güneş Koruyucu", "Parfüm", "Ruj", "Saç Bakım Seti", "Tıraş Makinesi", "Diş Fırçası", "Duş Jeli", "El Kremi", "Maske"),
            "Kitap & Hobi", List.of("Roman", "Bilim Kurgu", "Puzzle 1000 Parça", "Satranç Takımı", "Boya Seti", "Gitar", "Kulaklık Standı", "Ajanda", "Kalem Seti", "Teleskop"),
            "Oyuncak & Bebek", List.of("LEGO Seti", "Oyuncak Araba", "Peluş Ayı", "Bebek Arabası", "Mama Sandalyesi", "Yapboz", "Drone", "Slime Seti", "Bebek Bezi Paketi", "Emzik"),
            "Bahçe & Yapı Market", List.of("Çim Biçme Makinesi", "Bahçe Hortumu", "El Aletleri Seti", "Saksı", "Gübre", "Bahçe Sandalyesi", "Testere", "Boya Seti", "Duvar Kağıdı", "Matkap"),
            "Otomotiv & Aksesuar", List.of("Araç Kamerası", "Oto Parfüm", "Koltuk Kılıfı", "Bagaj Organizer", "Lastik Pompası", "Akü", "Far Ampulü", "Direksiyon Kılıfı", "Oto Yıkama Seti", "Çakmak Şarj Cihazı"),
            "Süpermarket & Gıda", List.of("Organik Zeytinyağı", "Filtre Kahve", "Çikolata", "Bal", "Bakliyat Seti", "Makarna", "Meyve Suyu", "Kuruyemiş Paketi", "Çay", "Protein Bar")
    );

    /**
     * Tam otomatik toplu veri üretimi.
     * 1. Keycloak'ta satıcılar oluşturulur
     * 2. Kategoriler yoksa eklenir
     * 3. Her satıcı için ürünler DataFaker ile üretilir
     * 4. PostgreSQL + Elasticsearch'e batch olarak yazılır
     * 5. Inventory-service'e stok event'leri gönderilir
     */
    @Async
    @Transactional
    public void generateBulkData(int sellerCount, int productsPerSeller) {
        long startTime = System.currentTimeMillis();
        log.info("🚀 Toplu veri üretimi başlıyor: {} satıcı × {} ürün = {} toplam",
                sellerCount, productsPerSeller, sellerCount * productsPerSeller);

        // 1. Kategorileri oluştur (idempotent)
        List<Category> categories = ensureCategories();

        // 2. Keycloak'ta satıcıları oluştur
        List<KeycloakAdminService.SellerInfo> sellers =
                keycloakAdminService.createSellers(sellerCount, "1234");

        // 3. Her satıcı için ürün üret
        Faker faker = new Faker(new Locale("tr"));
        List<Product> allProducts = new ArrayList<>();
        Random random = new Random();

        for (KeycloakAdminService.SellerInfo seller : sellers) {
            for (int j = 0; j < productsPerSeller; j++) {
                Category category = categories.get(random.nextInt(categories.size()));
                String categoryName = category.getName();
                List<String> prefixes = PRODUCT_PREFIXES.getOrDefault(categoryName, List.of("Ürün"));
                String prefix = prefixes.get(random.nextInt(prefixes.size()));

                // Ürün adı: "Premium Kablosuz Kulaklık - Siyah" gibi
                String productName = prefix + " " + faker.color().name() + " " + faker.number().numberBetween(100, 999);
                String description = faker.lorem().paragraph(2);

                // Kategori bazlı gerçekçi fiyat aralıkları
                BigDecimal price = generateRealisticPrice(categoryName, random);

                // Dinamik fotoğraf URL'si (picsum.photos)
                int imageId = random.nextInt(1, 1000);
                String imageUrl = "https://picsum.photos/seed/" + imageId + "/600/600";

                Product product = Product.builder()
                        .name(productName)
                        .description(description)
                        .price(price)
                        .imageUrl(imageUrl)
                        .sellerId(seller.id())
                        .sellerName(seller.username())
                        .category(category)
                        .active(true)
                        .build();

                allProducts.add(product);
            }
        }

        // 4. Batch halinde PostgreSQL'e yaz (500'lük gruplarla)
        int batchSize = 500;
        List<Product> savedAll = new ArrayList<>();
        for (int i = 0; i < allProducts.size(); i += batchSize) {
            int end = Math.min(i + batchSize, allProducts.size());
            List<Product> batch = allProducts.subList(i, end);
            List<Product> saved = productRepository.saveAll(batch);
            savedAll.addAll(saved);
            log.info("  PostgreSQL batch kaydedildi: {}/{}", end, allProducts.size());
        }

        // 5. Batch halinde Elasticsearch'e yaz
        List<ProductDocument> docs = savedAll.stream().map(p -> ProductDocument.builder()
                .id(p.getId().toString())
                .name(p.getName())
                .description(p.getDescription())
                .price(p.getPrice())
                .categoryName(p.getCategory().getName())
                .sellerId(p.getSellerId().toString())
                .sellerName(p.getSellerName())
                .imageUrl(p.getImageUrl())
                .active(true)
                .build()).toList();

        for (int i = 0; i < docs.size(); i += batchSize) {
            int end = Math.min(i + batchSize, docs.size());
            searchRepository.saveAll(docs.subList(i, end));
            log.info("  Elasticsearch batch indekslendi: {}/{}", end, docs.size());
        }

        // 6. Inventory-service'e stok event'leri gönder
        for (Product p : savedAll) {
            try {
                Map<String, Object> event = Map.of(
                        "productId", p.getId().toString(),
                        "productName", p.getName(),
                        "quantity", new Random().nextInt(50, 500)
                );
                rabbitTemplate.convertAndSend(
                        EventConstants.EXCHANGE,
                        "product.created",
                        objectMapper.writeValueAsString(event)
                );
            } catch (Exception e) {
                log.warn("Stok event'i gönderilemedi: {}", p.getId());
            }
        }

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("✅ Toplu veri üretimi tamamlandı! {} ürün, {} satıcı, {} ms",
                savedAll.size(), sellers.size(), elapsed);
    }

    /**
     * Kategorileri oluşturur (idempotent — varsa tekrar oluşturmaz).
     */
    private List<Category> ensureCategories() {
        if (categoryRepository.count() >= CATEGORY_DEFS.size()) {
            return categoryRepository.findAll();
        }

        List<Category> categories = new ArrayList<>();
        for (String[] def : CATEGORY_DEFS) {
            categoryRepository.findByName(def[0]).ifPresentOrElse(
                    categories::add,
                    () -> categories.add(categoryRepository.save(
                            Category.builder()
                                    .name(def[0])
                                    .description(def[1])
                                    .iconUrl(def[2])
                                    .build()
                    ))
            );
        }
        log.info("Kategoriler hazır: {} adet", categories.size());
        return categories;
    }

    /**
     * Kategori bazlı gerçekçi fiyat üretimi.
     */
    private BigDecimal generateRealisticPrice(String category, Random random) {
        BigDecimal raw = switch (category) {
            case "Elektronik" -> BigDecimal.valueOf(random.nextDouble(1500, 85000));
            case "Giyim & Moda" -> BigDecimal.valueOf(random.nextDouble(99, 8000));
            case "Ev & Yaşam" -> BigDecimal.valueOf(random.nextDouble(49, 25000));
            case "Spor & Outdoor" -> BigDecimal.valueOf(random.nextDouble(79, 15000));
            case "Kozmetik & Kişisel Bakım" -> BigDecimal.valueOf(random.nextDouble(29, 5000));
            case "Kitap & Hobi" -> BigDecimal.valueOf(random.nextDouble(19, 3000));
            case "Oyuncak & Bebek" -> BigDecimal.valueOf(random.nextDouble(39, 4000));
            case "Bahçe & Yapı Market" -> BigDecimal.valueOf(random.nextDouble(29, 12000));
            case "Otomotiv & Aksesuar" -> BigDecimal.valueOf(random.nextDouble(49, 10000));
            case "Süpermarket & Gıda" -> BigDecimal.valueOf(random.nextDouble(9, 500));
            default -> BigDecimal.valueOf(random.nextDouble(19, 5000));
        };
        return raw.setScale(2, RoundingMode.HALF_UP);
    }
}
