package com.orderbook;

import com.orderbook.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class OrderBookTest {

    private OrderBook orderBook;

    @BeforeEach
    void setUp() {
        orderBook = OrderBook.builder("BTCUSD")
                .withTickSize(new BigDecimal("0.01"))
                .withLotSize(new BigDecimal("1"))
                .build();
    }

    @Test
    void testAddAndMatchLimitOrder() {
        UUID user1 = UUID.randomUUID();
        UUID user2 = UUID.randomUUID();

        GeneralOrderInfo buyInfo = new GeneralOrderInfo(OrderType.LIMIT, Side.BUY, 100, user1);
        LimitOrder buyOrder = new LimitOrder(buyInfo, 50000);
        orderBook.addOrder(buyOrder);

        GeneralOrderInfo sellInfo = new GeneralOrderInfo(OrderType.LIMIT, Side.SELL, 50, user2);
        LimitOrder sellOrder = new LimitOrder(sellInfo, 49000); // Crosses the buy order
        orderBook.addOrder(sellOrder);

        Trade trade = orderBook.consumeResult();
        assertNotNull(trade);
        assertEquals(50, trade.quantity());
        assertEquals(50000, trade.price()); // Executes at maker price
        assertEquals(sellInfo.id(), trade.takerId());
        assertEquals(buyInfo.id(), trade.makerId());

        assertNull(orderBook.consumeResult());
    }

    @Test
    void testCancelOrder() {
        UUID user1 = UUID.randomUUID();
        GeneralOrderInfo buyInfo = new GeneralOrderInfo(OrderType.LIMIT, Side.BUY, 100, user1);
        LimitOrder buyOrder = new LimitOrder(buyInfo, 50000);
        orderBook.addOrder(buyOrder);

        boolean cancelled = orderBook.cancelOrder(buyOrder);
        assertTrue(cancelled);

        boolean cancelledAgain = orderBook.cancelOrder(buyOrder);
        assertFalse(cancelledAgain);
    }

    @Test
    void testSelfTradePrevention() {
        UUID user1 = UUID.randomUUID();
        // same user
        GeneralOrderInfo buyInfo = new GeneralOrderInfo(OrderType.LIMIT, Side.BUY, 100, user1);
        LimitOrder buyOrder = new LimitOrder(buyInfo, 50000);
        orderBook.addOrder(buyOrder);

        GeneralOrderInfo sellInfo = new GeneralOrderInfo(OrderType.LIMIT, Side.SELL, 50, user1);
        LimitOrder sellOrder = new LimitOrder(sellInfo, 49000);
        orderBook.addOrder(sellOrder);

        // Under CANCEL_TAKER STP, taker (sell) should be cancelled, maker (buy) stays on book, no trade.
        Trade trade = orderBook.consumeResult();
        assertNull(trade);
        
        // We can check if maker is still there by trying to cancel it
        assertTrue(orderBook.cancelOrder(buyOrder));
    }
}
