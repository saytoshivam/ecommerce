package com.korber.handler;

import com.korber.model.InventoryBatch;

import java.util.List;
import java.util.Map;

public interface InventoryHandler {
    /**
     * Selects batches to fulfill an order based on the handler's strategy
     * @param batches Available batches sorted by expiry date
     * @param quantity Required quantity
     * @return Map of batchId to quantity to reserve
     */
    Map<Long, Integer> selectBatches(List<InventoryBatch> batches, Integer quantity);
}

