# inventory-api

Java 17 / Spring Boot service for **soft inventory reservations**. Checkout holds stock for a TTL, then either commits (decrements on-hand) or releases it back. The in-memory store is a stand-in for a durable store; the service boundary is the part meant to stay stable.

## Why this shape

Retail checkouts retry. Duplicate `POST /v1/reservations` with the same `Idempotency-Key` must not double-reserve. Multi-SKU carts can fail halfway; reserved lines are rolled back before the error returns. Concurrent holds on one SKU are serialized per SKU so on-hand never goes negative.

Fair-share allocation (`FairShareAllocator`) is the algorithm used when a planner needs to spread scarce units across competing demand instead of first-come-first-served.

## API

| Method | Path | Notes |
| --- | --- | --- |
| `POST` | `/v1/reservations` | Header `Idempotency-Key` optional |
| `GET` | `/v1/reservations/{id}` | |
| `POST` | `/v1/reservations/{id}/commit` | Decrements on-hand |
| `POST` | `/v1/reservations/{id}/release` | Returns units to available |
| `GET` | `/v1/skus/{sku}/availability` | |
| `GET` | `/actuator/health` | Liveness |

Create body:

```json
{
  "customerId": "store-9",
  "lines": { "WIDGET": 2, "GADGET": 1 }
}
```

## Run

Requires JDK 17 and Maven.

```bash
mvn test
mvn spring-boot:run
```

Docker:

```bash
docker build -t inventory-api .
docker run -p 8080:8080 inventory-api
```

## Design notes

- **Locking:** `SkuStock` uses a per-SKU `ReentrantLock`. Multi-SKU reservations sort SKU codes so lock acquisition order is stable if this is later moved to `lock()` across the cart.
- **Expiry:** a 30s sweep converts stale `HELD` rows to `EXPIRED`. See `CONTRIBUTING.md` for the heap-based follow-up when this scan becomes a bottleneck.
- **Tests:** unit tests for the allocator, service tests for idempotency / rollback / concurrent oversell, and a MockMvc slice for HTTP contracts.

Default on-hand per SKU is `100` in `application.yml` (`5` in unit tests).
