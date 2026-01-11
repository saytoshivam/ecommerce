package com.korber.service.impl;

import com.korber.dto.InventoryBatchDto;
import com.korber.dto.InventoryResponse;
import com.korber.dto.InventoryUpdateRequest;
import com.korber.dto.InventoryUpdateResponse;
import com.korber.factory.InventoryHandlerFactory;
import com.korber.handler.InventoryHandler;
import com.korber.model.InventoryBatch;
import com.korber.repository.InventoryBatchRepository;
import com.korber.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {
    private final InventoryBatchRepository repository;
    private final InventoryHandlerFactory handlerFactory;

    public InventoryResponse getInventoryByProductId(Long productId) {
        List<InventoryBatch> batches = repository.findByProductIdOrderByExpiryDateAsc(productId);
        
        if (batches.isEmpty()) {
            throw new IllegalArgumentException("Product not found: " + productId);
        }

        String productName = batches.get(0).getProductName();
        List<InventoryBatchDto> batchDtos = batches.stream()
                .map(batch -> new InventoryBatchDto(
                        batch.getBatchId(),
                        batch.getQuantity(),
                        batch.getExpiryDate()
                ))
                .collect(Collectors.toList());

        return new InventoryResponse(productId, productName, batchDtos);
    }

    @Transactional
    public InventoryUpdateResponse updateInventory(InventoryUpdateRequest request) {
        List<InventoryBatch> batches = repository.findByProductIdOrderByExpiryDateAsc(request.getProductId());
        
        if (batches.isEmpty()) {
            throw new IllegalArgumentException("Product not found: " + request.getProductId());
        }

        // Use Factory Pattern to get the appropriate handler
        InventoryHandler handler = handlerFactory.getHandler("FIFO");
        Map<Long, Integer> batchReservations = handler.selectBatches(batches, request.getQuantity());

        List<Long> reservedBatchIds = new ArrayList<>();

        // Update inventory quantities
        for (Map.Entry<Long, Integer> entry : batchReservations.entrySet()) {
            Long batchId = entry.getKey();
            Integer quantityToDeduct = entry.getValue();
            
            InventoryBatch batch = repository.findById(batchId)
                    .orElseThrow(() -> new IllegalArgumentException("Batch not found: " + batchId));
            
            if (batch.getQuantity() < quantityToDeduct) {
                throw new IllegalArgumentException("Insufficient quantity in batch: " + batchId);
            }
            
            batch.setQuantity(batch.getQuantity() - quantityToDeduct);
            repository.save(batch);
            reservedBatchIds.add(batchId);
        }

        return new InventoryUpdateResponse(reservedBatchIds, "Inventory updated successfully");
    }
}

