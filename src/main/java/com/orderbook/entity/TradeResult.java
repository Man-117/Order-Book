package com.orderbook.entity;

import java.util.List;

/**
 * Enriched trade result that associates a {@link MatchResult} with the book
 * symbol and any computed fees.
 */
public record TradeResult(String symbol, List<MatchResult> matchResults, long totalMakerFees, long totalTakerFees) {
}
