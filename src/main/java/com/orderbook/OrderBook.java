package com.orderbook;

import com.orderbook.entity.*;
import com.orderbook.utils.Converter;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

/**
 * High-performance limit order book with price-time priority matching.
 *
 * <h2>Architecture</h2>
 * <ul>
 *   <li>Bids are stored in a {@link TreeMap} keyed by price <em>descending</em>
 *       (highest bid first).</li>
 *   <li>Asks are stored in a {@link TreeMap} keyed by price <em>ascending</em>
 *       (lowest ask first).</li>
 *       cancel / update without scanning price levels.</li>
 *   <li>Each {@link PriceLevel} holds a {@link java.util.LinkedHashMap} of orders in
 *       FIFO insertion order.</li>
 * </ul>
 *
 * <h2>Supported order types</h2>
 * <ul>
 *   <li>Limit — standard resting limit order (Good Till Cancelled).</li>
 *   <li>Market — immediately matches against opposite side at any price.</li>
 *   <li>Iceberg — visible quantity rests; hidden refills when visible exhausts.</li>
 *   <li>Post-Only — rejected if it would immediately cross the spread.</li>
 * </ul>
 */
public final class OrderBook {

    private final String symbol;

    // -----------------------------------------------------------------------
    // Price levels
    // Bids: descending comparator so highest bid is first (best bid)
    // Asks: natural (ascending) order so lowest ask is first (best ask)
    // -----------------------------------------------------------------------

    private final SortedMap<Long, PriceLevel> bids = new TreeMap<>(Comparator.reverseOrder());
    private final SortedMap<Long, PriceLevel> asks = new TreeMap<>();
    private final Map<UUID, IcebergOrderOnBook> icebergOrders = new HashMap<>();
    private final ConcurrentLinkedQueue<Trade> trades = new ConcurrentLinkedQueue<>();
    private final Queue<Trade> tradesToProcess = new LinkedList<>();

    // -----------------------------------------------------------------------
    // Configuration (immutable)
    // -----------------------------------------------------------------------

    private final BigDecimal tickSize;
    private final BigDecimal lotSize;
    private final Long minOrderSize;
    private final Long maxOrderSize;

    private OrderBook(Builder builder) {
        this.symbol = builder.symbol;
        this.tickSize = builder.tickSize;
        this.lotSize = builder.lotSize;
        this.minOrderSize = builder.minOrderSize;
        this.maxOrderSize = builder.maxOrderSize;
    }

    public static Builder builder(String symbol) {
        return new Builder(symbol);
    }


    public static final class Builder {
        private final String symbol;
        private BigDecimal tickSize = null;
        private BigDecimal lotSize = null;
        private Long minOrderSize = null;
        private Long maxOrderSize = null;

        private Builder(String symbol) {
            this.symbol = Objects.requireNonNull(symbol, "symbol");
        }

        public Builder withTickSize(BigDecimal tickSize) {
            if (tickSize == null || tickSize.compareTo(BigDecimal.ZERO) <= 0)
                throw new IllegalArgumentException("tickSize must be > 0");
            this.tickSize = tickSize;
            return this;
        }

        public Builder withLotSize(BigDecimal lotSize) {
            if (lotSize == null || lotSize.compareTo(BigDecimal.ZERO) <= 0)
                throw new IllegalArgumentException("lotSize must be > 0");
            this.lotSize = lotSize;
            return this;
        }

        public Builder withMinOrderSize(long min) {
            this.minOrderSize = min;
            return this;
        }

        public Builder withMaxOrderSize(long max) {
            this.maxOrderSize = max;
            return this;
        }

        public OrderBook build() {
            return new OrderBook(this);
        }
    }

    // -----------------------------------------------------------------------
    // Unified Order Submission
    // -----------------------------------------------------------------------
    public Trade consumeResult() {
        return trades.poll();
    }

    public void addOrder(LimitOrder order) {
        preProcess(order.generalOrderInfo());
        //special process
        OrderOnBook orderOnBook = orderPreProcess(order);

        orderTake(orderOnBook);

        orderAfterProcess(order);
        afterProcess(order.generalOrderInfo());
    }

    public <T> void addOrder(MarketOrder order) {
        preProcess(order.generalOrderInfo());
        orderPreProcess(order);

        orderTake(order);

        orderAfterProcess(order);
        afterProcess(order.generalOrderInfo());
    }

    public void addOrder(IcebergOrder order) {
        preProcess(order.childOrder()
                .generalOrderInfo());
        OrderOnBook orderOnBook = orderPreProcess(order);

        orderTake(orderOnBook);

        orderAfterProcess(order);
        afterProcess(order.childOrder()
                .generalOrderInfo());
    }

