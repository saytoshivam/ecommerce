package com.korber.service;

import com.korber.dto.InventoryResponse;
import com.korber.dto.InventoryUpdateRequest;
import com.korber.dto.InventoryUpdateResponse;

public interface InventoryService {
    public InventoryResponse getInventoryByProductId(Long productId);
    public InventoryUpdateResponse updateInventory(InventoryUpdateRequest request);
}
