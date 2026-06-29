package com.orderbook.entity;

public enum OrderReqType {
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
    POST_ONLY, ICEBERG
}
