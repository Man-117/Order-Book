package com.orderbook.entity;

/**
 * Exception thrown by the order book for business-logic errors.
 */
public class OrderBookException extends RuntimeException {

    public enum Reason {
        INVALID_PRICE,
        INVALID_QUANTITY,
        ORDER_NOT_FOUND,
        DUPLICATE_ORDER_ID,
        POST_ONLY_WOULD_CROSS,
        INSUFFICIENT_LIQUIDITY,
        SELF_TRADE_PREVENTED,
        KILL_SWITCH_ACTIVE,
        INVALID_TICK_SIZE,
        INVALID_LOT_SIZE,
        ORDER_EXCEEDS_MAX_SIZE,
        ORDER_BELOW_MIN_SIZE,
        MISSING_USER_ID,
    }

    private final Reason reason;

    public OrderBookException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public Reason getReason() { return reason; }
}
