package com.orderbook.entity;

import com.orderbook.utils.CommonUtil;
import com.orderbook.utils.IDGenerator;

import java.util.UUID;

/**
 * Represents a request to submit an order to the OrderBook.
 */
public record GeneralOrderInfo(UUID id, OrderType type, Side side, long quantity, UUID userId, long timestamp) {
    public GeneralOrderInfo(OrderType type, Side side, long quantity, UUID userId) {
        this(IDGenerator.get(), type, side, quantity, userId, CommonUtil.now());
    }
}


