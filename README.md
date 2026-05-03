# N11 E-Ticaret Pazaryeri Mikroservisleri

**Spring Boot 3.4.x**, **React** ve **Spring AI** ile oluşturulmuş, **Google Kubernetes Engine (GKE) Autopilot** üzerinde çalışacak şekilde tasarlanmış modern, ölçeklenebilir ve yüksek erişilebilirliğe sahip e-ticaret pazaryeri platformu.

## 🚀 Mimari Öne Çıkanlar (Senior Seviye Tasarım)

Bu proje sadece standart bir CRUD uygulaması değildir; veri tutarlılığını, dayanıklılığı ve yüksek performansı garanti etmek için karmaşık dağıtık sistem desenlerini (patterns) uygular:

1.  **Saga Choreography Pattern**: `Order`, `Payment`, `Inventory` ve `Notification` servisleri arasında dağıtık işlem (transaction) yönetimi, **RabbitMQ** olay güdümlü (event-driven) koreografisi kullanılarak sağlanır.
2.  **Transactional Outbox Pattern**: İkili yazma (dual-write) hatalarını (veritabanına kaydetme vs. mesaj kuyruğuna gönderme) önler. Olaylar (Events) aynı yerel işlem (transaction) içinde `outbox_events` tablosuna kaydedilir ve bir yoklama planlayıcısı (`OutboxRelayScheduler`) bunları RabbitMQ'ya aktarır.
3.  **Idempotency & Eşzamanlılık Güvenliği**: 
    *   `Idempotency-Key` başlıkları (headers), aynı ödeme isteğinin birden fazla siparişe dönüşmesini engeller.
    *   PostgreSQL'deki `PESSIMISTIC_WRITE` kilitleri, yüksek yoğunluklu flaş indirimlerde stok rezervasyonunun kesin olarak doğru yapılmasını sağlar.
4.  **CQRS & Dual-Write Stratejisi**: `Product Service`, kesin veriyi PostgreSQL'e yazar, ancak çok hızlı tam metin (full-text) ürün aramaları için bu veriyi anında **Elasticsearch**'e indeksler.
5.  **Strategy Tasarım Deseni**: `Payment Service`, temel iş mantığını (Open/Closed Prensibi) değiştirmeden birden fazla ödeme sağlayıcısı (`Iyzico`, `Cüzdan/Wallet`) arasında dinamik geçiş yapar.
6.  **Gerçek Zamanlı Bildirimler**: `Notification Service` RabbitMQ olaylarını tüketir ve **WebSocket (STOMP)** üzerinden React arayüzüne gerçek zamanlı güncellemeler gönderir.
7.  **Yapay Zeka Destekli Ürün Keşfi**: `mcp-ai-server`, **Spring AI** kullanarak Model Context Protocol (MCP) uygular. Bu sayede LLM'lerin (Büyük Dil Modelleri) ürün aramasını, kategorileri getirmesini ve sohbet odaklı bir alışveriş asistanı gibi davranmasını sağlar.
8.  **Tam Gözlemlenebilirlik (Observability)**: **OpenTelemetry** ile dağıtık izleme (tracing), **Prometheus** ile metrikler, **Grafana** ile dashboard'lar ve **Jaeger** ile iz görselleştirme. API Gateway üzerindeki bir `CorrelationIdFilter`, isteklerin tüm mikroservis ekosistemi boyunca takip edilebilmesini sağlar.
9.  **Premium Frontend UI**: React, Vite ve saf CSS ile geliştirilmiş; **Keycloak PKCE** kimlik doğrulamasıyla entegre edilmiş özel bir Tasarım Sistemi (Glassmorphism, Dark Mode, Mikro animasyonlar) barındırır.

---

## 🏗️ Teknoloji Yığını (Tech Stack)

