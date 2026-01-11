package com.korber.service.impl;

import com.korber.dto.*;
import com.korber.model.Order;
import com.korber.model.OrderStatus;
import com.korber.repository.OrderRepository;
import com.korber.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
    private final RestTemplate restTemplate;

    @Value("${inventory.service.url}")
    private String inventoryServiceUrl;

    @Transactional
    public OrderResponse placeOrder(OrderRequest request) {
        // Check inventory availability
        InventoryResponse inventoryResponse = getInventory(request.getProductId());
        
        if (inventoryResponse == null) {
            throw new IllegalArgumentException("Product not found: " + request.getProductId());
        }

        // Calculate total available quantity
        int totalAvailable = inventoryResponse.getBatches().stream()
                .mapToInt(InventoryBatchDto::getQuantity)
                .sum();

        if (totalAvailable < request.getQuantity()) {
            throw new IllegalArgumentException("Insufficient inventory. Required: " + request.getQuantity() + ", Available: " + totalAvailable);
        }

        // Update inventory (Inventory Service will handle batch selection)
        InventoryUpdateRequest updateRequest = new InventoryUpdateRequest();
        updateRequest.setProductId(request.getProductId());
        updateRequest.setQuantity(request.getQuantity());

        ResponseEntity<InventoryUpdateResponse> updateResponse = restTemplate.postForEntity(
                inventoryServiceUrl + "/inventory/update",
                updateRequest,
                InventoryUpdateResponse.class
        );

        if (!updateResponse.getStatusCode().is2xxSuccessful() || updateResponse.getBody() == null) {
            throw new RuntimeException("Failed to update inventory: " + updateResponse.getStatusCode());
        }

        List<Long> reservedBatchIds = updateResponse.getBody().getReservedBatchIds();

        // Create order
        Order order = new Order();
        order.setOrderId(null); // Ensure ID is null for new entity
        order.setProductId(request.getProductId());
        order.setProductName(inventoryResponse.getProductName());
        order.setQuantity(request.getQuantity());
        order.setStatus(OrderStatus.PLACED);
        order.setOrderDate(LocalDate.now());

        Order savedOrder = orderRepository.save(order);

        // Build response
        OrderResponse response = new OrderResponse();
        response.setOrderId(savedOrder.getOrderId());
        response.setProductId(savedOrder.getProductId());
        response.setProductName(savedOrder.getProductName());
        response.setQuantity(savedOrder.getQuantity());
        response.setStatus(savedOrder.getStatus().name());
        response.setReservedFromBatchIds(reservedBatchIds);
        response.setMessage("Order placed. Inventory reserved.");

        return response;
    }

    private InventoryResponse getInventory(Long productId) {
        try {
            ResponseEntity<InventoryResponse> response = restTemplate.getForEntity(
                    inventoryServiceUrl + "/inventory/" + productId,
                    InventoryResponse.class
            );
            return response.getBody();
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch inventory: " + e.getMessage(), e);
        }
    }

}

