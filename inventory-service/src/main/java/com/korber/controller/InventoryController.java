package com.korber.controller;

import com.korber.dto.InventoryResponse;
import com.korber.dto.InventoryUpdateRequest;
import com.korber.dto.InventoryUpdateResponse;
import com.korber.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/inventory")
@RequiredArgsConstructor
public class InventoryController {
    private final InventoryService inventoryService;

    @GetMapping("/{productId}")
    public ResponseEntity<InventoryResponse> getInventory(@PathVariable Long productId) {
        try {
            InventoryResponse response = inventoryService.getInventoryByProductId(productId);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/update")
    public ResponseEntity<InventoryUpdateResponse> updateInventory(@RequestBody InventoryUpdateRequest request) {
        try {
            InventoryUpdateResponse response = inventoryService.updateInventory(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}

