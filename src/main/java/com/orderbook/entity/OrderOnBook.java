package com.orderbook.entity;

public class OrderOnBook {
    private final GeneralOrderInfo generalOrderInfo;
    private final long price;
    private long filled = 0;

    public OrderOnBook(GeneralOrderInfo generalOrderInfo, long price) {
        this.generalOrderInfo = generalOrderInfo;
        this.price = price;
    }

    public void fill(long quantity) {
        filled += quantity;
        if (filled > generalOrderInfo.quantity()) {
            throw new RuntimeException("overfilled!");
        }
    }

    public GeneralOrderInfo generalOrderInfo() {
        return generalOrderInfo;
    }

    public long price() {
        return price;
    }

    public long filled() {
        return filled;
    }

    public long remainder() {
        return generalOrderInfo.quantity() - filled;
    }

    public boolean fullFilled() {
        return generalOrderInfo.quantity() == filled;
    }
    public OrderSnapShot snapShot() {
        return new OrderSnapShot(generalOrderInfo, price, filled);
    }

    public long remainingQuantity() {
        return generalOrderInfo.quantity() - filled;
    }

}
