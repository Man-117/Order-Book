package com.orderbook.entity;

public class MarketOrderOnBook {
    private final GeneralOrderInfo orderInfo;
    private long filled;

    public MarketOrderOnBook(GeneralOrderInfo orderInfo) {
        this.orderInfo = orderInfo;
        this.filled = 0;
    }

    public OrderSnapShot snapShot(long price) {
        return new OrderSnapShot(orderInfo, price, filled);
    }

    public GeneralOrderInfo getOrderInfo() {
        return orderInfo;
    }
}
