# Mimari Karar Kayıtları (Architecture Decision Records)

Bu belge, proje boyunca alınan önemli mimari kararları ve gerekçelerini açıklar.

---

## ADR-001: Saga Choreography Pattern Seçimi

**Tarih**: Nisan 2026  
**Durum**: Kabul Edildi

### Bağlam
Order, Payment, Inventory ve Notification servisleri arasında dağıtık işlem yönetimi gerekiyor. İki ana yaklaşım var:
1. **Saga Orchestration**: Merkezi bir orkestratör tüm adımları yönetir
2. **Saga Choreography**: Servisler event'ler aracılığıyla birbirlerini tetikler

### Karar
**Choreography** yaklaşımı seçildi.

### Gerekçe
- **Düşük coupling**: Servisler birbirine doğrudan bağımlı değil
- **Basitlik**: 4 servis için orchestrator overhead gereksiz
- **Ölçeklenebilirlik**: Her servis bağımsız scale edilebilir
- **RabbitMQ**: Zaten altyapıda mevcut, ek maliyet yok

### Trade-off
- Akışı izlemek için CorrelationId kullanılır
- Hata ayıklama orchestration'a göre daha zor olabilir

---

## ADR-002: Transactional Outbox Pattern

**Tarih**: Nisan 2026  
**Durum**: Kabul Edildi

### Bağlam
Order servisi veritabanına yazıp ardından RabbitMQ'ya event publish ettiğinde, ikisi farklı transaction'larda gerçekleşir. DB commit başarılı olup MQ publish başarısız olursa → **inconsistent state**.

### Karar
**Outbox Pattern** uygulandı:
1. Event, `outbox_events` tablosuna aynı DB transaction içinde yazılır
2. `OutboxRelayScheduler` periyodik olarak tabloyu tarar
3. Yayınlanmamış event'leri RabbitMQ'ya gönderir
4. Başarılı publish sonrası event'i işaretler

### Gerekçe
- DB transaction garantisi ile event kaybı önlenir
- At-least-once delivery sağlanır
- Idempotency-Key ile duplicate event güvenliği

---

## ADR-003: CQRS ile Dual-Write (PostgreSQL + Elasticsearch)

**Tarih**: Nisan 2026  
**Durum**: Kabul Edildi

### Bağlam
Ürün aramasının çok hızlı olması gerekiyor. PostgreSQL LIKE sorguları performans için yetersiz.

### Karar
- **Write**: PostgreSQL'e kesin veri kaydı
- **Read**: Elasticsearch'e anlık indeksleme
- Product Service hem JPA hem Elasticsearch repository kullanır

### Gerekçe
- Elasticsearch milisaniye altı full-text search sağlar
- PostgreSQL veri tutarlılığını garanti eder
- İndeksleme hatası durumunda log + retry mekanizması

---

## ADR-004: Strategy Pattern ile Ödeme Sağlayıcı Yönetimi

**Tarih**: Nisan 2026  
**Durum**: Kabul Edildi

### Bağlam
Birden fazla ödeme yöntemi desteklenmeli (kredi kartı, cüzdan vb.) ve yeni sağlayıcılar kolayca eklenebilmeli.

### Karar
Strategy Design Pattern uygulandı:
```java
interface PaymentProvider {
    PaymentResult process(PaymentRequest request);
}

class IyzicoProvider implements PaymentProvider { ... }
class WalletProvider implements PaymentProvider { ... }
```

### Gerekçe
- **Open/Closed Principle**: Yeni sağlayıcı eklemek mevcut kodu değiştirmez
- **Test edilebilirlik**: Mock provider ile unit test kolaylığı
- Runtime'da sağlayıcı seçimi

---

## ADR-005: GKE Autopilot Deployment

**Tarih**: Mayıs 2026  
**Durum**: Kabul Edildi

### Bağlam
Proje, gerçek cloud ortamında çalışabilmelidir. Seçenekler:
1. Docker Compose ile VPS
2. GKE Standard
3. GKE Autopilot

### Karar
**GKE Autopilot** seçildi.

### Gerekçe
- Node yönetimi gereksiz — Google otomatik yönetir
- Pod bazlı fiyatlandırma — maliyet optimizasyonu
- Kubernetes-native deployment — prodüksiyona en yakın deneyim
- GitHub Actions + Workload Identity Federation ile güvenli CI/CD

### Konfigürasyon Detayları
- Recreate deployment strategy (kaynak kısıtları nedeniyle)
- StartupProbe: 30 × 10s = 5dk Spring Boot başlangıç toleransı
- Per-pod: 128Mi-512Mi RAM, 50m-500m CPU

---

## ADR-006: Keycloak OAuth2.0 / OIDC

**Tarih**: Nisan 2026  
**Durum**: Kabul Edildi

### Bağlam
Kullanıcı kimlik doğrulaması ve yetkilendirme sistemi gerekli.

### Karar
**Keycloak** Identity Provider olarak seçildi.

### Gerekçe
- OAuth2.0 + OIDC standartları
- PKCE flow ile SPA güvenliği
- Rol bazlı erişim kontrolü (RBAC)
- Realm/Client/Role yönetimi tek yerden
- Token validasyonu API Gateway seviyesinde — servisler JWT parse etmek zorunda değil

---

## ADR-007: Spring AI + Model Context Protocol (MCP)

**Tarih**: Nisan 2026  
**Durum**: Kabul Edildi

### Bağlam
Kullanıcıların doğal dilde ürün araması yapabilmesi isteniyor.

### Karar
- **Spring AI** ile LLM entegrasyonu
- **MCP (Model Context Protocol)** ile tool-based interaction
- `mcp-ai-server` bağımsız mikroservis olarak çalışır

### MCP Tool'lar
| Tool | Açıklama |
|------|----------|
| `searchProducts` | Doğal dil → ürün filtresi çevirimi |
| `getCategories` | Mevcut kategori listesi |
| `getProductDetails` | Tekil ürün detayı |

### Gerekçe
- MCP, LLM'lere yapılandırılmış veri erişimi sağlar
- Spring AI, Spring ekosistemiyle doğal entegrasyon
- Fallback mekanizması ile LLM erişilemezliğinde keyword arama