    public boolean addOrder(PostOnlyOrder order) {
        preProcess(order.generalOrderInfo());
        OrderOnBook orderOnBook = orderPreProcess(order);
        if (orderOnBook == null) return false;
        orderTake(orderOnBook);

        orderAfterProcess(order);
        afterProcess(order.generalOrderInfo());
        return true;
    }

    private void preProcess(GeneralOrderInfo generalOrderInfo) {
        validateOrder(generalOrderInfo);
    }


    private OrderOnBook orderPreProcess(LimitOrder order) {

        validatePrice(order.price());
        return Converter.toOrderOnBook(order);
    }


    private void orderPreProcess(MarketOrder order) {

    }

    private OrderOnBook orderPreProcess(PostOnlyOrder order) {
        boolean accept = switch (order.generalOrderInfo()
                .side()) {
            case SELL -> bids.firstKey() < order.price();
            case BUY -> asks.firstKey() > order.price();
        };
        if (!accept) {
            return null;
        }
        return Converter.toOrderOnBook(order);
    }

    private OrderOnBook orderPreProcess(IcebergOrder order) {
        OrderOnBook orderOnBook = orderPreProcess(order.childOrder());
        if (order.totalQuantity() <= order.childOrder()
                .generalOrderInfo()
                .quantity())
            throw new OrderBookException(OrderBookException.Reason.INVALID_QUANTITY, "Slice for iceberg order must be >= 0: " + order.childOrder()
                    .price());


        IcebergOrderOnBook parent = Converter.toOrderOnBook(order);
        icebergOrders.put(order.childOrder()
                .generalOrderInfo()
                .id(), parent);
        return orderOnBook;
    }

    private void afterProcess(GeneralOrderInfo generalOrderInfo) {
        while (!tradesToProcess.isEmpty()) {
            Trade trade = tradesToProcess.poll();
            replenishIfNecessary(trade.makerSnapShot());
            trades.add(trade);
        }
    }

    private void orderAfterProcess(LimitOrder order) {
        System.out.println("Limit order processed");
    }

    private void orderAfterProcess(PostOnlyOrder order) {
        System.out.println("Post only order process");
    }

    private void orderAfterProcess(IcebergOrder order) {
        System.out.println("Iceberg order processed");
    }

    private void orderAfterProcess(MarketOrder order) {
        System.out.println("Market order processed");
    }


    // -----------------------------------------------------------------------
// Order cancellation
// -----------------------------------------------------------------------
//    TODO handle the concurrent access of bids and asks
    public boolean cancelOrder(LimitOrder order) {

        SortedMap<Long, PriceLevel> targetBook = switch (order.generalOrderInfo()
                .side()) {
            case BUY -> bids;
            case SELL -> asks;
        };
        PriceLevel level = targetBook.get(order.price());
        if (level == null) {
            return false;
        }
        Optional<OrderOnBook> result = level.remove(order.generalOrderInfo()
                .id());

        return result.isPresent();
    }

    public boolean[] cancelOrder(LimitOrder... orders) {
        boolean[] results = new boolean[orders.length];
        for (int i = 0; i < orders.length; i++) {
            results[i] = cancelOrder(orders[i]);
        }
        return results;
    }

    public boolean cancelOrder(PostOnlyOrder order) {

        SortedMap<Long, PriceLevel> targetBook = switch (order.generalOrderInfo()
                .side()) {
            case BUY -> bids;
            case SELL -> asks;
        };
        PriceLevel level = targetBook.get(order.price());
        if (level == null) {
            return false;
        }
        Optional<OrderOnBook> result = level.remove(order.generalOrderInfo()
                .id());

        return result.isPresent();
    }

    public boolean[] cancelOrder(PostOnlyOrder... orders) {
        boolean[] results = new boolean[orders.length];
        for (int i = 0; i < orders.length; i++) {
            results[i] = cancelOrder(orders[i]);
        }
        return results;
    }

    public boolean cancelOrder(IcebergOrder order) {

        SortedMap<Long, PriceLevel> targetBook = switch (order.childOrder()
                .generalOrderInfo()
                .side()) {
            case BUY -> bids;
            case SELL -> asks;
        };
        PriceLevel level = targetBook.get(order.childOrder()
                .price());
        if (level == null) {
            return false;
        }
        Optional<OrderOnBook> result = level.remove(order.childOrder()
                .generalOrderInfo()
                .id());

        return result.isPresent();
    }

