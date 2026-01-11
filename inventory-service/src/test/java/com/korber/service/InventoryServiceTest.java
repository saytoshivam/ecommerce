package com.korber.service;

import com.korber.dto.InventoryBatchDto;
import com.korber.dto.InventoryResponse;
import com.korber.dto.InventoryUpdateRequest;
import com.korber.dto.InventoryUpdateResponse;
import com.korber.factory.InventoryHandlerFactory;
import com.korber.handler.InventoryHandler;
import com.korber.model.InventoryBatch;
import com.korber.repository.InventoryBatchRepository;
import com.korber.service.impl.InventoryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryBatchRepository repository;

    @Mock
    private InventoryHandlerFactory handlerFactory;

    @Mock
    private InventoryHandler inventoryHandler;

    @InjectMocks
    private InventoryServiceImpl inventoryService;

    private InventoryBatch batch1;
    private InventoryBatch batch2;
    private List<InventoryBatch> batches;

    @BeforeEach
    void setUp() {
        batch1 = new InventoryBatch();
        batch1.setBatchId(1L);
        batch1.setProductId(1001L);
        batch1.setProductName("Laptop");
        batch1.setQuantity(50);
        batch1.setExpiryDate(LocalDate.of(2026, 6, 25));

        batch2 = new InventoryBatch();
        batch2.setBatchId(2L);
        batch2.setProductId(1001L);
        batch2.setProductName("Laptop");
        batch2.setQuantity(30);
        batch2.setExpiryDate(LocalDate.of(2026, 7, 15));

        batches = Arrays.asList(batch1, batch2);
    }

    @Test
    void testGetInventoryByProductId_Success() {
        // Given
        Long productId = 1001L;
        when(repository.findByProductIdOrderByExpiryDateAsc(productId)).thenReturn(batches);

        // When
        InventoryResponse response = inventoryService.getInventoryByProductId(productId);

        // Then
        assertNotNull(response);
        assertEquals(productId, response.getProductId());
        assertEquals("Laptop", response.getProductName());
        assertEquals(2, response.getBatches().size());
        assertEquals(1L, response.getBatches().get(0).getBatchId());
        assertEquals(2L, response.getBatches().get(1).getBatchId());
        verify(repository, times(1)).findByProductIdOrderByExpiryDateAsc(productId);
    }

    @Test
    void testGetInventoryByProductId_ProductNotFound() {
        // Given
        Long productId = 9999L;
        when(repository.findByProductIdOrderByExpiryDateAsc(productId)).thenReturn(Collections.emptyList());

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            inventoryService.getInventoryByProductId(productId);
        });

        assertEquals("Product not found: 9999", exception.getMessage());
        verify(repository, times(1)).findByProductIdOrderByExpiryDateAsc(productId);
    }

    @Test
    void testUpdateInventory_Success() {
        // Given
        InventoryUpdateRequest request = new InventoryUpdateRequest();
        request.setProductId(1001L);
        request.setQuantity(20);

        Map<Long, Integer> batchReservations = new HashMap<>();
        batchReservations.put(1L, 20);

        when(repository.findByProductIdOrderByExpiryDateAsc(1001L)).thenReturn(batches);
        when(handlerFactory.getHandler("FIFO")).thenReturn(inventoryHandler);
        when(inventoryHandler.selectBatches(batches, 20)).thenReturn(batchReservations);
        when(repository.findById(1L)).thenReturn(Optional.of(batch1));
        when(repository.save(any(InventoryBatch.class))).thenReturn(batch1);

        // When
        InventoryUpdateResponse response = inventoryService.updateInventory(request);

        // Then
        assertNotNull(response);
        assertEquals(1, response.getReservedBatchIds().size());
        assertEquals(1L, response.getReservedBatchIds().get(0));
        assertEquals("Inventory updated successfully", response.getMessage());
        assertEquals(30, batch1.getQuantity()); // 50 - 20 = 30
        verify(repository, times(1)).save(batch1);
    }

    @Test
    void testUpdateInventory_ProductNotFound() {
        // Given
        InventoryUpdateRequest request = new InventoryUpdateRequest();
        request.setProductId(9999L);
        request.setQuantity(10);

        when(repository.findByProductIdOrderByExpiryDateAsc(9999L)).thenReturn(Collections.emptyList());

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            inventoryService.updateInventory(request);
        });

        assertEquals("Product not found: 9999", exception.getMessage());
        verify(repository, never()).save(any());
    }

    @Test
    void testUpdateInventory_InsufficientQuantity() {
        // Given
        InventoryUpdateRequest request = new InventoryUpdateRequest();
        request.setProductId(1001L);
        request.setQuantity(100); // More than available (50 + 30 = 80)

        Map<Long, Integer> batchReservations = new HashMap<>();
        batchReservations.put(1L, 100); // Trying to reserve more than available in batch

        when(repository.findByProductIdOrderByExpiryDateAsc(1001L)).thenReturn(batches);
        when(handlerFactory.getHandler("FIFO")).thenReturn(inventoryHandler);
        when(inventoryHandler.selectBatches(batches, 100)).thenReturn(batchReservations);
        when(repository.findById(1L)).thenReturn(Optional.of(batch1));

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            inventoryService.updateInventory(request);
        });

        assertEquals("Insufficient quantity in batch: 1", exception.getMessage());
    }

    @Test
    void testUpdateInventory_MultipleBatches() {
        // Given
        InventoryUpdateRequest request = new InventoryUpdateRequest();
        request.setProductId(1001L);
        request.setQuantity(60); // Requires both batches

        Map<Long, Integer> batchReservations = new HashMap<>();
        batchReservations.put(1L, 50); // Full batch1
        batchReservations.put(2L, 10); // Partial batch2

        when(repository.findByProductIdOrderByExpiryDateAsc(1001L)).thenReturn(batches);
        when(handlerFactory.getHandler("FIFO")).thenReturn(inventoryHandler);
        when(inventoryHandler.selectBatches(batches, 60)).thenReturn(batchReservations);
        when(repository.findById(1L)).thenReturn(Optional.of(batch1));
        when(repository.findById(2L)).thenReturn(Optional.of(batch2));
        when(repository.save(any(InventoryBatch.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        InventoryUpdateResponse response = inventoryService.updateInventory(request);

        // Then
        assertNotNull(response);
        assertEquals(2, response.getReservedBatchIds().size());
        assertTrue(response.getReservedBatchIds().contains(1L));
        assertTrue(response.getReservedBatchIds().contains(2L));
        assertEquals(0, batch1.getQuantity()); // 50 - 50 = 0
        assertEquals(20, batch2.getQuantity()); // 30 - 10 = 20
        verify(repository, times(2)).save(any(InventoryBatch.class));
    }
}

