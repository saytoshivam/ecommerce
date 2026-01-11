package com.korber.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventoryResponse {
    private Long productId;
    private String productName;
    private List<InventoryBatchDto> batches;
}

