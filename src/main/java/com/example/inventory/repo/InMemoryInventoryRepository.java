package com.example.inventory.repo;

import com.example.inventory.domain.Reservation;
import com.example.inventory.domain.SkuStock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryInventoryRepository implements InventoryRepository {

    private final int defaultOnHand;
    private final Map<String, SkuStock> stock = new ConcurrentHashMap<>();
    private final Map<UUID, Reservation> reservations = new ConcurrentHashMap<>();
    private final Map<String, UUID> idempotency = new ConcurrentHashMap<>();

    public InMemoryInventoryRepository(
            @Value("${inventory.default-on-hand}") int defaultOnHand) {
        this.defaultOnHand = defaultOnHand;
    }

    @Override
    public SkuStock stockFor(String sku) {
        return stock.computeIfAbsent(sku, key -> new SkuStock(key, defaultOnHand));
    }

    @Override
    public void save(Reservation reservation) {
        reservations.put(reservation.id(), reservation);
        if (reservation.idempotencyKey() != null) {
            idempotency.put(reservation.idempotencyKey(), reservation.id());
        }
    }

    @Override
    public Optional<Reservation> findReservation(UUID id) {
        return Optional.ofNullable(reservations.get(id));
    }

    @Override
    public Optional<Reservation> findByIdempotencyKey(String key) {
        UUID id = idempotency.get(key);
        return id == null ? Optional.empty() : findReservation(id);
    }

    @Override
    public Collection<Reservation> reservations() {
        return reservations.values();
    }
}
