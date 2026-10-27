package com.example.inventory.domain;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record Reservation(
        UUID id,
        String customerId,
        Map<String, Integer> lines,
        ReservationStatus status,
        Instant createdAt,
        Instant expiresAt,
        String idempotencyKey
) {
    public Reservation withStatus(ReservationStatus next) {
        return new Reservation(id, customerId, lines, next, createdAt, expiresAt, idempotencyKey);
    }
}
