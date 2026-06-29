package com.orderbook.entity;

/**
 * Defines the Self-Trade Prevention (STP) behavior.
 */
public enum STPMode {
    /**
     * No STP checks.
     */
    NONE,

    /**
     * Cancel the incoming (taker) order on self-trade conflict.
     */
    CANCEL_TAKER,

    /**
     * Cancel the resting (maker) order(s) on self-trade conflict.
     */
    CANCEL_MAKER,

    /**
     * Cancel both maker and taker orders on self-trade conflict.
     */
    CANCEL_BOTH
}
