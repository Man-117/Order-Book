package com.orderbook.entity;

import com.orderbook.utils.CommonUtil;
import com.orderbook.utils.IDGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class IcebergOrderOnBook {
    private final UUID id;
    private LimitOrder childOrder;
    private final List<Trade> trades=new ArrayList<>();
    private long remainingQuantity;

    public IcebergOrderOnBook(UUID id, LimitOrder childOrder, long remainingQuantity) {
        this.id = id;
        this.childOrder = childOrder;
        this.remainingQuantity = remainingQuantity;
    }

    public LimitOrder replenish() {
        remainingQuantity -= childOrder.generalOrderInfo()
                .quantity();
        long quantity = Math.min(remainingQuantity, childOrder.generalOrderInfo()
                .quantity());
        GeneralOrderInfo childInfo = childOrder.generalOrderInfo();
        GeneralOrderInfo newInfo = new GeneralOrderInfo(IDGenerator.get(), childInfo.type(), childInfo.side(), quantity, childInfo.userId(), CommonUtil.now());
        childOrder = new LimitOrder(newInfo, childOrder.price());
        return childOrder;
    }

    public List<Trade> getTrades() {
        return trades;
    }

    public void addTrade(Trade trades) {
        this.trades.add(trades);
    }

    public UUID getId() {
        return id;
    }

    public LimitOrder getChildOrder() {
        return childOrder;
    }

    public void setChildOrder(LimitOrder childOrder) {
        this.childOrder = childOrder;
    }

    public long getRemainingQuantity() {
        return remainingQuantity;
    }

    public void setRemainingQuantity(long remainingQuantity) {
        this.remainingQuantity = remainingQuantity;
    }
}
