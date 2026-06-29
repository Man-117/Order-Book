package com.orderbook.entity;

/**
 * Represents the side of an order in the order book.
 */
public enum Side {
    BUY,
    SELL;

    public Side opposite() {
        return this == BUY ? SELL : BUY;
    }
}
