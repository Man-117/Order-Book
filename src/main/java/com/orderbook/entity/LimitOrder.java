package com.orderbook.entity;

public record LimitOrder(GeneralOrderInfo generalOrderInfo, long price) {
}
