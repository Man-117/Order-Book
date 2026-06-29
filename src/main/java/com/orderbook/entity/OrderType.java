package com.orderbook.entity;

/**
 * Defines the type of order.
 */
public enum OrderType {
    /**
     * Standard limit order.
     */
    LIMIT,

    /**
     * Market order.
     */
    MARKET,

    /**
     * Post-Only order that is rejected if it would match immediately.
     */
    POST_ONLY
}
