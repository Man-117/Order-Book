package com.orderbook;

import com.orderbook.entity.*;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PriceLevelTest {

    @Test
    void testPriceLevelMatch() {
        PriceLevel level = new PriceLevel(50000);
        
        UUID makerId = UUID.randomUUID();
        GeneralOrderInfo makerInfo = new GeneralOrderInfo(OrderType.LIMIT, Side.SELL, 100, makerId);
        LimitOrder makerOrder = new LimitOrder(makerInfo, 50000);
        OrderOnBook makerOnBook = new OrderOnBook(makerInfo, 50000);
        level.add(makerOnBook);
        
        UUID takerId = UUID.randomUUID();
        GeneralOrderInfo takerInfo = new GeneralOrderInfo(OrderType.LIMIT, Side.BUY, 40, takerId);
        LimitOrder takerOrder = new LimitOrder(takerInfo, 50000);
        OrderOnBook takerOnBook = new OrderOnBook(takerInfo, 50000);
        
        MatchResult result = level.match(takerOnBook);
        
        assertFalse(result.cancelTaker());
        assertEquals(1, result.trades().size());
        assertEquals(40, result.trades().get(0).quantity());
        assertEquals(50000, result.trades().get(0).price());
        assertEquals(takerInfo.id(), result.trades().get(0).takerId());
        assertEquals(makerInfo.id(), result.trades().get(0).makerId());
    }

}
