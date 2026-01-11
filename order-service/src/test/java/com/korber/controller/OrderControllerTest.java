package com.korber.controller;

import com.korber.dto.OrderRequest;
import com.korber.dto.OrderResponse;
import com.korber.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    @Test
    void testPlaceOrder_Success() throws Exception {
        // Given
        OrderRequest request = new OrderRequest();
        request.setProductId(1001L);
        request.setQuantity(10);

        OrderResponse response = new OrderResponse();
        response.setOrderId(1L);
        response.setProductId(1001L);
        response.setProductName("Laptop");
        response.setQuantity(10);
        response.setStatus("PLACED");
        response.setReservedFromBatchIds(Arrays.asList(1L));
        response.setMessage("Order placed. Inventory reserved.");

        when(orderService.placeOrder(any(OrderRequest.class))).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":1001,\"quantity\":10}"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.orderId").value(1L))
                .andExpect(jsonPath("$.productId").value(1001L))
                .andExpect(jsonPath("$.productName").value("Laptop"))
                .andExpect(jsonPath("$.quantity").value(10))
                .andExpect(jsonPath("$.status").value("PLACED"))
                .andExpect(jsonPath("$.reservedFromBatchIds[0]").value(1L))
                .andExpect(jsonPath("$.message").value("Order placed. Inventory reserved."));

        verify(orderService, times(1)).placeOrder(any(OrderRequest.class));
    }

    @Test
    void testPlaceOrder_BadRequest() throws Exception {
        // Given
        when(orderService.placeOrder(any(OrderRequest.class)))
                .thenThrow(new IllegalArgumentException("Product not found: 9999"));

        // When & Then
        mockMvc.perform(post("/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":9999,\"quantity\":10}"))
                .andExpect(status().isBadRequest());

        verify(orderService, times(1)).placeOrder(any(OrderRequest.class));
    }

    @Test
    void testPlaceOrder_InternalServerError() throws Exception {
        // Given
        when(orderService.placeOrder(any(OrderRequest.class)))
                .thenThrow(new RuntimeException("Failed to update inventory"));

        // When & Then
        mockMvc.perform(post("/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":1001,\"quantity\":10}"))
                .andExpect(status().isInternalServerError());

        verify(orderService, times(1)).placeOrder(any(OrderRequest.class));
    }
}

