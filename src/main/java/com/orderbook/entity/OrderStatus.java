package com.orderbook.entity;

/**
 * Represents the execution state of an order in the book.
 */
public enum OrderStatus {
    /**
     * Order is accepted and is resting in the book.
     */
    OPEN,

    /**
     * Order has been partially matched, with remaining quantity resting.
     */
    PARTIALLY_FILLED,

    /**
     * Order has been fully filled and is no longer resting.
     */
    FILLED,

    /**
     * Order was cancelled by user request.
     */
    CANCELLED,

    /**
     * Order was rejected due to risk, validations, kill-switch, or STP.
     */
    REJECTED
}
