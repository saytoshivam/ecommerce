package com.korber.handler.impl;

import com.korber.handler.InventoryHandler;
import com.korber.model.InventoryBatch;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class FIFOInventoryHandler implements InventoryHandler {
    @Override
    public Map<Long, Integer> selectBatches(List<InventoryBatch> batches, Integer quantity) {
        Map<Long, Integer> selectedBatches = new HashMap<>();
        int remainingQuantity = quantity;

        // FIFO: Use batches in order of expiry date (already sorted)
        for (InventoryBatch batch : batches) {
            if (remainingQuantity <= 0) {
                break;
            }

            int availableQuantity = batch.getQuantity();
            if (availableQuantity > 0) {
                int quantityToReserve = Math.min(remainingQuantity, availableQuantity);
                selectedBatches.put(batch.getBatchId(), quantityToReserve);
                remainingQuantity -= quantityToReserve;
            }
        }

        if (remainingQuantity > 0) {
            throw new IllegalArgumentException("Insufficient inventory. Required: " + quantity + ", Available: " + (quantity - remainingQuantity));
        }

        return selectedBatches;
    }
}

