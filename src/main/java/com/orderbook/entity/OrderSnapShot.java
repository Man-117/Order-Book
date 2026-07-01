package com.orderbook.entity;

public record OrderSnapShot(GeneralOrderInfo generalOrderInfo, long price, long filled) {
    public boolean fullFilled() {
        return generalOrderInfo.quantity() == filled;
    }
}
