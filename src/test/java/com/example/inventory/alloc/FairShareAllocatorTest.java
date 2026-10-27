package com.example.inventory.alloc;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FairShareAllocatorTest {

    private final FairShareAllocator allocator = new FairShareAllocator();

    @Test
    void splitsScarceStockAcrossSmallerOrdersFirst() {
        Map<String, Integer> granted = allocator.allocate(10, List.of(
                new DemandLine("large", 20),
                new DemandLine("small", 3),
                new DemandLine("medium", 6)
        ));

        assertThat(granted.get("small")).isEqualTo(3);
        assertThat(granted.get("medium")).isEqualTo(3);
        assertThat(granted.get("large")).isEqualTo(4);
        assertThat(granted.values().stream().mapToInt(Integer::intValue).sum()).isEqualTo(10);
    }

    @Test
    void grantsNothingWhenEmpty() {
        assertThat(allocator.allocate(0, List.of(new DemandLine("a", 5)))).containsEntry("a", 0);
    }

    @Test
    void rejectsNegativeAvailability() {
        assertThatThrownBy(() -> allocator.allocate(-1, List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