    public boolean[] cancelOrder(IcebergOrder... orders) {
        boolean[] results = new boolean[orders.length];
        for (int i = 0; i < orders.length; i++) {
            results[i] = cancelOrder(orders[i]);
        }
        return results;
    }

// -----------------------------------------------------------------------
// Internal — matching engine
// -----------------------------------------------------------------------


    /**
     * Walk the opposite side of the book filling {@code qty} at {@code limitPrice}
     * (or any price for market orders when {@code limitPrice == 0}).
     *
     * @return total quantity filled
     */
    private void orderTake(OrderOnBook order) {

        long remaining = order.generalOrderInfo()
                .quantity();
        SortedMap<Long, PriceLevel> oppositeBook = order.generalOrderInfo()
                .side() == Side.BUY ? asks : bids;

        Set<Map.Entry<Long, PriceLevel>> entries = oppositeBook.entrySet();
        for (Map.Entry<Long, PriceLevel> entry : entries) {

            long levelPrice = entry.getKey();

            // Price check for limit orders

            if (order.generalOrderInfo()
                    .side() == Side.BUY && levelPrice > order.price()) break;
            if (order.generalOrderInfo()
                    .side() == Side.SELL && levelPrice < order.price()) break;


            PriceLevel level = entry.getValue();

            MatchResult matchResult = level.match(order);
            if (matchResult.trades()
                    .isEmpty()) {
                break;
            }
            long postpone = TimeUnit.SECONDS.toMillis(1);
            tradesToProcess.addAll(matchResult.trades());
            if (matchResult.cancelTaker()) return;
        }
        placeOrder(order);
    }

    private void placeOrder(OrderOnBook order) {
        var place = switch (order.generalOrderInfo()
                .side()) {
            case BUY -> bids;
            case SELL -> asks;
        };
        place.computeIfAbsent(order.price(), PriceLevel::new);
        place.get(order.price())
                .add(order);

    }

    private void orderTake(MarketOrder order) {

        long remaining = order.generalOrderInfo()
                .quantity();
        SortedMap<Long, PriceLevel> oppositeBook = order.generalOrderInfo()
                .side() == Side.BUY ? asks : bids;

        Set<Map.Entry<Long, PriceLevel>> entries = oppositeBook.entrySet();
        for (Map.Entry<Long, PriceLevel> entry : entries) {

            PriceLevel level = entry.getValue();

            MatchResult matchResult = level.match(order);
            long postpone = TimeUnit.SECONDS.toMillis(1);
            while (!tradesToProcess.addAll(matchResult.trades())) {
                try {
                    Thread.sleep(postpone);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                postpone += postpone * 2;
            }
            if (matchResult.cancelTaker()) return;
        }
    }

    private void replenishIfNecessary(OrderSnapShot snapShot) {
        if (snapShot.filled() != snapShot.generalOrderInfo()
                .quantity()) {
            return;
        }
        IcebergOrderOnBook icebergOrder = icebergOrders.get(snapShot.generalOrderInfo()
                .id());
        if (icebergOrder == null) {
            return;
        }
        addOrder(icebergOrder.replenish());
    }

    // -----------------------------------------------------------------------
// Internal — validation helpers
// -----------------------------------------------------------------------
    private void validateOrder(GeneralOrderInfo orderInfo) {
        if (minOrderSize != null && orderInfo.quantity() < minOrderSize)
            throw new OrderBookException(OrderBookException.Reason.ORDER_BELOW_MIN_SIZE, "Quantity " + orderInfo.quantity() + " < minimum order size " + minOrderSize);
        if (maxOrderSize != null && orderInfo.quantity() > maxOrderSize)
            throw new OrderBookException(OrderBookException.Reason.ORDER_EXCEEDS_MAX_SIZE, "Quantity " + orderInfo.quantity() + " > maximum order size " + maxOrderSize);
    }

    private void validatePrice(long price) {
        if (price <= 0)
            throw new OrderBookException(OrderBookException.Reason.INVALID_PRICE, "Price must be > 0: " + price);
    }

// Tick and lot size modulo validations removed because price and quantity
// are now treated as integer multipliers of the decimal tick/lot sizes.

    private void validateSizeConstraints(UUID id, long quantity) {
    }


    /**
     * Would adding a resting order at {@code price} on {@code side}
     * immediately cross the spread?
     */
    private boolean wouldCross(PostOnlyOrder order) {
        Side side = order.generalOrderInfo()
                .side();
        long price = order.price();
        if (side == Side.BUY) {
            long bestAsk = asks.firstKey();
            return price >= bestAsk;
        } else {
            long bestBid = bids.firstKey();
            return price <= bestBid;
        }
    }
}
