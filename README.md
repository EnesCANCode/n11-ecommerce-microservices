# N11 E-Ticaret Pazaryeri — Mikroservis Mimarisi

<div align="center">

**Spring Boot 3.4 · Spring Cloud 2024 · Spring AI (MCP) · React · GKE Autopilot**

[![CI/CD](https://github.com/EnesCANCode/n11-ecommerce-microservices/actions/workflows/ci.yml/badge.svg)](https://github.com/EnesCANCode/n11-ecommerce-microservices/actions)
![Java](https://img.shields.io/badge/Java-23-ED8B00?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.4.12-6DB33F?logo=springboot)
![React](https://img.shields.io/badge/React-18-61DAFB?logo=react)
![Kubernetes](https://img.shields.io/badge/GKE_Autopilot-Deployed-326CE5?logo=kubernetes)
![License](https://img.shields.io/badge/License-MIT-yellow)

</div>

---

## 🎯 Proje Hakkında

N11 TalentHub Backend Bootcamp (patika.dev × n11) bitirme projesi olarak geliştirilen, **10 mikroservis + React frontend** içeren tam kapsamlı e-ticaret platformu. Proje, **Google Kubernetes Engine (GKE) Autopilot** üzerinde canlı olarak çalışmaktadır.

> **Bu proje sadece bir CRUD uygulaması değildir.** Dağıtık sistem desenlerini (Saga, Outbox, CQRS), gerçek dünya güvenlik standartlarını (Keycloak OIDC, Workload Identity Federation) ve AI destekli ürün keşfini (Spring AI + MCP) bir arada uygular.

---

## 🏗️ Mimari Genel Bakış

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        GOOGLE KUBERNETES ENGINE                         │
│                         (Autopilot Cluster)                             │
│  ┌───────────────────────────────────────────────────────────────────┐  │
│  │                         FRONTEND                                  │  │
│  │                    React + Vite (SPA)                              │  │
│  └───────────────────────┬───────────────────────────────────────────┘  │
│                          │                                              │
│  ┌───────────────────────▼───────────────────────────────────────────┐  │
│  │              API GATEWAY (:8000)                                   │  │
│  │        Spring Cloud Gateway · CORS · CorrelationId                │  │
│  └──┬──────┬──────┬──────┬──────┬──────┬──────┬──────┬──────┬───────┘  │
│     │      │      │      │      │      │      │      │      │          │
│  ┌──▼─┐ ┌──▼─┐ ┌──▼─┐ ┌──▼─┐ ┌──▼─┐ ┌──▼─┐ ┌──▼─┐ ┌──▼─┐ ┌──▼──┐   │
│  │Prod│ │Bskt│ │Ordr│ │Pay │ │Inv │ │Notf│ │MCP │ │Cfg │ │Disc │   │
│  │8082│ │8083│ │8084│ │8085│ │8086│ │8087│ │8090│ │8888│ │8761 │   │
│  └──┬─┘ └──┬─┘ └──┬─┘ └──┬─┘ └──┬─┘ └──┬─┘ └────┘ └────┘ └─────┘   │
│     │      │      │      │      │      │                               │
│  ┌──▼──────▼──────▼──────▼──────▼──────▼────────────────────────────┐  │
│  │                    INFRASTRUCTURE                                 │  │
│  │  PostgreSQL · Redis · RabbitMQ · Keycloak                         │  │
│  └───────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## ✨ Öne Çıkan Özellikler

### 🚀 Cloud-Native Deployment (Hiçbir Rakipte Yok!)
- **GKE Autopilot** üzerinde 14 pod ile canlı çalışan prodüksiyon ortamı
- **GitHub Actions CI/CD**: Push → Build (matrix) → Docker Push → GKE Deploy — tam otomatik
- **Workload Identity Federation**: Service account key yerine OIDC tabanlı güvenli auth
- **Artifact Registry**: Docker imajları `europe-west3-docker.pkg.dev` üzerinde

### 🔄 Dağıtık Sistem Desenleri
| Desen | Açıklama |
|-------|----------|
| **Saga Choreography** | Order → Payment → Inventory → Notification arası event-driven koordinasyon |
| **Transactional Outbox** | `outbox_events` tablosu + `OutboxRelayScheduler` ile ikili yazma hatalarını önleme |
| **Idempotency-Key** | Duplicate sipariş engelleme — `UNIQUE` constraint ile DB seviyesinde güvenlik |
| **CQRS + Dual-Write** | PostgreSQL (write) + Elasticsearch (read) — milisaniye altı ürün arama |
| **Pessimistic Locking** | `SELECT ... FOR UPDATE` ile stok yarış koşullarını engelleme |
| **Strategy Pattern** | Payment Provider dinamik geçişi (Iyzico / Wallet) — OCP uyumlu |

### 🤖 AI Destekli Ürün Keşfi
- **Spring AI + Model Context Protocol (MCP)** ile doğal dilde ürün arama
- "Bana 500 TL altı kulaklık göster" → AI ürün filtreleri çıkarır
- MCP Tool'lar: `searchProducts`, `getCategories`, `getProductDetails`

### 🔐 Güvenlik
- **Keycloak** OAuth2.0 / OIDC — PKCE flow ile frontend auth
- **JWT** token validasyonu API Gateway seviyesinde
- Rol bazlı erişim: `ADMIN`, `SELLER`, `USER`

### 📊 Gözlemlenebilirlik (Observability)
- **OpenTelemetry** → dağıtık izleme (distributed tracing)
- **Prometheus** → metrik toplama
- **Grafana** → dashboard görselleştirme
- **CorrelationIdFilter** → request tracking across services

---

## 📁 Proje Yapısı

```
n11-ecommerce-project/
├── backend/
│   ├── api-gateway/           # Spring Cloud Gateway — routing, CORS, auth
│   ├── config-server/         # Merkezi yapılandırma sunucusu
│   ├── discovery-server/      # Netflix Eureka — servis keşfi
│   ├── product-service/       # Ürün CRUD + Elasticsearch indeksleme
│   ├── basket-service/        # Redis tabanlı sepet yönetimi
│   ├── order-service/         # Sipariş + Outbox + Saga orkestrasyon
│   ├── payment-service/       # Strategy pattern ile ödeme işleme
│   ├── inventory-service/     # Pessimistic lock ile stok yönetimi
│   ├── notification-service/  # WebSocket (STOMP) push bildirimleri
│   ├── mcp-ai-server/         # Spring AI + MCP — doğal dil ürün arama
│   └── common-lib/            # Ortak DTO, Event, Exception sınıfları
├── frontend/                  # React 18 + Vite SPA
├── k8s/                       # Kubernetes manifest dosyaları
│   ├── infra.yaml             # PostgreSQL, Redis, RabbitMQ, Keycloak
│   └── backend.yaml           # 10 mikroservis deployment
├── infra/                     # Docker Compose (yerel geliştirme)
├── .github/workflows/
│   └── ci.yml                 # GitHub Actions CI/CD pipeline
└── docs/                      # Mimari dokümantasyon
```

---

## 🔄 Saga Akışı — Sipariş Yaşam Döngüsü

```
Kullanıcı: POST /api/orders (Idempotency-Key header)
    │
    ▼
┌─────────────────────────────────────────────┐
│ 1. Order Service                            │
│    - Sipariş PENDING olarak kaydedilir      │
│    - OutboxEvent aynı TX'te yazılır         │
│    - OutboxRelayScheduler → RabbitMQ        │
└─────────────────┬───────────────────────────┘
                  │ OrderCreatedEvent
                  ▼
┌─────────────────────────────────────────────┐
│ 2. Inventory Service                        │
│    - SELECT ... FOR UPDATE (pessimistic)    │
│    - Stok düşürülür                         │
│    → StockReservedEvent / StockFailedEvent  │
└─────────────────┬───────────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────────┐
│ 3. Payment Service                          │
│    - Strategy Pattern: Iyzico / Wallet      │
│    → PaymentCompletedEvent / FailedEvent    │
└─────────────────┬───────────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────────┐
│ 4. Order → CONFIRMED / CANCELLED            │
│    → NotificationEvent                      │
│    → WebSocket push to React UI             │
└─────────────────────────────────────────────┘

Compensation: Payment FAIL → Stok geri açılır (rollback)
```

---

## 🛠️ Teknoloji Yığını

| Katman | Teknolojiler |
|--------|-------------|
| **Backend** | Java 23, Spring Boot 3.4.12, Spring Cloud 2024.0.3 |
| **API Gateway** | Spring Cloud Gateway, CorrelationId Filter |
| **Service Discovery** | Netflix Eureka |
| **Config** | Spring Cloud Config Server |
| **Database** | PostgreSQL 17 (servis başına ayrı DB) |
| **Cache** | Redis 7 |
| **Message Broker** | RabbitMQ 3.13 |
| **Search** | Elasticsearch 8.x |
| **Auth** | Keycloak 24 (OAuth2 / OIDC / PKCE) |
| **AI** | Spring AI, Model Context Protocol (MCP) |
| **Frontend** | React 18, Vite, React Router, Axios, StompJS |
| **DevOps** | Docker, GitHub Actions, GKE Autopilot, Artifact Registry |
| **Observability** | OpenTelemetry, Prometheus, Grafana, Jaeger |
| **Testing** | JUnit 5, Mockito, AssertJ |

---

## ☁️ CI/CD Pipeline & GKE Deployment

```yaml
Push to main
    │
    ├── Backend Build & Test (Maven)
    │
    ├── Matrix: Dockerize & Push (10 servis paralel)
    │   └── GHA Cache ile hızlı build
    │
    └── Deploy to GKE
        ├── Workload Identity Federation auth
        ├── kubectl apply -f k8s/infra.yaml
        ├── kubectl apply -f k8s/backend.yaml
        └── Verify deployment (5min timeout)
```

### Yerel Geliştirme

```bash
# 1. Altyapıyı başlat
cd infra && docker compose up -d

# 2. Backend'i derle
cd backend && mvn clean install -DskipTests

# 3. Servisleri sırayla başlat
# config-server → discovery-server → diğerleri

# 4. Frontend
cd frontend && npm install && npm run dev
# → http://localhost:5173
```

### Test Kullanıcıları (Keycloak)
| Rol | Kullanıcı | Şifre |
|-----|-----------|-------|
| Admin | `admin` | `admin` |
| Seller | `seller` | `seller` |
| User | `user` | `user` |

---

## 🧪 Testler

```bash
# Tüm testleri çalıştır
cd backend && mvn test

# Belirli bir servis
mvn -pl order-service test

# Test coverage
mvn -pl order-service test jacoco:report
```

Test kapsamı:
- **Unit Tests**: JUnit 5 + Mockito ile servis katmanı testleri
- **Saga Flow Tests**: Outbox event oluşturma, idempotency kontrolü
- **Status Machine Tests**: Sipariş durum geçişleri

---

## 👤 Geliştirici

**Enes Can** — N11 TalentHub Backend Bootcamp (patika.dev × n11) · 2026

---

## 📄 Lisans

Bu proje MIT lisansı ile lisanslanmıştır.
