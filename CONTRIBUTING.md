# Contributing

## Review checklist

- Does the change preserve reservation invariants? On-hand must never go negative, and reserved units must be released on expire, cancel, or partial failure.
- Are new endpoints idempotent where a client retry is expected?
- Did tests cover the failure path, not only the happy path? Concurrent oversell is the regression that matters here.
- Any new shared mutable state needs an explicit lock ordering (SKU code ascending) or a single-SKU lock.
- Avoid N+1 lookups in request handlers. Batch stock reads by SKU.
- Log in terms of reservation id and SKU, never raw customer payloads.

## Performance notes

Hold TTL expiry is a scheduled sweep. If reservation volume grows, replace the full scan in `expireHolds` with a min-heap keyed by `expiresAt` so expiry is O(log n) instead of O(n).
