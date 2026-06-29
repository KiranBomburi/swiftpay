# SwiftPay — Real-Time P2P Payment Ledger

Hackathon project. Built a fintech backend that handles peer-to-peer transfers with consistency guarantees, idempotency, and event-driven processing via Kafka.

## Architecture

```
Client
  │
  ▼
Service A — Transaction Gateway (:8080)
  • REST API (POST /v1/payments)
  • Idempotency check via Redis
  • Balance validation (Redis cache + DB fallback)
  • Saves PENDING record → Postgres
  • Fires PaymentInitiated → Kafka
  │
  ▼ Kafka: payment.initiated
  │
Service B — Ledger Service (:8081)
  • Consumes PaymentInitiated
  • Atomic debit/credit with pessimistic DB locks
  • Double-entry ledger records
  • Retry with exponential backoff + DLT
  • Fires PaymentCompleted/PaymentFailed → Kafka
  • GET /v1/ledger/history/{userId}
  │
  ▼ Kafka: payment.completed
  │
Service C — Analytics Worker (:8082)
  • Consumes completed events
  • Writes to analytics table
  • GET /v1/analytics/volume
```

## Stack

- Java 21 + Spring Boot 3.2
- PostgreSQL 16 (one DB per service)
- Apache Kafka + Zookeeper
- Redis (idempotency + balance cache)
- Flyway for migrations
- Swagger/OpenAPI
- Docker Compose
- GitHub Actions CI

## Running locally

```bash
git clone <repo>
cd swiftpay
docker compose up --build
```

Services:
- Gateway: http://localhost:8080
- Ledger: http://localhost:8081
- Analytics: http://localhost:8082
- Kafka UI: http://localhost:9093
- Swagger: http://localhost:8080/swagger-ui.html

## Test it

```bash
# fire a payment
curl -X POST http://localhost:8080/v1/payments \
  -H "Content-Type: application/json" \
  -d '{"senderId":"user_001","receiverId":"user_002","amount":500,"currency":"INR"}'

# check ledger history
curl http://localhost:8081/v1/ledger/history/user_001

# test idempotency - second call returns 409
curl -X POST http://localhost:8080/v1/payments \
  -H "Content-Type: application/json" \
  -d '{"senderId":"user_001","receiverId":"user_002","amount":100,"currency":"INR","idempotencyKey":"my-key-123"}'

# test insufficient funds - returns 422
curl -X POST http://localhost:8080/v1/payments \
  -H "Content-Type: application/json" \
  -d '{"senderId":"user_001","receiverId":"user_002","amount":999999,"currency":"INR"}'

# analytics
curl http://localhost:8082/v1/analytics/volume
```

## Seeded accounts

| User | Balance |
|------|---------|
| user_001 | ₹50,000 |
| user_002 | ₹25,000 |
| user_003 | ₹10,000 |
| user_004 | ₹75,000 |

## Load test

Uses K6. Target: 250 TPS sustained.

```bash
brew install k6
cd load-test
k6 run k6-load-test.js
```

## Notes / known issues

- Balance sync between Gateway and Ledger is eventual consistency. Gateway checks a cached/stale balance, Ledger has ground truth. Fine for hackathon, would need a proper solution in prod.
- Analytics is backed by Postgres (mock OLAP). Ideally ClickHouse.
- No auth yet — would add JWT + Spring Security before going live.
- Single Kafka broker, replication factor 1. Not prod-ready obviously.
