package com.example.inventory.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.Map;

public record ReservationRequest(
        @NotBlank String customerId,
        @NotEmpty Map<String, Integer> lines
) {
}
