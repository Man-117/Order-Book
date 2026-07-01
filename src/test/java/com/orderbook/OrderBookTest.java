package com.orderbook;

import com.orderbook.entity.*;
import com.orderbook.utils.IDGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class OrderBookTest {

    private OrderBook orderBook;
    private UUID user1;
    private UUID user2;

    @BeforeEach
    void setUp() {
        orderBook = OrderBook.builder("BTC/USD").build();
        user1 = IDGenerator.get();
        user2 = IDGenerator.get();
    }

    @Test
    void testBasicLimitOrderMatching() {
        // user1 buys 10 @ 10000
        LimitOrder buyOrder = new LimitOrder(
                new GeneralOrderInfo(OrderType.LIMIT, Side.BUY, 10, user1), 10000L);
        orderBook.addOrder(buyOrder);

        // user2 sells 5 @ 10000
        LimitOrder sellOrder1 = new LimitOrder(
                new GeneralOrderInfo(OrderType.LIMIT, Side.SELL, 5, user2), 10000L);
        orderBook.addOrder(sellOrder1);

        Trade trade1 = orderBook.consumeResult();
        assertNotNull(trade1);
        assertEquals(5, trade1.quantity());
        assertEquals(10000L, trade1.price());

        // user2 sells another 10 @ 10000 (should fill 5, leave 5 resting)
        LimitOrder sellOrder2 = new LimitOrder(
                new GeneralOrderInfo(OrderType.LIMIT, Side.SELL, 10, user2), 10000L);
        orderBook.addOrder(sellOrder2);

        Trade trade2 = orderBook.consumeResult();
        assertNotNull(trade2);
        assertEquals(5, trade2.quantity());
        assertEquals(10000L, trade2.price());

        // No more trades
        assertNull(orderBook.consumeResult());
    }

    @Test
    void testMarketOrderMatching() {
        // Provide liquidity
        orderBook.addOrder(new LimitOrder(
                new GeneralOrderInfo(OrderType.LIMIT, Side.SELL, 10, user1), 10100L));
        orderBook.addOrder(new LimitOrder(
                new GeneralOrderInfo(OrderType.LIMIT, Side.SELL, 10, user1), 10200L));

        // Market buy for 15
        MarketOrder marketBuy = new MarketOrder(
                new GeneralOrderInfo(OrderType.MARKET, Side.BUY, 15, user2));
        orderBook.addOrder(marketBuy);

        Trade trade1 = orderBook.consumeResult();
        assertNotNull(trade1);
        assertEquals(10, trade1.quantity());
        assertEquals(10100L, trade1.price());

        Trade trade2 = orderBook.consumeResult();
        assertNotNull(trade2);
        assertEquals(5, trade2.quantity());
        assertEquals(10200L, trade2.price());
        
        assertNull(orderBook.consumeResult());
    }

    @Test
    void testIcebergOrderReplenishment() {
        // Iceberg sell order: total 25, visible 5, at price 10000
        LimitOrder child = new LimitOrder(
                new GeneralOrderInfo(OrderType.LIMIT, Side.SELL, 5, user1), 10000L);
        IcebergOrder iceberg = new IcebergOrder(child, 25);
        orderBook.addOrder(iceberg);

        // Buy 12 @ 10000
        LimitOrder buyOrder = new LimitOrder(
                new GeneralOrderInfo(OrderType.LIMIT, Side.BUY, 12, user2), 10000L);
        orderBook.addOrder(buyOrder);

        // Should produce 3 trades (5 from first visible, 5 from replenishment, 2 from next replenishment)
        Trade trade1 = orderBook.consumeResult();
        assertNotNull(trade1);
        assertEquals(5, trade1.quantity());

        Trade trade2 = orderBook.consumeResult();
        assertNotNull(trade2);
        assertEquals(5, trade2.quantity());

        Trade trade3 = orderBook.consumeResult();
        assertNotNull(trade3);
        assertEquals(2, trade3.quantity());
        
        assertNull(orderBook.consumeResult());
    }

    @Test
    void testPostOnlyOrder() {
        // Add resting liquidity
        orderBook.addOrder(new LimitOrder(
                new GeneralOrderInfo(OrderType.LIMIT, Side.BUY, 10, user1), 10000L));

        // Attempt to cross the spread with Post-Only (Sell @ 9900)
        PostOnlyOrder crossingOrder = new PostOnlyOrder(
                new GeneralOrderInfo(OrderType.LIMIT, Side.SELL, 10, user2), 9900L);
        boolean addedCross = orderBook.addOrder(crossingOrder);
        assertFalse(addedCross, "Post-only order should be rejected if it crosses the spread");

        // Attempt non-crossing Post-Only (Sell @ 10100)
        PostOnlyOrder nonCrossingOrder = new PostOnlyOrder(
                new GeneralOrderInfo(OrderType.LIMIT, Side.SELL, 10, user2), 10100L);
        boolean addedNonCross = orderBook.addOrder(nonCrossingOrder);
        assertTrue(addedNonCross, "Post-only order should be accepted if it does not cross the spread");
        
        assertNull(orderBook.consumeResult());
    }

    @Test
    void testOrderCancellation() {
        LimitOrder order1 = new LimitOrder(
                new GeneralOrderInfo(OrderType.LIMIT, Side.BUY, 10, user1), 10000L);
        orderBook.addOrder(order1);

        assertTrue(orderBook.cancelOrder(order1));
        assertFalse(orderBook.cancelOrder(order1), "Should return false on subsequent cancellation");
    }
}
