package com.orderbook.entity;

import com.orderbook.utils.IDGenerator;

import java.util.UUID;

public record IcebergOrder(UUID id, LimitOrder childOrder, long totalQuantity) {
    public IcebergOrder(LimitOrder childOrder, long totalQuantity) {
        this(IDGenerator.get(), childOrder, totalQuantity);
    }
}