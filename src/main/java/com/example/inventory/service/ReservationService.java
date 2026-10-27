package com.example.inventory.service;

import com.example.inventory.api.dto.ReservationRequest;
import com.example.inventory.domain.Reservation;
import com.example.inventory.domain.ReservationStatus;
import com.example.inventory.domain.SkuStock;
import com.example.inventory.repo.InventoryRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class ReservationService {

    private final InventoryRepository repository;
    private final Duration ttl;

    public ReservationService(
            InventoryRepository repository,
            @Value("${inventory.reservation-ttl}") Duration ttl) {
        this.repository = repository;
        this.ttl = ttl;
    }

    public Reservation create(ReservationRequest request, String idempotencyKey) {
        validateLines(request.lines());
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            var existing = repository.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                return existing.get();
            }
        }

        Map<String, SkuStock> locked = new LinkedHashMap<>();
        request.lines().keySet().stream().sorted().forEach(sku -> locked.put(sku, repository.stockFor(sku)));

        Map<String, Integer> reserved = new LinkedHashMap<>();
        try {
            for (var entry : request.lines().entrySet()) {
                SkuStock stock = locked.get(entry.getKey());
                if (!stock.tryReserve(entry.getValue())) {
                    throw new UnprocessableException("Insufficient stock for SKU " + entry.getKey());
                }
                reserved.put(entry.getKey(), entry.getValue());
            }
        } catch (RuntimeException ex) {
            reserved.forEach((sku, units) -> locked.get(sku).release(units));
            throw ex;
        }

        Instant now = Instant.now();
        Reservation reservation = new Reservation(
                UUID.randomUUID(),
                request.customerId(),
                Map.copyOf(request.lines()),
                ReservationStatus.HELD,
                now,
                now.plus(ttl),
                idempotencyKey
        );
        repository.save(reservation);
        return reservation;
    }

    public Reservation commit(UUID id) {
        Reservation reservation = requireHeld(id);
        reservation.lines().forEach((sku, units) -> {
            if (!repository.stockFor(sku).tryCommit(units)) {
                throw new ConflictException("Stock changed underneath reservation " + id);
            }
        });
        Reservation committed = reservation.withStatus(ReservationStatus.COMMITTED);
        repository.save(committed);
        return committed;
    }

    public Reservation release(UUID id) {
        Reservation reservation = requireHeld(id);
        reservation.lines().forEach((sku, units) -> repository.stockFor(sku).release(units));
        Reservation released = reservation.withStatus(ReservationStatus.RELEASED);
        repository.save(released);
        return released;
    }

    public Reservation get(UUID id) {
        return repository.findReservation(id)
                .orElseThrow(() -> new NotFoundException("Reservation " + id + " not found"));
    }

    public SkuStock availability(String sku) {
        return repository.stockFor(sku);
    }

    @Scheduled(fixedDelayString = "PT30S")
    public void expireHolds() {
        Instant now = Instant.now();
        repository.reservations().stream()
                .filter(reservation -> reservation.status() == ReservationStatus.HELD)
                .filter(reservation -> reservation.expiresAt().isBefore(now))
                .forEach(reservation -> {
                    reservation.lines().forEach((sku, units) -> repository.stockFor(sku).release(units));
                    repository.save(reservation.withStatus(ReservationStatus.EXPIRED));
                });
    }

    private Reservation requireHeld(UUID id) {
        Reservation reservation = get(id);
        if (reservation.status() != ReservationStatus.HELD) {
            throw new ConflictException("Reservation " + id + " is " + reservation.status());
        }
        if (reservation.expiresAt().isBefore(Instant.now())) {
            throw new ConflictException("Reservation " + id + " expired");
        }
        return reservation;
    }

    private static void validateLines(Map<String, Integer> lines) {
        lines.forEach((sku, units) -> {
            if (units == null || units <= 0) {
                throw new UnprocessableException("Quantity for " + sku + " must be positive");
            }
        });
    }
}
