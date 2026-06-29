package com.orderbook.example;

import com.orderbook.OrderBook;
import com.orderbook.entity.*;
import com.orderbook.utils.CommonUtil;
import com.orderbook.utils.IDGenerator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Demonstration of the Java order book implementation.
 * <p>
 * Run with:  mvn -f orderbook-java/pom.xml exec:java -Dexec.mainClass=com.orderbook.example.MainExample
 * or simply: java -cp target/classes com.orderbook.example.MainExample
 */
public class MainExample {

    public static void main(String[] args) {
        System.out.println("=== Java OrderBook Engine Demo ===\n");

        // ---------------------------------------------------------------
        // 1. Basic limit order matching
        // ---------------------------------------------------------------
        System.out.println("--- 1. Basic limit order matching (BTC/USD) ---");
        OrderBook book = OrderBook.builder("BTC/USD")
                .build();
        UUID user1 = IDGenerator.get();
        UUID user2 = IDGenerator.get();

        // Populate bids
        List<LimitOrder> limitOrders = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            long price = 9900 + i * 20L;  // 9900, 9920, 9940, 9960, 9980
            long qty = 10 + i * 5L;
            GeneralOrderInfo orderInfo = new GeneralOrderInfo(OrderType.LIMIT, Side.BUY, qty, user1);
            LimitOrder limitOrder = new LimitOrder(orderInfo, price);
            limitOrders.add(limitOrder);
            book.addOrder(limitOrder);
            System.out.printf("  Added BUY  %d @ %d%n", qty, price);
        }

        // Populate asks
        for (int i = 0; i < 5; i++) {
            long price = 10000 + i * 20L; // 10000, 10020, …
            long qty = 10 + i * 5L;
            GeneralOrderInfo orderInfo = new GeneralOrderInfo(OrderType.LIMIT, Side.SELL, qty, user2);
            LimitOrder limitOrder = new LimitOrder(orderInfo, price);
            limitOrders.add(limitOrder);
            book.addOrder(limitOrder);
            System.out.printf("  Added SELL %d @ %d%n", qty, price);
        }

        System.out.println("\n--- Market order BUY 15 ---");

        GeneralOrderInfo orderInfo = new GeneralOrderInfo(OrderType.MARKET, Side.BUY, 15, user1);
        MarketOrder marketOrder = new MarketOrder(orderInfo);
        book.addOrder(marketOrder);
        var r1=book.consumeResult();
        System.out.printf("  Executed result: %s", r1);

        // ---------------------------------------------------------------
        // 2. Iceberg order
        // ---------------------------------------------------------------
        System.out.println("\n--- 2. Iceberg order (visible=5, hidden=20) ---");
        orderInfo = new GeneralOrderInfo(OrderType.LIMIT, Side.SELL, 5, user2);
        LimitOrder child = new LimitOrder(orderInfo, 1000L);
        IcebergOrder parent = new IcebergOrder(child, 25);
        book.addOrder(parent);
        var r2=book.consumeResult();
        System.out.println("  Iceberg SELL 5+20 @ 100 added.");
        System.out.printf("  Market BUY 12, match result: %s", r2);


        // ---------------------------------------------------------------
        // 4. Mass cancel
        // ---------------------------------------------------------------
        System.out.println("\n--- 4. Mass cancel by side ---");
        LimitOrder[] orderArray = limitOrders.toArray(new LimitOrder[10]);
        boolean[] results = book.cancelOrder(orderArray);
        System.out.printf("cancel result: %s", Arrays.toString(results));
    }
}
