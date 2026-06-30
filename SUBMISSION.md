# SwiftPay — Submission Notes

## Load Test Results (250 TPS / 1M transactions)

Ran with K6 against the local docker-compose stack.

```
Total requests    : 1,003,969
p95 latency       : 64.1 ms
p99 latency       : (see load-test-results.json for full breakdown)
Error rate        : 0.00%
Duration          : 1h 7m
Interrupted iters : 0
```

Full structured results: [`load-test/load-test-results.json`](./load-test/load-test-results.json)

PCAP trace (890MB, zipped to 147MB — too large for GitHub's 100MB limit):
**[Download PCAP trace from Google Drive](https://drive.google.com/file/d/1at0lDuNBYZUXQ1mvULLTgbhXgGh9aEmt/view?usp=sharing)**

## Functional Requirements — Answered

**Does the end-to-end payment flow work?**
Yes. POST /v1/payments → PENDING saved to Postgres → PaymentInitiated fired to Kafka → Ledger Service consumes it → atomic debit/credit with pessimistic locking → PaymentCompleted fired → Analytics Worker consumes and records it. Verified manually and under the 1M-request load test above with 0% error rate.

**Does it handle "insufficient funds"?**
Yes, at two layers:
1. Gateway does an initial balance check (cached or DB) and returns `422 INSUFFICIENT_FUNDS` immediately if insufficient.
2. Ledger Service re-validates balance at processing time (since the gateway's check can be stale) and marks the payment FAILED if the sender's balance has changed since the initial check.

**Does `docker compose up` start the whole environment successfully?**
Yes. `docker compose up --build` brings up Postgres (x3), Kafka, Zookeeper, Redis, Kafka UI, and all 3 Spring Boot services with health checks.

**How does the system handle a Kafka outage?**
The Ledger Service's consumer is wrapped with `@RetryableTopic` — 5 attempts with exponential backoff (2s → 4s → 8s → 16s → 30s cap). If all retries are exhausted, the event is routed to a Dead Letter Topic (`payment.initiated.dlt`) for manual review rather than being silently dropped.

**How does the system handle a Database constraint violation?**
Caught in the service layer. The payment transaction is rolled back and the result is marked FAILED with a reason. A `PaymentFailed` event is then published back to Kafka, so downstream consumers — and the original requester, via the status endpoint — are aware.

## API Documentation

- Gateway Swagger UI: http://localhost:8080/swagger-ui.html
- Ledger Swagger UI: http://localhost:8081/swagger-ui.html

## Architecture

See [README.md](./README.md) for the full architecture diagram and setup instructions.