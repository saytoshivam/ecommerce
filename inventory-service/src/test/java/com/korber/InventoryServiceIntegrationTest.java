package com.korber;

import com.korber.dto.InventoryBatchDto;
import com.korber.dto.InventoryResponse;
import com.korber.dto.InventoryUpdateRequest;
import com.korber.dto.InventoryUpdateResponse;
import com.korber.model.InventoryBatch;
import com.korber.repository.InventoryBatchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class InventoryServiceIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private InventoryBatchRepository repository;

    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/inventory";
    }

    @Test
    @Transactional
    void testGetInventory_Integration() {
        // Given - Data is loaded by Liquibase
        Long productId = 1001L;

        // When
        ResponseEntity<InventoryResponse> response = restTemplate.getForEntity(
                baseUrl + "/" + productId, InventoryResponse.class);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(productId, response.getBody().getProductId());
        assertNotNull(response.getBody().getBatches());
        assertFalse(response.getBody().getBatches().isEmpty());
    }

    @Test
    void testUpdateInventory_Integration() {
        // Given
        InventoryUpdateRequest request = new InventoryUpdateRequest();
        request.setProductId(1001L);
        request.setQuantity(10);

        // Get initial quantity before update via REST call
        ResponseEntity<InventoryResponse> beforeResponse = restTemplate.getForEntity(
                baseUrl + "/1001", InventoryResponse.class);
        assertEquals(HttpStatus.OK, beforeResponse.getStatusCode());
        int initialQuantity = beforeResponse.getBody().getBatches().get(0).getQuantity();

        // When
        ResponseEntity<InventoryUpdateResponse> response = restTemplate.postForEntity(
                baseUrl + "/update", request, InventoryUpdateResponse.class);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getReservedBatchIds());
        assertFalse(response.getBody().getReservedBatchIds().isEmpty());

        // Verify quantity was updated by making another REST call
        // This ensures we see the committed changes from the update transaction
        ResponseEntity<InventoryResponse> afterResponse = restTemplate.getForEntity(
                baseUrl + "/1001", InventoryResponse.class);
        assertEquals(HttpStatus.OK, afterResponse.getStatusCode());
        
        // Find the updated batch in the response
        Long updatedBatchId = response.getBody().getReservedBatchIds().get(0);
        InventoryBatchDto updatedBatchDto = afterResponse.getBody().getBatches().stream()
                .filter(b -> b.getBatchId().equals(updatedBatchId))
                .findFirst()
                .orElse(null);
        
        assertNotNull(updatedBatchDto, "Updated batch should be found in inventory response");
        assertEquals(initialQuantity - 10, updatedBatchDto.getQuantity(), 
                "Quantity should be reduced by the requested amount");
    }

    @Test
    @Transactional
    void testGetInventory_ProductNotFound() {
        // Given
        Long productId = 99999L;

        // When
        ResponseEntity<InventoryResponse> response = restTemplate.getForEntity(
                baseUrl + "/" + productId, InventoryResponse.class);

        // Then
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    @Transactional
    void testUpdateInventory_InsufficientQuantity() {
        // Given
        InventoryUpdateRequest request = new InventoryUpdateRequest();
        request.setProductId(1001L);
        request.setQuantity(10000); // Very large quantity

        // When
        ResponseEntity<InventoryUpdateResponse> response = restTemplate.postForEntity(
                baseUrl + "/update", request, InventoryUpdateResponse.class);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }
}

