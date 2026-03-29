# k6 Baseline Scripts

## Purpose

These scripts provide the first repeatable baseline load tests for the bookstore microservices stack.

They focus on:

1. browse books
2. place a direct order
3. run a short smoke flow that mixes reads and writes

## Prerequisites

- `docker compose up -d`
- gateway reachable at `http://localhost:8080`
- auth-server reachable through gateway at `http://localhost:8080/api/v1/oauth2/token`
- Prometheus reachable if you want to compare results with dashboards
- test data includes a valid `userId` and at least one purchasable `bookId`

## Environment Variables

These scripts are parameterized through environment variables:

- `BASE_URL` default: `http://localhost:8080`
- `TOKEN_URL` default: `http://localhost:8080/api/v1/oauth2/token`
- `CLIENT_ID` default: `local-client`
- `CLIENT_SECRET` default: `local-secret`
- `USER_ID` default: `1`
- `BOOK_ID` default: `1`
- `BOOK_SEARCH` default: `java`
- `REQUEST_TIMEOUT` default for `books-browse.js`: `10s`
- `BOOK_PAGE_SIZE` default for `books-browse.js`: `10`

## Scripts

- `smoke-order-flow.js`
  - quick validation run
  - small number of users
  - useful before taking baseline screenshots

- `books-browse.js`
  - read-heavy traffic against catalog endpoints
  - captures baseline browse/search latency

- `checkout-order-flow.js`
  - write-heavy traffic against direct order creation
  - useful for checkout, outbox, catalog consumer, and notification paths

- `checkout-order-flow-stress.js`
  - stronger version of the direct-order scenario
  - use this only after the baseline run is healthy
  - intended to find the first real bottleneck instead of tuning speculatively

## Example Commands

Run smoke flow:

```bash
k6 run perf/smoke-order-flow.js
```

Run browse baseline:

```bash
k6 run -e BASE_URL=http://localhost:8080 -e BOOK_SEARCH=java perf/books-browse.js
```

Run browse in safer debug mode:

```bash
k6 run -e BASE_URL=http://localhost:8080 -e REQUEST_TIMEOUT=10s -e BOOK_PAGE_SIZE=5 perf/books-browse.js
```

Run order baseline:

```bash
k6 run -e BASE_URL=http://localhost:8080 -e USER_ID=1 -e BOOK_ID=1 perf/checkout-order-flow.js
```

Run stronger checkout stress:

```bash
k6 run -e BASE_URL=http://localhost:8080 -e USER_ID=1 -e BOOK_ID=1 perf/checkout-order-flow-stress.js
```

## What To Record After Each Run

- start time and git commit
- scenario name
- virtual users and duration
- throughput
- p50, p95, p99 latency
- error rate
- Grafana screenshots for order, outbox, inventory, and notification panels

## Suggested Progression

Use the scripts in this order:

1. `smoke-order-flow.js`
2. `books-browse.js`
3. `checkout-order-flow.js`
4. `checkout-order-flow-stress.js`

If the baseline scripts stay healthy and Grafana shows no visible lag, do not optimize yet. Move to the stress script and look for the first real signal:

- sharp p95 growth
- request failures
- outbox lag
- consumer lag
- retry spikes
- circuit breaker activity

## Notes

- The order scripts use `client_credentials` token retrieval because that already exists in the repo scripts.
- If your local auth flow differs, pass a token manually or adjust the token helper in the scripts.
- If a book runs out of stock, either reset stock or point `BOOK_ID` to a different seeded record.
- If browse requests time out, compare the same endpoint through gateway and catalog directly before increasing VUs.
  - `http://localhost:8080/api/v1/books?page=0&size=5`
  - `http://localhost:8082/api/v1/books?page=0&size=5`
