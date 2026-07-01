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

    public void fill(long quantity) {
        filled += quantity;
        if (filled > orderInfo.quantity()) {
            throw new RuntimeException("overfilled!");
        }
    }

    public boolean fullFilled() {
        return filled == orderInfo.quantity();
    }

    public long remainingQuantity() {
        return orderInfo.quantity() - filled;
    }
}
