package com.example.inventory.api.dto;

public record AvailabilityResponse(String sku, int onHand, int reserved, int available) {
}
