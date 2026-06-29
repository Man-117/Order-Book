package com.orderbook.entity;

public record OrderSnapShot(GeneralOrderInfo generalOrderInfo, long price, long filled) {
}
