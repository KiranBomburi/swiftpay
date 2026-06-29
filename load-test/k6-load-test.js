import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

// custom metrics to track what matters
// default k6 metrics are fine but these give more insight into payment-specific stuff
const paymentSuccess   = new Counter('payment_success');
const paymentFailed    = new Counter('payment_failed');
const duplicateHit     = new Counter('duplicate_idempotency');
const insufficientFund = new Counter('insufficient_funds');
const errorRate        = new Rate('error_rate');
const paymentDuration  = new Trend('payment_duration_ms', true);

// ── Test configuration ───────────────────────────────────────
// Target: 250 TPS sustained, 1 million total transactions
export const options = {
  scenarios: {
    sustained_load: {
      executor: 'constant-arrival-rate',
      rate: 250,           // 250 requests/second
      timeUnit: '1s',
      duration: '67m',     // ~1,000,000 requests at 250 TPS
      preAllocatedVUs: 300,
      maxVUs: 500,
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<500', 'p(99)<1000'],  // 95th percentile < 500ms
    error_rate: ['rate<0.01'],                         // less than 1% errors
    http_req_failed: ['rate<0.02'],
  },
};

// ── Test users (seeded in DB) ────────────────────────────────
const USERS = ['user_001', 'user_002', 'user_003', 'user_004'];
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

function randomUser() {
  return USERS[Math.floor(Math.random() * USERS.length)];
}

function randomAmount() {
  // Small amounts to avoid hitting insufficient funds frequently
  return (Math.random() * 50 + 1).toFixed(2);
}

// ── Main test function ───────────────────────────────────────
export default function () {
  let sender   = randomUser();
  let receiver = randomUser();
  // Make sure sender != receiver
  while (receiver === sender) {
    receiver = randomUser();
  }

  const payload = JSON.stringify({
    senderId:   sender,
    receiverId: receiver,
    amount:     parseFloat(randomAmount()),
    currency:   'INR',
    // No idempotencyKey — let the gateway generate one each time
  });

  const params = {
    headers: { 'Content-Type': 'application/json' },
    timeout: '10s',
  };

  const start    = Date.now();
  const response = http.post(`${BASE_URL}/v1/payments`, payload, params);
  const duration = Date.now() - start;

  paymentDuration.add(duration);

  const ok = check(response, {
    'status is 202': (r) => r.status === 202,
  });

  if (response.status === 202) {
    paymentSuccess.add(1);
    errorRate.add(0);
  } else if (response.status === 409) {
    duplicateHit.add(1);
    errorRate.add(0); // not a real error
  } else if (response.status === 422) {
    insufficientFund.add(1);
    errorRate.add(0); // expected business rejection
  } else {
    paymentFailed.add(1);
    errorRate.add(1);
    console.error(`Unexpected ${response.status}: ${response.body}`);
  }

  // No sleep — we're using constant-arrival-rate executor
}

// ── Summary handler ──────────────────────────────────────────
export function handleSummary(data) {
  return {
    'load-test-results.json': JSON.stringify(data, null, 2),
    stdout: textSummary(data),
  };
}

function textSummary(data) {
  const reqs     = data.metrics.http_reqs?.values?.count ?? 0;
  const p95      = data.metrics.http_req_duration?.values?.['p(95)'] ?? 0;
  const p99      = data.metrics.http_req_duration?.values?.['p(99)'] ?? 0;
  const errRate  = data.metrics.error_rate?.values?.rate ?? 0;

  return `
╔══════════════════════════════════════════╗
║       SwiftPay Load Test Summary         ║
╠══════════════════════════════════════════╣
║  Total requests    : ${String(reqs).padEnd(19)}║
║  p95 latency       : ${(p95.toFixed(1) + ' ms').padEnd(19)}║
║  p99 latency       : ${(p99.toFixed(1) + ' ms').padEnd(19)}║
║  Error rate        : ${((errRate * 100).toFixed(2) + '%').padEnd(19)}║
╚══════════════════════════════════════════╝
`;
}
