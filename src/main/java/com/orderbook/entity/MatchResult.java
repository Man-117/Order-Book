package com.orderbook.entity;

import java.util.List;

/**
 * The result of a matching operation.
 *
 * <p>Contains the list of trades that were executed and the remaining
 * (unfilled) quantity of the incoming order.
 */
public record MatchResult(OrderOnBook order, List<Trade> trades, boolean cancelTaker) {
}