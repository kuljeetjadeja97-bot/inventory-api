package com.example.inventory.alloc;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Water-filling allocator: when a SKU is oversold at reservation time, remaining
 * units are spread so no single customer starves smaller orders.
 *
 * Complexity: O(n log n) from the sort, then a single O(n) fill pass.
 */
public final class FairShareAllocator {

    public Map<String, Integer> allocate(int available, List<DemandLine> demand) {
        if (available < 0) {
            throw new IllegalArgumentException("available must be >= 0");
        }
        List<DemandLine> ordered = new ArrayList<>(demand);
        ordered.sort(Comparator.comparingInt(DemandLine::requested));

        Map<String, Integer> result = new HashMap<>();
        int remainingPeople = ordered.size();
        int remainingUnits = available;

        for (int i = 0; i < ordered.size(); i++) {
            DemandLine line = ordered.get(i);
            if (remainingPeople == 0) {
                result.put(line.customerId(), 0);
                continue;
            }
            int fair = remainingUnits / remainingPeople;
            int granted = Math.min(line.requested(), fair);
            result.merge(line.customerId(), granted, Integer::sum);
            remainingUnits -= granted;
            remainingPeople--;
        }
        return result;
    }
}
