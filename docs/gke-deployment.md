# GKE Autopilot Deployment Rehberi

## Genel Bakış

Bu proje, **Google Kubernetes Engine (GKE) Autopilot** üzerinde tam otomatik CI/CD pipeline ile deploy edilmektedir. Push → Build → Deploy sürecinin tamamı GitHub Actions üzerinden gerçekleşir.

## Mimari

```
GitHub (Push)
    │
    ▼
GitHub Actions Runner
    ├── 1. Maven Build (Backend)
    ├── 2. Docker Build × 10 (Matrix - Parallel)
    │   └── Push → Artifact Registry (europe-west3)
    └── 3. kubectl apply → GKE Autopilot
         ├── k8s/infra.yaml (PostgreSQL, Redis, RabbitMQ, Keycloak)
         └── k8s/backend.yaml (10 Mikroservis)
```

## Cluster Bilgileri

| Özellik | Değer |
|---------|-------|
| **Cluster Tipi** | GKE Autopilot |
| **Region** | europe-west3 (Frankfurt) |
| **Proje** | n11-ecommerce |
| **Registry** | europe-west3-docker.pkg.dev/n11-ecommerce/n11-repo |
| **Auth** | Workload Identity Federation (OIDC) |

## Pod Listesi (14 Pod)

### Infrastructure (4 Pod)
| Pod | Tip | Port |
|-----|-----|------|
| postgres-0 | StatefulSet | 5432 |
| redis-0 | StatefulSet | 6379 |
| rabbitmq-0 | StatefulSet | 5672, 15672 |
| keycloak | Deployment | 8080 |

### Backend Services (10 Pod)
| Pod | Port | Açıklama |
|-----|------|----------|
| config-server | 8888 | Merkezi yapılandırma |
| discovery-server | 8761 | Eureka service registry |
| api-gateway | 8000 | Routing + JWT validation |
| product-service | 8082 | Ürün CRUD + ES indexing |
| basket-service | 8083 | Redis sepet |
| order-service | 8084 | Saga + Outbox |
| payment-service | 8085 | Strategy Pattern ödeme |
| inventory-service | 8086 | Pessimistic lock stok |
| notification-service | 8087 | WebSocket push |
| mcp-ai-server | 8090 | Spring AI + MCP |

## Kaynak Yönetimi

GKE Autopilot'ta her pod için kaynak limitleri:

```yaml
resources:
  requests:
    memory: "128Mi"
    cpu: "50m"
  limits:
    memory: "512Mi"
    cpu: "500m"
```

### Startup Probe Konfigürasyonu

Spring Boot servisleri yavaş başladığı için `startupProbe` kullanılır:

```yaml
startupProbe:
  httpGet:
    path: /actuator/health/liveness
    port: <service-port>
  failureThreshold: 30    # 30 deneme
  periodSeconds: 10       # 10sn aralık
  # Toplam: 5 dakika başlangıç toleransı
```

Bu sayede Kubernetes, Spring Boot'un tam olarak ayağa kalkmasını bekler ve erken pod kill'i önlenir.

## Güvenlik — Workload Identity Federation

CI/CD pipeline'ında GCP'ye erişim için **service account key yerine** OIDC tabanlı Workload Identity Federation kullanılır:

```yaml
# .github/workflows/ci.yml
- id: auth
  uses: google-github-actions/auth@v2
  with:
    workload_identity_provider: 'projects/.../providers/github'
    service_account: 'github-actions@n11-ecommerce.iam.gserviceaccount.com'
```

**Avantajları:**
- Key rotasyonu gereksiz
- Key sızması riski yok
- Google önerilen best practice

## Deployment Stratejisi

**Recreate** stratejisi kullanılır:

```yaml
strategy:
  type: Recreate
```

**Neden Rolling Update değil?**
- Autopilot'ta kaynak kısıtlaması var
- Rolling update sırasında aynı anda 2× pod çalışır → yetersiz kaynak
- Recreate → önce eski pod silinir → sonra yeni oluşur → güvenli

## Manuel Deployment Komutları

```bash
# Cluster'a bağlan
gcloud container clusters get-credentials n11-cluster \
  --region europe-west3 \
  --project n11-ecommerce

# Infrastructure deploy
kubectl apply -f k8s/infra.yaml

# Backend deploy
kubectl apply -f k8s/backend.yaml

# Pod durumunu kontrol et
kubectl get pods

# Belirli pod logları
kubectl logs -f deployment/order-service

# Pod restart sorunlarında önceki log
kubectl logs --previous deployment/payment-service
```
