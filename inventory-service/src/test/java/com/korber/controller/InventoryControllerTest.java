package com.korber.controller;

import com.korber.dto.InventoryBatchDto;
import com.korber.dto.InventoryResponse;
import com.korber.dto.InventoryUpdateRequest;
import com.korber.dto.InventoryUpdateResponse;
import com.korber.service.InventoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InventoryController.class)
class InventoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private InventoryService inventoryService;

    @Test
    void testGetInventory_Success() throws Exception {
        // Given
        Long productId = 1001L;
        InventoryBatchDto batchDto = new InventoryBatchDto(1L, 50, LocalDate.of(2026, 6, 25));
        InventoryResponse response = new InventoryResponse(productId, "Laptop", Arrays.asList(batchDto));

        when(inventoryService.getInventoryByProductId(productId)).thenReturn(response);

        // When & Then
        mockMvc.perform(get("/inventory/{productId}", productId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.productId").value(productId))
                .andExpect(jsonPath("$.productName").value("Laptop"))
                .andExpect(jsonPath("$.batches[0].batchId").value(1L))
                .andExpect(jsonPath("$.batches[0].quantity").value(50));

        verify(inventoryService, times(1)).getInventoryByProductId(productId);
    }

    @Test
    void testGetInventory_ProductNotFound() throws Exception {
        // Given
        Long productId = 9999L;
        when(inventoryService.getInventoryByProductId(productId))
                .thenThrow(new IllegalArgumentException("Product not found: " + productId));

        // When & Then
        mockMvc.perform(get("/inventory/{productId}", productId))
                .andExpect(status().isNotFound());

        verify(inventoryService, times(1)).getInventoryByProductId(productId);
    }

    @Test
    void testUpdateInventory_Success() throws Exception {
        // Given
        InventoryUpdateRequest request = new InventoryUpdateRequest();
        request.setProductId(1001L);
        request.setQuantity(20);

        InventoryUpdateResponse response = new InventoryUpdateResponse(
                Arrays.asList(1L), "Inventory updated successfully"
        );

        when(inventoryService.updateInventory(any(InventoryUpdateRequest.class))).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/inventory/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":1001,\"quantity\":20}"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.reservedBatchIds[0]").value(1L))
                .andExpect(jsonPath("$.message").value("Inventory updated successfully"));

        verify(inventoryService, times(1)).updateInventory(any(InventoryUpdateRequest.class));
    }

    @Test
    void testUpdateInventory_BadRequest() throws Exception {
        // Given
        when(inventoryService.updateInventory(any(InventoryUpdateRequest.class)))
                .thenThrow(new IllegalArgumentException("Product not found: 9999"));

        // When & Then
        mockMvc.perform(post("/inventory/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":9999,\"quantity\":10}"))
                .andExpect(status().isBadRequest());

        verify(inventoryService, times(1)).updateInventory(any(InventoryUpdateRequest.class));
    }
}

