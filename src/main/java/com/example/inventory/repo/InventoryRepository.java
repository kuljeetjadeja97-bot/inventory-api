package com.example.inventory.repo;

import com.example.inventory.domain.Reservation;
import com.example.inventory.domain.SkuStock;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface InventoryRepository {

    SkuStock stockFor(String sku);

    void save(Reservation reservation);

    Optional<Reservation> findReservation(UUID id);

    Optional<Reservation> findByIdempotencyKey(String key);

    Collection<Reservation> reservations();
}
