package com.example.inventory.service;

import com.example.inventory.api.dto.ReservationRequest;
import com.example.inventory.domain.ReservationStatus;
import com.example.inventory.repo.InMemoryInventoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReservationServiceTest {

    private ReservationService service;

    @BeforeEach
    void setUp() {
        service = new ReservationService(new InMemoryInventoryRepository(5), Duration.ofMinutes(15));
    }

    @Test
    void reservesThenCommitsReducingOnHand() {
        var held = service.create(new ReservationRequest("cust-1", Map.of("SKU-1", 2)), "key-1");
        assertThat(held.status()).isEqualTo(ReservationStatus.HELD);
        assertThat(service.availability("SKU-1").available()).isEqualTo(3);

        var committed = service.commit(held.id());
        assertThat(committed.status()).isEqualTo(ReservationStatus.COMMITTED);
        assertThat(service.availability("SKU-1").onHand()).isEqualTo(3);
        assertThat(service.availability("SKU-1").reserved()).isEqualTo(0);
    }

    @Test
    void replaysIdempotentCreate() {
        var first = service.create(new ReservationRequest("cust-1", Map.of("SKU-1", 1)), "same-key");
        var second = service.create(new ReservationRequest("cust-1", Map.of("SKU-1", 1)), "same-key");
        assertThat(second.id()).isEqualTo(first.id());
        assertThat(service.availability("SKU-1").reserved()).isEqualTo(1);
    }

    @Test
    void rejectsOversell() {
        service.create(new ReservationRequest("cust-1", Map.of("SKU-1", 5)), null);
        assertThatThrownBy(() -> service.create(new ReservationRequest("cust-2", Map.of("SKU-1", 1)), null))
                .isInstanceOf(UnprocessableException.class);
    }

    @Test
    void rollsBackPartialMultiSkuFailure() {
        service.create(new ReservationRequest("cust-1", Map.of("SKU-A", 5)), null);
        assertThatThrownBy(() ->
                service.create(new ReservationRequest("cust-2", Map.of("SKU-B", 1, "SKU-A", 1)), null))
                .isInstanceOf(UnprocessableException.class);
        assertThat(service.availability("SKU-B").reserved()).isEqualTo(0);
    }

    @Test
    void concurrentReservesNeverExceedOnHand() throws Exception {
        int threads = 20;
        var pool = Executors.newFixedThreadPool(threads);
        var start = new CountDownLatch(1);
        var done = new CountDownLatch(threads);
        var successes = new AtomicInteger();

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    start.await();
                    service.create(new ReservationRequest("c", Map.of("HOT", 1)), UUID.randomUUID().toString());
                    successes.incrementAndGet();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (UnprocessableException ignored) {
                    // expected when stock is gone
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown();
        done.await();
        pool.shutdownNow();

        assertThat(successes.get()).isEqualTo(5);
        assertThat(service.availability("HOT").reserved()).isEqualTo(5);
    }
}