*   **Backend Çekirdeği**: Java 23, Spring Boot 3.4.12, Spring Cloud 2024.0.3 (Eureka, Gateway, Config)
*   **Veritabanı & Önbellek**: PostgreSQL (Her servis için ayrı veritabanı), Redis (Sepet & Caching)
*   **Arama Motoru**: Elasticsearch 8.x
*   **Mesaj Kuyruğu (Message Broker)**: RabbitMQ
*   **Güvenlik**: Keycloak (OAuth2.0 / OpenID Connect)
*   **Yapay Zeka / LLM**: Spring AI, Model Context Protocol (MCP)
*   **Frontend**: React 18, Vite, React Router, Axios, StompJS
*   **DevOps & Altyapı**: Docker, Docker Compose, Kubernetes (GKE), GitHub Actions

---

## 📁 Modül Yapısı

| Modül | Port | Açıklama |
| :--- | :--- | :--- |
| `discovery-server` | 8761 | Netflix Eureka Servis Kayıt Merkezi |
| `config-server` | 8888 | Merkezi Spring Cloud Config Sunucusu |
| `api-gateway` | 8000 | Spring Cloud Gateway, CORS, Correlation ID |
| `common-lib` | - | Ortak DTO'lar, İstisnalar (Exceptions), Event'ler, Entity'ler |
| `product-service` | 8081 | Ürün ve Kategori yönetimi (Postgres + Elastic) |
| `mcp-ai-server` | 8082 | Doğal dilde ürün araması için Yapay Zeka Ajanı |
| `inventory-service`| 8083 | Kötümser kilitleme (pessimistic lock) ile stok yönetimi |
| `basket-service` | 8084 | Redis tabanlı alışveriş sepeti |
| `order-service` | 8085 | Sipariş oluşturma, Outbox deseni, Saga orchestration |
| `payment-service` | 8086 | Strategy deseni üzerinden ödeme işleme |
| `notification-service`| 8087 | Sipariş durumları için WebSocket push bildirimleri |
| `frontend` | 5173 | React SPA Web Uygulaması |

---

## 🛠️ Yerel Geliştirme Kurulumu

### Ön Koşullar
*   Java 23 & Maven 3.9+
*   Node.js 20+
*   Docker & Docker Compose

### 1. Altyapıyı Başlatın (Veritabanları, RabbitMQ, Keycloak vb.)
```bash
cd infra
docker compose up -d
docker compose -f docker-compose.monitoring.yml up -d
```
*Keycloak'ın hazır olmasını bekleyin (yaklaşık 30-60 saniye).*

### 2. Backend Servislerini Derleyin ve Başlatın
```bash
cd backend
mvn clean install -DskipTests
# Önce Discovery ve Config Server'ı başlatın!
# Daha sonra geri kalan servisleri başlatın.
```

### 3. Frontend'i Başlatın
```bash
cd frontend
npm install
npm run dev
```
*Uygulamaya `http://localhost:5173` adresinden erişebilirsiniz.*

### Test Kullanıcı Bilgileri (Keycloak)
*   **Admin (Yönetici)**: `admin` / `admin`
*   **Seller (Satıcı)**: `seller` / `seller`
*   **User (Kullanıcı)**: `user` / `user`

---

## ☁️ Kubernetes Deployment (GKE Autopilot)

Proje tamamen konteynerize edilmiştir ve Kubernetes dağıtımına (deployment) hazırdır. Manifest dosyaları `k8s/` dizininde yer almaktadır.

1.  **İmajları Derleyin ve Gönderin (Push)**: Dockerfile'ları derlediğinizden ve kendi Container Registry'nize (örn. GCR veya Artifact Registry) gönderdiğinizden emin olun.
2.  **Altyapıyı Kurun**: `kubectl apply -f k8s/infra.yaml`
3.  **Backend Servislerini Kurun**: `kubectl apply -f k8s/backend.yaml`
4.  **Frontend'i Kurun**: `kubectl apply -f k8s/frontend.yaml`

*Not: Bir prodüksiyon GKE ortamında, yerel Volume tanımlarını GCP Persistent Disk'lerle değiştirin ve API Gateway / Frontend'i GCP LoadBalancers/Ingress üzerinden dışa açın.*
