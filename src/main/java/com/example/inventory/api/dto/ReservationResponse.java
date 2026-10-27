package com.example.inventory.api.dto;

import com.example.inventory.domain.Reservation;
import com.example.inventory.domain.ReservationStatus;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record ReservationResponse(
        UUID id,
        String customerId,
        Map<String, Integer> lines,
        ReservationStatus status,
        Instant createdAt,
        Instant expiresAt
) {
    public static ReservationResponse from(Reservation reservation) {
        return new ReservationResponse(
                reservation.id(),
                reservation.customerId(),
                reservation.lines(),
                reservation.status(),
                reservation.createdAt(),
                reservation.expiresAt()
        );
    }
}
