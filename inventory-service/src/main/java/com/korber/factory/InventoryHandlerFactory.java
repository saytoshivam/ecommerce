package com.korber.factory;

import com.korber.handler.InventoryHandler;
import com.korber.handler.impl.FIFOInventoryHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class InventoryHandlerFactory {
    private final FIFOInventoryHandler fifoInventoryHandler;
    private final Map<String, InventoryHandler> handlers = new HashMap<>();

    @PostConstruct
    public void init() {
        // Register default handler (FIFO - First In First Out based on expiry date)
        registerHandler("FIFO", fifoInventoryHandler);
    }

    public void registerHandler(String type, InventoryHandler handler) {
        handlers.put(type, handler);
    }

    public InventoryHandler getHandler(String type) {
        InventoryHandler handler = handlers.get(type);
        if (handler == null) {
            // Default to FIFO if handler not found
            handler = handlers.get("FIFO");
        }
        return handler;
    }
}

