package com.orderbook.entity;

import java.util.UUID;

/**
 * Represents a single executed trade between a taker and a maker order.
 */
public record Trade(long price, long quantity, UUID takerId, UUID makerId, OrderSnapShot makerSnapShot, long timestamp) {
}
