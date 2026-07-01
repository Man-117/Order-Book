package com.orderbook;

import com.orderbook.config.Config;
import com.orderbook.entity.MarketOrderOnBook;
import com.orderbook.entity.MatchResult;
import com.orderbook.entity.OrderOnBook;
import com.orderbook.entity.Trade;
import com.orderbook.utils.CommonUtil;

import java.util.*;

/**
 * A price level in the order book, holding all resting orders at one price.
 *
 * <p>Orders are maintained in FIFO (time-priority) order.
 */
public final class PriceLevel {

    private final long price;

    // FIFO order queue — LinkedHashMap preserves insertion order and gives O(1) remove by id.
    private final Map<UUID, OrderOnBook> orders = new LinkedHashMap<>();

    PriceLevel(long price) {
        this.price = price;
    }

    MatchResult match(OrderOnBook taker) {
        List<Trade> trades = new ArrayList<>();
        boolean cancelTaker = false;
        Iterator<Map.Entry<UUID, OrderOnBook>> it = orders.entrySet()
                .iterator();

        iterate_orders:
        while (it.hasNext() && !taker.fullFilled()) {
            Map.Entry<UUID, OrderOnBook> entry = it.next();
            OrderOnBook maker = entry.getValue();

            // Self-trade prevention
            STPDecision stp = checkStp(taker.generalOrderInfo()
                    .userId(), maker.generalOrderInfo()
                    .userId());
            switch (stp) {
                case CANCEL_TAKER -> {
                    cancelTaker = true;
                    break iterate_orders;
                }
                case CANCEL_MAKER -> {
                    orders.remove(maker.generalOrderInfo()
                            .id());
                    continue iterate_orders;
                }
                case CANCEL_BOTH -> {
                    cancelTaker = true;
                    orders.remove(maker.generalOrderInfo()
                            .id());
                    break iterate_orders;
                }
            }

            long matchQty = Math.min(taker.remainder(), maker.remainder());
            taker.fill(matchQty);
            maker.fill(matchQty);
            if (maker.fullFilled()) {
                orders.remove(maker.generalOrderInfo()
                        .id());
            }
            trades.add(new Trade(price, matchQty, taker.generalOrderInfo()
                    .id(), maker.generalOrderInfo()
                    .id(), maker.snapShot(), taker.snapShot(), CommonUtil.now()));
        }
        return new MatchResult(taker, trades, cancelTaker);
    }

    MatchResult match(MarketOrderOnBook taker) {
        List<Trade> trades = new ArrayList<>();
        boolean cancelTaker = false;

        Iterator<Map.Entry<UUID, OrderOnBook>> it = orders.entrySet()
                .iterator();

        iterate_orders:
        while (it.hasNext() && !taker.fullFilled()) {
            Map.Entry<UUID, OrderOnBook> entry = it.next();
            OrderOnBook maker = entry.getValue();

            // Self-trade prevention
            STPDecision stp = checkStp(taker.getOrderInfo()
                    .userId(), maker.generalOrderInfo()
                    .userId());
            switch (stp) {
                case CANCEL_TAKER -> {
                    cancelTaker = true;
                    break iterate_orders;
                }
                case CANCEL_MAKER -> {
                    orders.remove(maker.generalOrderInfo()
                            .id());
                    continue iterate_orders;
                }
                case CANCEL_BOTH -> {
                    cancelTaker = true;
                    orders.remove(maker.generalOrderInfo()
                            .id());
                    break iterate_orders;
                }
            }

            long matchQty = Math.min(taker.remainingQuantity(), maker.remainingQuantity());
            maker.fill(matchQty);
            taker.fill(matchQty);
            if (maker.fullFilled()) {
                orders.remove(maker.generalOrderInfo()
                        .id());
            }
            trades.add(new Trade(price, matchQty, taker.getOrderInfo()
                    .id(), maker.generalOrderInfo()
                    .id(), maker.snapShot(), taker.snapShot(price), CommonUtil.now()));


        }
        return new MatchResult(new OrderOnBook(taker.getOrderInfo(), price), trades, cancelTaker);
    }
    // -----------------------------------------------------------------------
    // Accessors
    // -----------------------------------------------------------------------

    public long getPrice() {
        return price;
    }

    /**
     * Total quantity (visible + hidden) across all orders at this level.
     */
    public Optional<OrderOnBook> find(UUID id) {
        return Optional.ofNullable(orders.get(id));
    }

    // -----------------------------------------------------------------------
    // Mutations (package-private)
    // -----------------------------------------------------------------------

    void add(OrderOnBook order) {
        orders.put(order.generalOrderInfo()
                .id(), order);
    }

    /**
     * Returns the removed order, or empty if not found.
     */
    Optional<OrderOnBook> remove(UUID id) {
        return Optional.ofNullable(orders.remove(id));
    }


    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private enum STPDecision {NONE, CANCEL_TAKER, CANCEL_MAKER, CANCEL_BOTH}

    private static STPDecision checkStp(UUID taker, UUID maker) {
        if (!taker.equals(maker)) return STPDecision.NONE;

        return switch (Config.STOP_MODE) {
            case CANCEL_TAKER -> STPDecision.CANCEL_TAKER;
            case CANCEL_MAKER -> STPDecision.CANCEL_MAKER;
            case CANCEL_BOTH -> STPDecision.CANCEL_BOTH;
            case NONE -> STPDecision.NONE;
        };
    }
}
