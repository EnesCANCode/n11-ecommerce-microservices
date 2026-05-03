# Saga Akışı — Detaylı Sequence Diagram

## Başarılı Sipariş Akışı

```
Client                API Gateway        Order Service        RabbitMQ         Inventory          Payment           Notification
  │                      │                    │                  │                │                  │                  │
  │  POST /api/orders    │                    │                  │                │                  │                  │
  │  Idempotency-Key: x  │                    │                  │                │                  │                  │
  │─────────────────────►│                    │                  │                │                  │                  │
  │                      │  Forward + JWT     │                  │                │                  │                  │
  │                      │───────────────────►│                  │                │                  │                  │
  │                      │                    │                  │                │                  │                  │
  │                      │                    │ ┌──────────────┐ │                │                  │                  │
  │                      │                    │ │ SAME TX:     │ │                │                  │                  │
  │                      │                    │ │ 1. Save Order│ │                │                  │                  │
  │                      │                    │ │    (PENDING) │ │                │                  │                  │
  │                      │                    │ │ 2. Save      │ │                │                  │                  │
  │                      │                    │ │    OutboxEvent│ │                │                  │                  │
  │                      │                    │ └──────────────┘ │                │                  │                  │
  │                      │                    │                  │                │                  │                  │
  │                      │  202 Accepted      │                  │                │                  │                  │
  │  ◄────────────────────────────────────────│                  │                │                  │                  │
  │                      │                    │                  │                │                  │                  │
  │                      │                    │ OutboxScheduler  │                │                  │                  │
  │                      │                    │ (every 2s)       │                │                  │                  │
  │                      │                    │─────────────────►│                │                  │                  │
  │                      │                    │ OrderCreatedEvent│                │                  │                  │
  │                      │                    │                  │ consume         │                  │                  │
  │                      │                    │                  │───────────────►│                  │                  │
  │                      │                    │                  │                │                  │                  │
  │                      │                    │                  │                │ SELECT..FOR UPDATE│                  │
  │                      │                    │                  │                │ Reserve Stock     │                  │
  │                      │                    │                  │                │                  │                  │
  │                      │                    │                  │◄───────────────│                  │                  │
  │                      │                    │                  │ StockReserved   │                  │                  │
  │                      │                    │                  │                                   │                  │
  │                      │                    │                  │──────────────────────────────────►│                  │
  │                      │                    │                  │                                   │ process()        │
  │                      │                    │                  │                                   │ Strategy Pattern │
  │                      │                    │                  │◄──────────────────────────────────│                  │
  │                      │                    │                  │ PaymentCompleted                  │                  │
  │                      │                    │                  │                                                      │
  │                      │                    │◄─────────────────│                                                      │
  │                      │                    │ Update: CONFIRMED│                                                      │
  │                      │                    │                  │─────────────────────────────────────────────────────►│
  │                      │                    │                  │ OrderConfirmedEvent                                  │
  │                      │                    │                  │                                  WebSocket push      │
  │  ◄─────────────────────────────────────────────────────────────────────────────────────────────────────────────────│
  │  WS: "Siparişiniz                        │                  │                                                      │
  │   onaylandı!"                            │                  │                                                      │
```

## Başarısız Akış (Compensation)

```
Inventory Service        RabbitMQ          Order Service         Payment Service
       │                    │                    │                      │
       │ StockFailed        │                    │                      │
       │───────────────────►│                    │                      │
       │                    │───────────────────►│                      │
       │                    │                    │ Update: CANCELLED    │
       │                    │                    │                      │
       │                    │                    │ (Ödeme yapılmışsa)   │
       │                    │                    │─────────────────────►│
       │                    │                    │ RefundRequest        │
       │                    │                    │                      │ refund()
       │                    │                    │◄─────────────────────│
       │                    │                    │ RefundCompleted      │
```

## Idempotency Güvenliği

```
Client                    Order Service              Database
  │                            │                        │
  │ POST /orders               │                        │
  │ Idempotency-Key: abc-123   │                        │
  │───────────────────────────►│                        │
  │                            │ findByIdempotencyKey() │
  │                            │───────────────────────►│
  │                            │ null (yeni)            │
  │                            │◄───────────────────────│
  │                            │ save(order)            │
  │                            │───────────────────────►│
  │ 201 Created                │                        │
  │◄───────────────────────────│                        │
  │                            │                        │
  │ POST /orders (RETRY!)      │                        │
  │ Idempotency-Key: abc-123   │                        │
  │───────────────────────────►│                        │
  │                            │ findByIdempotencyKey() │
  │                            │───────────────────────►│
  │                            │ existing order found   │
  │                            │◄───────────────────────│
  │ 200 OK (same response)     │                        │
  │◄───────────────────────────│                        │
  │ (Duplicate engellendi!)    │                        │
```

## Pessimistic Locking — Stok Güvenliği

```
Thread A (Sipariş 1)        Database              Thread B (Sipariş 2)
       │                       │                         │
       │ SELECT stock          │                         │
       │ WHERE product_id = X  │                         │
       │ FOR UPDATE ───────────►│                         │
       │                       │ LOCK acquired           │
       │                       │                         │
       │                       │         SELECT stock    │
       │                       │         FOR UPDATE ◄────│
       │                       │         ⏳ WAIT...      │
       │                       │                         │
       │ UPDATE stock -= 1     │                         │
       │──────────────────────►│                         │
       │ COMMIT                │                         │
       │──────────────────────►│                         │
       │                       │ LOCK released           │
       │                       │                         │
       │                       │         LOCK acquired   │
       │                       │         stock = 0       │
       │                       │         → REJECT ──────►│
       │                       │         StockFailedEvent │
```
