package com.korber.service;

import com.korber.dto.OrderRequest;
import com.korber.dto.OrderResponse;

public interface OrderService {
     OrderResponse placeOrder(OrderRequest request);
}
