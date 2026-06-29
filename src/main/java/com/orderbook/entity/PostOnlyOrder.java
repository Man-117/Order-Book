package com.orderbook.entity;

public record PostOnlyOrder(GeneralOrderInfo generalOrderInfo, long price) {
}
