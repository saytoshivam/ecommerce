package com.korber.service;

import com.korber.dto.*;
import com.korber.model.Order;
import com.korber.model.OrderStatus;
import com.korber.repository.OrderRepository;
import com.korber.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private OrderServiceImpl orderService;

    private String inventoryServiceUrl = "http://localhost:8081";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(orderService, "inventoryServiceUrl", inventoryServiceUrl);
    }

    @Test
    void testPlaceOrder_Success() {
        // Given
        OrderRequest request = new OrderRequest();
        request.setProductId(1001L);
        request.setQuantity(10);

        InventoryBatchDto batchDto = new InventoryBatchDto(1L, 50, LocalDate.of(2026, 6, 25));
        InventoryResponse inventoryResponse = new InventoryResponse(
                1001L, "Laptop", Arrays.asList(batchDto)
        );

        InventoryUpdateRequest updateRequest = new InventoryUpdateRequest();
        updateRequest.setProductId(1001L);
        updateRequest.setQuantity(10);

        InventoryUpdateResponse updateResponse = new InventoryUpdateResponse(
                Arrays.asList(1L), "Inventory updated successfully"
        );

        Order savedOrder = new Order();
        savedOrder.setOrderId(1L);
        savedOrder.setProductId(1001L);
        savedOrder.setProductName("Laptop");
        savedOrder.setQuantity(10);
        savedOrder.setStatus(OrderStatus.PLACED);
        savedOrder.setOrderDate(LocalDate.now());

        when(restTemplate.getForEntity(
                eq(inventoryServiceUrl + "/inventory/1001"),
                eq(InventoryResponse.class)))
                .thenReturn(new ResponseEntity<>(inventoryResponse, HttpStatus.OK));

        when(restTemplate.postForEntity(
                eq(inventoryServiceUrl + "/inventory/update"),
                any(InventoryUpdateRequest.class),
                eq(InventoryUpdateResponse.class)))
                .thenReturn(new ResponseEntity<>(updateResponse, HttpStatus.OK));

        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        // When
        OrderResponse response = orderService.placeOrder(request);

        // Then
        assertNotNull(response);
        assertEquals(1L, response.getOrderId());
        assertEquals(1001L, response.getProductId());
        assertEquals("Laptop", response.getProductName());
        assertEquals(10, response.getQuantity());
        assertEquals("PLACED", response.getStatus());
        assertEquals(1, response.getReservedFromBatchIds().size());
        assertEquals(1L, response.getReservedFromBatchIds().get(0));
        assertEquals("Order placed. Inventory reserved.", response.getMessage());

        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    void testPlaceOrder_ProductNotFound() {
        // Given
        OrderRequest request = new OrderRequest();
        request.setProductId(9999L);
        request.setQuantity(10);

        when(restTemplate.getForEntity(
                eq(inventoryServiceUrl + "/inventory/9999"),
                eq(InventoryResponse.class)))
                .thenThrow(new RuntimeException("Connection refused"));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            orderService.placeOrder(request);
        });

        assertTrue(exception.getMessage().contains("Failed to fetch inventory"));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void testPlaceOrder_InsufficientInventory() {
        // Given
        OrderRequest request = new OrderRequest();
        request.setProductId(1001L);
        request.setQuantity(100); // More than available

        InventoryBatchDto batchDto = new InventoryBatchDto(1L, 50, LocalDate.of(2026, 6, 25));
        InventoryResponse inventoryResponse = new InventoryResponse(
                1001L, "Laptop", Arrays.asList(batchDto)
        );

        when(restTemplate.getForEntity(
                eq(inventoryServiceUrl + "/inventory/1001"),
                eq(InventoryResponse.class)))
                .thenReturn(new ResponseEntity<>(inventoryResponse, HttpStatus.OK));

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            orderService.placeOrder(request);
        });

        assertTrue(exception.getMessage().contains("Insufficient inventory"));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void testPlaceOrder_InventoryUpdateFails() {
        // Given
        OrderRequest request = new OrderRequest();
        request.setProductId(1001L);
        request.setQuantity(10);

        InventoryBatchDto batchDto = new InventoryBatchDto(1L, 50, LocalDate.of(2026, 6, 25));
        InventoryResponse inventoryResponse = new InventoryResponse(
                1001L, "Laptop", Arrays.asList(batchDto)
        );

        when(restTemplate.getForEntity(
                eq(inventoryServiceUrl + "/inventory/1001"),
                eq(InventoryResponse.class)))
                .thenReturn(new ResponseEntity<>(inventoryResponse, HttpStatus.OK));

        when(restTemplate.postForEntity(
                eq(inventoryServiceUrl + "/inventory/update"),
                any(InventoryUpdateRequest.class),
                eq(InventoryUpdateResponse.class)))
                .thenReturn(new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            orderService.placeOrder(request);
        });

        assertTrue(exception.getMessage().contains("Failed to update inventory"));
        verify(orderRepository, never()).save(any());
    }
}

