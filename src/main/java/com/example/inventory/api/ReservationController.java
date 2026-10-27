package com.example.inventory.api;

import com.example.inventory.api.dto.AvailabilityResponse;
import com.example.inventory.api.dto.ReservationRequest;
import com.example.inventory.api.dto.ReservationResponse;
import com.example.inventory.domain.SkuStock;
import com.example.inventory.service.ReservationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/v1")
public class ReservationController {

    private final ReservationService service;

    public ReservationController(ReservationService service) {
        this.service = service;
    }

    @PostMapping("/reservations")
    @ResponseStatus(HttpStatus.CREATED)
    public ReservationResponse create(
            @Valid @RequestBody ReservationRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        return ReservationResponse.from(service.create(request, idempotencyKey));
    }

    @GetMapping("/reservations/{id}")
    public ReservationResponse get(@PathVariable UUID id) {
        return ReservationResponse.from(service.get(id));
    }

    @PostMapping("/reservations/{id}/commit")
    public ReservationResponse commit(@PathVariable UUID id) {
        return ReservationResponse.from(service.commit(id));
    }

    @PostMapping("/reservations/{id}/release")
    public ReservationResponse release(@PathVariable UUID id) {
        return ReservationResponse.from(service.release(id));
    }

    @GetMapping("/skus/{sku}/availability")
    public AvailabilityResponse availability(@PathVariable String sku) {
        SkuStock stock = service.availability(sku);
        return new AvailabilityResponse(stock.sku(), stock.onHand(), stock.reserved(), stock.available());
    }
}
