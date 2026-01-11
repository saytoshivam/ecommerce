package com.korber.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventoryUpdateResponse {
    private List<Long> reservedBatchIds;
    private String message;
}

